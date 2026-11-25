package org.me.gcu.gaughan_nicholas_s2533284;

// <tpegDirection>. UNKNOWN catches anything new in feed
public enum Direction {
    NORTH_BOUND("Northbound"),
    SOUTH_BOUND("Southbound"),
    EAST_BOUND("Eastbound"),
    WEST_BOUND("Westbound"),
    BOTH_WAYS("Both directions"),
    UNKNOWN("Unknown");

    private final String displayName;

    Direction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static Direction fromXml(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        switch (text.trim()) {
            case "northBound":
                return NORTH_BOUND;
            case "southBound":
                return SOUTH_BOUND;
            case "eastBound":
                return EAST_BOUND;
            case "westBound":
                return WEST_BOUND;
            case "bothWays":
                return BOTH_WAYS;
            default:
                return UNKNOWN;
        }
    }
}
