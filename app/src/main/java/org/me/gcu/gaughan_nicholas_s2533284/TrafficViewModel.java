package org.me.gcu.gaughan_nicholas_s2533284;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Holds the latest loaded data so it survives screen rotation
public class TrafficViewModel extends ViewModel {

    // Automatic update intervals
    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000; // 1 hour after a good load
    private static final long RETRY_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes after a failed load

    private List<Situation> situations = new ArrayList<>();
    private LoadStatus status = null; //  null until first load finishes
    private String message = "";
    private long lastUpdateTime = 0; // when the data was downloaded
    private long lastLoadTime = 0; // when last load finished. Used to time the next refresh

    // Ticked situation Ids, instead of objects
    // Retained on refresh
    private final Set<String> selectedIds = new HashSet<>();

    // Is map (rather than list) showing in portrait mode?
    private boolean mapShowing = false;

    // Filters -----
    // Kept after rotation and journey to the map
    // Distances are measured from a fixed point (GCU)
    private static final double GCU_LATITUDE = 55.8667;
    private static final double GCU_LONGITUDE = -4.2503;
    private static final double METRES_PER_MILE = 1609.344;

    private SituationCategory categoryFilter = null; // null = all categories
    private String searchText = ""; // all roads
    private int maxDistanceMiles = 0; // 0 is any distance

    // Called on UI thread when LoadFeedsTask sends result
    public void setResult(FeedResult result) {
        situations = result.getSituations();
        status = result.getStatus();
        message = result.getMessage();
        lastUpdateTime = result.getUpdatedTime();
        lastLoadTime = System.currentTimeMillis();
        removeOldSelections();
    }

    // Forget ticks for situations that are no longer in the feed (finished incident/roadworks)
    private void removeOldSelections() {
        Set<String> currentIds = new HashSet<>();
        for (Situation s : situations) {
            currentIds.add(s.getId());
        }
        selectedIds.retainAll(currentIds);
    }

    public List<Situation> getSituations() { return situations; }
    public LoadStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public long getLastUpdateTime() { return lastUpdateTime; }

    public boolean hasData() {
        return status != null;
    }

    // For details dialog. Null if no longer in feed
    public Situation getSituationById(String id) {
        if (id == null) {
            return null;
        }
        for (Situation s : situations) {
            if (id.equals(s.getId())) {
                return s;
            }
        }
        return null;
    }

    // Selected situations -----

    public boolean isSelected(String id) {
        return selectedIds.contains(id);
    }

    public void setSelected(String id, boolean selected) {
        if (selected) {
            selectedIds.add(id);
        }
        else {
            selectedIds.remove(id);
        }
    }

    public boolean hasSelection() {
        return !selectedIds.isEmpty();
    }

    // The ticked situations, in list order. Used by the map.
    public List<Situation> getSelectedSituations() {
        List<Situation> selected = new ArrayList<>();
        for (Situation s : situations) {
            if (selectedIds.contains(s.getId())) {
                selected.add(s);
            }
        }
        return selected;
    }

    public boolean isMapShowing() { return mapShowing; }
    public void setMapShowing(boolean mapShowing) { this.mapShowing = mapShowing; }

    // Filters -----

    public SituationCategory getCategoryFilter() { return categoryFilter; }
    public void setCategoryFilter(SituationCategory categoryFilter) { this.categoryFilter = categoryFilter; }

    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }

    public int getMaxDistanceMiles() { return maxDistanceMiles; }
    public void setMaxDistanceMiles(int maxDistanceMiles) { this.maxDistanceMiles = maxDistanceMiles; }

    // Situations are passed through filters
    // returns filtered list of situations
    public List<Situation> getFilteredSituations() {
        List<Situation> filtered = new ArrayList<>();
        for (Situation s : situations) {
            if (matchesFilters(s)) {
                filtered.add(s);
            }
        }
        return filtered;
    }

    private boolean matchesFilters(Situation s) {
        // Category
        if (categoryFilter != null && s.getCategory() != categoryFilter) {
            return false;
        }
        // Road search. Empty search matches all
        if (!s.getLocation().matchesRoad(searchText)) {
            return false;
        }
        // Distance from GCU
        if (maxDistanceMiles > 0) {
            Double metres = s.getLocation().distanceTo(GCU_LATITUDE, GCU_LONGITUDE);
            if (metres == null) {
                return false; // no coordinates
            }
            if (metres > maxDistanceMiles * METRES_PER_MILE) {
                return false;
            }
        }
        return true;
    }

    // How long to wait before next automatic refresh
    // 0 = load now
    public long getDelayUntilNextRefresh() {
        if (!hasData()) {
            return 0;
        }
        long interval;
        if (status == LoadStatus.SUCCESS) {
            interval = REFRESH_INTERVAL_MS;
        }
        else {
            interval = RETRY_INTERVAL_MS;
        }
        long timeSinceLoad = System.currentTimeMillis() - lastLoadTime;
        long delay = interval - timeSinceLoad;
        if (delay < 0) {
            delay = 0;
        }
        return delay;
    }
}
