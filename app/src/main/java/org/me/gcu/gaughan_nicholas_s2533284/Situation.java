package org.me.gcu.gaughan_nicholas_s2533284;

import java.time.OffsetDateTime;

public class Situation {
    private final SituationCategory category;
    private final SituationLocation location = new SituationLocation();

    private String id;
    private SituationType type = SituationType.UNKNOWN;
    private String rawType; // raw xsi:type
    private String description;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime; // roadworks only

    // Wrapper types. Null means not listed
    private Integer operationalLanes; // unplanned events only
    private Integer restrictedLanes; // unplanned events only
    private DelayType delay; // roadworks only

    public Situation(SituationCategory category) {
        this.category = category;
    }

    public SituationCategory getCategory() { return category; }
    public SituationLocation getLocation() { return location; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Type -----

    public SituationType getType() { return type; }

    // Takes raw xsi:type and works out matching enum value
    public void setType(String rawType) {
        this.rawType = rawType;
        this.type = SituationType.fromXml(rawType);
    }

    // Readable type name. Unknown types fall back to the raw XML name
    public String getTypeDisplayName() {
        if (type == SituationType.UNKNOWN && rawType != null) {
            return SituationType.prettify(rawType);
        }
        return type.getDisplayName();
    }

    // Description -----

    public String getDescription() { return description; }

    public void setDescription(String description) {
        this.description = description.trim();
    }

    // Roads -----

    // Combine/format affected road list to string
    public String getRoadsDisplay() {
        if (location.getAffectedRoads().isEmpty()) {
            return "Unknown road";
        }
        return String.join(", ", location.getAffectedRoads());
    }

    // Times and impact -----

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public Integer getOperationalLanes() { return operationalLanes; }
    public void setOperationalLanes(Integer operationalLanes) { this.operationalLanes = operationalLanes; }

    public Integer getRestrictedLanes() { return restrictedLanes; }
    public void setRestrictedLanes(Integer restrictedLanes) { this.restrictedLanes = restrictedLanes; }

    public DelayType getDelay() { return delay; }
    public void setDelay(DelayType delay) { this.delay = delay; }

    @Override
    public String toString() {
        return "[" + category + "] " + getTypeDisplayName()
                + " (" + id + ")"
                + "\n  " + description
                + "\n  roads=" + getRoadsDisplay()
                + " direction=" + location.getDirection()
                + "\n  start=(" + location.getStartLatitude() + ", " + location.getStartLongitude() + ")"
                + " end=(" + location.getEndLatitude() + ", " + location.getEndLongitude() + ")"
                + "\n  startTime=" + startTime + " endTime=" + endTime
                + "\n  delay=" + delay
                + " operationalLanes=" + operationalLanes
                + " restrictedLanes=" + restrictedLanes;
    }
}
