package org.me.gcu.gaughan_nicholas_s2533284;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

// Loads both feeds on a background thread (run by Executor in MainActivity)
// Progress (for loading bar) and the FeedResult sent as messages to handler UI thread looper
public class LoadFeedsTask implements Runnable {

    // Message types (msg.what)
    public static final int MSG_PROGRESS = 1; // Loading progress % (arg1)
    public static final int MSG_RESULT = 2; // FeedResult (obj)

    private final TrafficRepository repository;
    private final Handler uiHandler;

    // Run state. New task object for every load
    private final List<String> messages = new ArrayList<>();
    private boolean usedCache = false;
    private boolean feedMissing = false;
    private long oldestData = System.currentTimeMillis();

    public LoadFeedsTask(TrafficRepository repository, Handler uiHandler) {
        this.repository = repository;
        this.uiHandler = uiHandler;
    }

    @Override
    public void run() {
        Log.d("LoadFeedsTask", "Loading feeds in background");
        List<Situation> situations = new ArrayList<>();
        sendProgress(10); // update loading bar progress

        // Load and parse unplanned events xml
        String unplannedXml = getFeedXml("Incidents",
                TrafficRepository.UNPLANNED_EVENTS_URL, TrafficRepository.UNPLANNED_CACHE_FILE);
        sendProgress(30);
        if (unplannedXml != null) {
            situations.addAll(repository.parse(unplannedXml, SituationCategory.UNPLANNED_EVENT));
        }
        sendProgress(45);

        // Load and parse roadworks xml. Merge with unplanned events
        String roadworksXml = getFeedXml("Roadworks",
                TrafficRepository.ROADWORKS_URL, TrafficRepository.ROADWORKS_CACHE_FILE);
        if (roadworksXml != null) {
            situations.addAll(repository.parse(roadworksXml, SituationCategory.ROADWORKS));
        }
        sendProgress(80);

        LoadStatus status;
        if (feedMissing) {
            status = LoadStatus.ERROR;
        }
        else if (usedCache) {
            status = LoadStatus.OFFLINE;
        }
        else {
            status = LoadStatus.SUCCESS;
        }

        FeedResult result = new FeedResult(situations, status, oldestData, String.join("\n", messages));
        Log.d("LoadFeedsTask", status + ": " + situations.size() + " situations");
        sendProgress(100); // Loading bar progress complete

        // Send FeedResult to the UI thread
        Message message = uiHandler.obtainMessage();
        message.what = MSG_RESULT;
        message.obj = result;
        uiHandler.sendMessage(message);
    }

    // Download and cache feed. Failed download reverts to cached
    // Return null if both fail
    private String getFeedXml(String label, String url, String cacheFile) {
        String xml = repository.downloadXml(url);
        if (xml != null) {
            repository.saveCache(cacheFile, xml);
            return xml;
        }

        xml = repository.readCache(cacheFile);
        if (xml != null) {
            usedCache = true;
            oldestData = Math.min(oldestData, repository.getCacheTime(cacheFile));
            messages.add(label + ": showing cached data");
        } else {
            feedMissing = true;
            messages.add(label + ": unavailable");
        }
        return xml;
    }

    // Send loading bar progress to UI thread
    private void sendProgress(int percent) {
        Message message = uiHandler.obtainMessage();
        message.what = MSG_PROGRESS;
        message.arg1 = percent; // integer
        uiHandler.sendMessage(message);
    }
}
