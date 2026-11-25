package org.me.gcu.gaughan_nicholas_s2533284;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Pop-up with situation details
// AlertDialog wrapped in dialog fragment so its is recreated after rotation
public class SituationDetailsDialog extends DialogFragment {

    private static final String ARG_SITUATION_ID = "situationId";

    private static final int NO_COLOUR = 0; // 0 = use the normal text colour (addLine)

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    // Empty constructor to recreate fragment after rotation
    // ID passed in the arguments Bundle, which is saved and restored with fragment
    public static SituationDetailsDialog newInstance(String situationId) {
        SituationDetailsDialog dialog = new SituationDetailsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SITUATION_ID, situationId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The Activity's inflater
        // Fragment's getLayoutInflater() would call onCreateDialog again
        View v = requireActivity().getLayoutInflater().inflate(R.layout.dialog_situation_details, null);
        ImageView icon = v.findViewById(R.id.detailsIcon);
        TextView title = v.findViewById(R.id.detailsTitle);
        TextView details = v.findViewById(R.id.detailsText);

        // Same ViewModel as MainActivity and list
        TrafficViewModel viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        String id = requireArguments().getString(ARG_SITUATION_ID);
        Situation situation = viewModel.getSituationById(id);

        if (situation == null) {
            // e.g. situation ended and a refresh removed it while dialog was open
            icon.setVisibility(View.GONE);
            title.setText(R.string.details_not_available);
            details.setVisibility(View.GONE);
        }
        else {
            icon.setImageResource(SituationIcons.getIconRes(situation.getType()));
            icon.setColorFilter(ContextCompat.getColor(requireContext(),
                    SituationIcons.getColorRes(situation.getCategory())));
            title.setText(situation.getTypeDisplayName());
            details.setText(buildDetails(situation));
        }

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setPositiveButton(R.string.close, null) // null listener. Button just closes the dialog
                .create();
    }

    // Builds details as one piece of text, one "Label: value" per line
    // SpannableStringBuilder used to enable coloured delay time/bold
    private CharSequence buildDetails(Situation s) {
        SpannableStringBuilder text = new SpannableStringBuilder();

        addLine(text, getString(R.string.label_category), s.getCategory().getDisplayName());
        addLine(text, getString(R.string.label_roads), s.getRoadsDisplay());

        Direction direction = s.getLocation().getDirection();
        if (direction != Direction.UNKNOWN) {
            addLine(text, getString(R.string.label_direction), direction.getDisplayName());
        }

        addLine(text, getString(R.string.label_description), s.getDescription());

        // Impact shows lanes for incidents
        // Shows delay level for roadworks
        if (s.getCategory() == SituationCategory.UNPLANNED_EVENT) {
            addLine(text, getString(R.string.label_lanes), getLanesText(s));
        }
        else {
            DelayType delay = s.getDelay();
            if (delay != null && delay != DelayType.UNKNOWN) {
                int colour = ContextCompat.getColor(requireContext(), SituationIcons.getDelayColorRes(delay));
                addLine(text, getString(R.string.label_delay), delay.getDisplayName(), colour);
            }
        }

        addLine(text, getString(R.string.label_start), formatTime(s.getStartTime()));
        addLine(text, getString(R.string.label_end), formatTime(s.getEndTime()));

        return text;
    }

    // Normal text colour
    private void addLine(SpannableStringBuilder text, String label, String value) {
        addLine(text, label, value, NO_COLOUR);
    }

    // Label in bold and the value in given colour
    // Missing values are skipped, so dialog has no empty lines
    private void addLine(SpannableStringBuilder text, String label, String value, int colour) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (text.length() > 0) {
            text.append("\n");
        }

        int labelStart = text.length();
        text.append(label).append(": ");
        text.setSpan(new StyleSpan(Typeface.BOLD), labelStart, text.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int valueStart = text.length();
        text.append(value);
        if (colour != NO_COLOUR) {
            text.setSpan(new ForegroundColorSpan(colour), valueStart, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // e.g. "2 open, 1 restricted", or null if no lane numbers from feed
    private String getLanesText(Situation s) {
        List<String> parts = new ArrayList<>();
        if (s.getOperationalLanes() != null) {
            parts.add(getString(R.string.lanes_open, s.getOperationalLanes()));
        }
        if (s.getRestrictedLanes() != null) {
            parts.add(getString(R.string.lanes_restricted, s.getRestrictedLanes()));
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(", ", parts);
    }

    // Converts the feed time to the phone's time zone
    // null if no time
    private String formatTime(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZoneSameInstant(ZoneId.systemDefault()).format(TIME_FORMAT);
    }
}
