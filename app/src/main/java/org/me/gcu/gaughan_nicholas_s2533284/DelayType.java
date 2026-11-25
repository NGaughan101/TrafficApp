package org.me.gcu.gaughan_nicholas_s2533284;

// Roadworks only. UNKNOWN catches anything new in feed
public enum DelayType {
    DELAYS("Delays"),
    LONG_DELAYS("Long delays"),
    VERY_LONG_DELAYS("Very long delays"),
    UNKNOWN("Unknown");

    private final String displayName;

    DelayType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static DelayType fromXml(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        switch (text.trim()) {
            case "delays":
                return DELAYS;
            case "longDelays":
                return LONG_DELAYS;
            case "veryLongDelays":
                return VERY_LONG_DELAYS;
            default:
                return UNKNOWN;
        }
    }
}
