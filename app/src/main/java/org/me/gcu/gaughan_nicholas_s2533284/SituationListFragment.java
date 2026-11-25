package org.me.gcu.gaughan_nicholas_s2533284;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Situation list, filters (search, category, distance)
// and 'View Selected On Map' button
public class SituationListFragment extends Fragment implements SituationAdapter.SituationRowListener {

    // Miles for each distance spinner entry, same order as R.array.distance_options. 0 = any distance
    private static final int[] DISTANCE_MILES = {1, 5, 10, 50, 0};

    // Fragment lets MainActivity know when map button is pressed
    // Only the Activity can swap the fragments
    public interface SituationListListener {
        void onViewSelectedOnMap();
    }

    private SituationListListener listener; // MainActivity
    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyText;

    public SituationListFragment() {
        // Required empty constructor
    }

    // Stores reference to MainActivity for listener callback
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SituationListListener) {
            listener = (SituationListListener) context;
        }
        else {
            throw new RuntimeException(context + " must implement SituationListListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_situation_list, container, false);

        // requireActivity() gives the same ViewModel instance that MainActivity uses
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        recyclerView = v.findViewById(R.id.situationRecyclerView);
        emptyText = v.findViewById(R.id.emptyText);
        adapter = new SituationAdapter(viewModel, this);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);

        Button mapButton = v.findViewById(R.id.viewOnMapButton);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (viewModel.hasSelection()) {
                    listener.onViewSelectedOnMap();
                }
                else {
                    Toast.makeText(getContext(), R.string.toast_nothing_selected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        setupFilters(v);

        return v;
    }

    // Shows the current filters from the ViewModel and adds a listener to each
    // Listener stores the new value in the ViewModel and redraws the list
    private void setupFilters(View v) {
        EditText searchBox = v.findViewById(R.id.searchBox);
        RadioGroup categoryGroup = v.findViewById(R.id.categoryGroup);
        Spinner distanceSpinner = v.findViewById(R.id.distanceSpinner);

        // Populate spinner from the string-array
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.distance_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceSpinner.setAdapter(spinnerAdapter);

        // Show the current filter values
        searchBox.setText(viewModel.getSearchText());
        SituationCategory category = viewModel.getCategoryFilter();
        if (category == SituationCategory.UNPLANNED_EVENT) {
            categoryGroup.check(R.id.radioIncidents);
        }
        else if (category == SituationCategory.ROADWORKS) {
            categoryGroup.check(R.id.radioRoadworks);
        }
        else {
            categoryGroup.check(R.id.radioAll);
        }
        distanceSpinner.setSelection(getSpinnerPosition(viewModel.getMaxDistanceMiles()));

        // Listener 1: Search box. Filters on every key press
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchText(s.toString());
                refreshList();
            }
        });

        // Listener 2: the done (tick) key closes the keyboard and removes the cursor
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)
                            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
                    searchBox.clearFocus(); // focus to root layout
                    return true;
                }
                return false;
            }
        });

        // Listener 3: category radio buttons
        categoryGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioIncidents) {
                    viewModel.setCategoryFilter(SituationCategory.UNPLANNED_EVENT);
                }
                else if (checkedId == R.id.radioRoadworks) {
                    viewModel.setCategoryFilter(SituationCategory.ROADWORKS);
                }
                else {
                    viewModel.setCategoryFilter(null); // All
                }
                refreshList();
            }
        });

        // Listener 4: distance spinner
        distanceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setMaxDistanceMiles(DISTANCE_MILES[position]);
                refreshList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    // Finds which spinner entry matches a distance in miles
    private int getSpinnerPosition(int miles) {
        for (int i = 0; i < DISTANCE_MILES.length; i++) {
            if (DISTANCE_MILES[i] == miles) {
                return i;
            }
        }
        return DISTANCE_MILES.length - 1; // "Any distance"
    }

    // Shows list again after rotating or returning to the app
    @Override
    public void onStart() {
        super.onStart();
        refreshList();
    }

    // Reloads list from the ViewModel with filters applied
    // Called by MainActivity after each load, and by filter listeners
    public void refreshList() {
        // The Activity can call this before fragment's views exist
        if (adapter == null) {
            return;
        }
        List<Situation> filtered = viewModel.getFilteredSituations();
        adapter.setSituations(filtered);
        if (filtered.isEmpty()) {
            // Inform user there is no data, or if hidden by filtering
            if (viewModel.getSituations().isEmpty()) {
                emptyText.setText(R.string.no_situations);
            }
            else {
                emptyText.setText(R.string.no_matches);
            }
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        }
        else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }
    }

    // Row listeners from SituationAdapter -----

    @Override
    public void onSituationClicked(Situation situation) {
        SituationDetailsDialog.newInstance(situation.getId())
                .show(getChildFragmentManager(), "details");
    }

    @Override
    public void onSituationChecked(Situation situation, boolean isChecked) {
        viewModel.setSelected(situation.getId(), isChecked);
    }
}
