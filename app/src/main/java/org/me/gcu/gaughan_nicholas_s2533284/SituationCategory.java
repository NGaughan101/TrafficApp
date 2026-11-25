package org.me.gcu.gaughan_nicholas_s2533284;

// Which feed situation belongs to. Set by the parser caller (not from the XML)
public enum SituationCategory {
    UNPLANNED_EVENT("Incident"),
    ROADWORKS("Roadworks");

    private final String displayName;

    SituationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
