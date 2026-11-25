package org.me.gcu.gaughan_nicholas_s2533284;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements SituationListFragment.SituationListListener {

    // Positions of the ViewFlipper's children
    private static final int LOADING_SCREEN = 0;
    private static final int CONTENT_SCREEN = 1;

    // Positions of the content ViewFlipper's children (portrait only)
    private static final int LIST_SCREEN = 0;
    private static final int MAP_SCREEN = 1;

    private ViewFlipper viewFlipper;
    private ViewFlipper contentFlipper; // null in landscape
    private ProgressBar progressBar;
    private TextView loadingText;

    private TrafficRepository repository;
    private TrafficViewModel viewModel; // Keeps the loaded data when the screen rotates
    private ExecutorService executor; // One background thread reused for every load. Queued loads run consecutively
    private Handler uiHandler;
    private boolean isVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Keep the content clear of the status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewFlipper = findViewById(R.id.viewFlipper);
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);

        // Only the portrait layout. Null means landscape (list and map displayed)
        contentFlipper = findViewById(R.id.contentFlipper);

        // Add both fragments on create only
        // After rotation the FragmentManager restores them into their containers
        // (both layouts have both containers),
        // Adding again would stack second copies on top
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.list_container, new SituationListFragment())
                    .add(R.id.map_container, new SituationMapFragment())
                    .commit();
        }

        // Created the first time, then the same instance is returned after each rotation
        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        // Back returns from the map to the list (portrait only (backToList))
        getOnBackPressedDispatcher().addCallback(this, backToList);
        // After rotation, show the map again if it was showing
        if (contentFlipper != null && viewModel.isMapShowing()) {
            showMap();
        }

        repository = new TrafficRepository(this);
        executor = Executors.newSingleThreadExecutor();
        createUiHandler();
    }

    // Handler attached to the main thread's Looper: messages sent by LoadFeedsTask handled here
    private void createUiHandler() {
        uiHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                // MSG_PROGRESS: loading bar progress percentage
                // MSG_RESULT: FeedResult object
                if (msg.what == LoadFeedsTask.MSG_PROGRESS) {
                    // Only visible during the first load
                    // bar still updates after, but is hidden by content
                    progressBar.setProgress(msg.arg1);
                    loadingText.setText(getString(R.string.loading_progress, msg.arg1));
                }
                else if (msg.what == LoadFeedsTask.MSG_RESULT) {
                    FeedResult result = (FeedResult) msg.obj;
                    viewModel.setResult(result);
                    showData();
                    // Switch to the content even if the load failed
                    viewFlipper.setDisplayedChild(CONTENT_SCREEN);
                    showLoadProblem();
                    scheduleNextRefresh();
                }
            }
        };
    }

    // Load data on the background thread. Re-scheduled after each result (scheduleNextRefresh)
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("MainActivity", "Starting automatic refresh");
            executor.execute(new LoadFeedsTask(repository, uiHandler));
        }
    };

    // Refesh every hour after a good load, sooner if feed could not be downloaded
    // ViewModel works out how much time is left
    private void scheduleNextRefresh() {
        uiHandler.removeCallbacks(refreshRunnable);
        if (isVisible) {
            long delay = viewModel.getDelayUntilNextRefresh();
            Log.d("MainActivity", "Next refresh in " + (delay / 1000) + " seconds");
            uiHandler.postDelayed(refreshRunnable, delay);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        isVisible = true;
        // After rotation the ViewModel retains data. Show it again instead of reloading
        // Empty on first launch, so the delay is 0 and a load starts straight away.
        if (viewModel.hasData()) {
            showData();
            viewFlipper.setDisplayedChild(CONTENT_SCREEN);
        }
        else {
            progressBar.setProgress(0);
            loadingText.setText(R.string.loading);
            viewFlipper.setDisplayedChild(LOADING_SCREEN);
        }
        scheduleNextRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Only update while the app is visible
        isVisible = false;
        uiHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
        executor.shutdown();
    }

    // Shows the ViewModels data. Called after a load and after rotation.
    private void showData() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (viewModel.getSituations().isEmpty()) {
                // Nothing loaded, so no update time
                actionBar.setSubtitle(null); // null disables subtitle
            }
            else {
                String time = new SimpleDateFormat("HH:mm", Locale.UK).format(new Date(viewModel.getLastUpdateTime()));
                actionBar.setSubtitle(getString(R.string.updated_at, time));
            }
        }

        // Both fragments always exist, even if one is hidden. Reload both from ViewModel
        SituationListFragment list = getListFragment();
        if (list != null) {
            list.refreshList();
        }
        SituationMapFragment map = getMapFragment();
        if (map != null) {
            map.refreshMap();
        }
    }

    private SituationListFragment getListFragment() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.list_container);
        if (f instanceof SituationListFragment) {
            return (SituationListFragment) f;
        }
        return null;
    }

    private SituationMapFragment getMapFragment() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (f instanceof SituationMapFragment) {
            return (SituationMapFragment) f;
        }
        return null;
    }

    // Called by SituationListFragment when "View Selected On Map" is pressed
    // Portrait: the map replaces the list. Landscape: update map (already visible)
    @Override
    public void onViewSelectedOnMap() {
        Log.d("MainActivity", "Show on map: " + viewModel.getSelectedSituations().size() + " situations");
        if (contentFlipper != null) {
            showMap();
        }
        SituationMapFragment map = getMapFragment();
        if (map != null) {
            map.showSelectedSituations();
        }
    }

    // Portrait: switching between list and the map -----

    // Pressing back on map returns to list. Only enabled while the map is visible
    // Otherwise back closes the app
    private final OnBackPressedCallback backToList = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            showList();
        }
    };

    private void showMap() {
        contentFlipper.setDisplayedChild(MAP_SCREEN);
        viewModel.setMapShowing(true); // remembered across rotation
        backToList.setEnabled(true);
    }

    private void showList() {
        contentFlipper.setDisplayedChild(LIST_SCREEN);
        viewModel.setMapShowing(false);
        backToList.setEnabled(false);
    }

    // Tells the user which feed could not be downloaded
    // Called only when a load finishes, so it is not repeated on every rotation
    private void showLoadProblem() {
        if (viewModel.getStatus() != LoadStatus.SUCCESS) {
            Toast.makeText(this, viewModel.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
