package org.me.gcu.gaughan_nicholas_s2533284;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// Fills the RecyclerView with one row per situation
// Only the rows on screen exist at any time
public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    // User taps row for details and taps checkbox to add to map selection
    public interface SituationRowListener {
        void onSituationClicked(Situation situation); // tap row
        void onSituationChecked(Situation situation, boolean isChecked); // tap checkbox
    }

    private List<Situation> situations = new ArrayList<>();
    private final TrafficViewModel viewModel; // which situations are ticked?
    private final SituationRowListener listener;

    public SituationAdapter(TrafficViewModel viewModel, SituationRowListener listener) {
        this.viewModel = viewModel;
        this.listener = listener;
    }

    // Holds the views of one row. findViewById only runs on create
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView description;
        private final CheckBox checkBox;

        public ViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.situationIcon);
            title = v.findViewById(R.id.situationTitle);
            description = v.findViewById(R.id.situationDescription);
            checkBox = v.findViewById(R.id.situationCheckBox);
        }
    }

    // Update list contents after load/filter change
    public void setSituations(List<Situation> situations) {
        this.situations = situations;
        notifyDataSetChanged();
    }

    // Creates an empty row (called by layout manager)
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // Create a new view inflating the custom layout
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(v);
    }

    // Put one situation's data into a row when created or recycled
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Situation situation = situations.get(position);

        holder.icon.setImageResource(SituationIcons.getIconRes(situation.getType()));
        int colour = ContextCompat.getColor(holder.itemView.getContext(),
                SituationIcons.getColorRes(situation.getCategory()));
        holder.icon.setColorFilter(colour);
        holder.icon.setContentDescription(situation.getTypeDisplayName());

        holder.title.setText(holder.itemView.getContext().getString(R.string.situation_title,
                situation.getTypeDisplayName(), situation.getRoadsDisplay()));
        holder.description.setText(situation.getDescription());

        // Set tick
        holder.checkBox.setChecked(viewModel.isSelected(situation.getId()));

        // Listener for row taps (shows situation details)
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSituationClicked(situation);
            }
        });

        // Listener for checkbox taps
        // setOnClickListener, not setOnCheckedChange
        // Only fires on a tap, not when recycled
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onSituationChecked(situation, holder.checkBox.isChecked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return situations.size();
    }
}
