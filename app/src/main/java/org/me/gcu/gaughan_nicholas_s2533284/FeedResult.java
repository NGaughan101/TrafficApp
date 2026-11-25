package org.me.gcu.gaughan_nicholas_s2533284;

import java.util.List;

// Collects everything from LoadFeedsTask.run(). Sent as message to UI thread
public class FeedResult {

    private final List<Situation> situations;
    private final LoadStatus status;
    private final long updatedTime; // when oldest data was downloaded (ms)
    private final String message; // e.g. "Roadworks: showing cached data", empty on success

    public FeedResult(List<Situation> situations, LoadStatus status, long updatedTime, String message) {
        this.situations = situations;
        this.status = status;
        this.updatedTime = updatedTime;
        this.message = message;
    }

    public List<Situation> getSituations() { return situations; }
    public LoadStatus getStatus() { return status; }
    public long getUpdatedTime() { return updatedTime; }
    public String getMessage() { return message; }
}
