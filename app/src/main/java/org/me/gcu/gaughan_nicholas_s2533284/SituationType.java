package org.me.gcu.gaughan_nicholas_s2533284;

// <situationRecord xsi:type="...">
// UNKNOWN catches any type not in the example data. Live feed may add new types
// Icons are chosen in the UI layer
public enum SituationType {
    ABNORMAL_TRAFFIC("AbnormalTraffic", "Abnormal Traffic"),
    ACCIDENT("Accident", "Accident"),
    GENERAL_OBSTRUCTION("GeneralObstruction", "Obstruction"),
    POOR_ENVIRONMENT("PoorEnvironmentConditions", "Poor Conditions"),
    ROAD_MANAGEMENT("RoadOrCarriagewayOrLaneManagement", "Road Management"),
    VEHICLE_OBSTRUCTION("VehicleObstruction", "Vehicle Obstruction"),
    MAINTENANCE_WORKS("MaintenanceWorks", "Roadworks"),
    UNKNOWN(null, "Other");

    private final String xmlName;
    private final String displayName;

    SituationType(String xmlName, String displayName) {
        this.xmlName = xmlName;
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static SituationType fromXml(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        String t = text.trim();
        for (SituationType type : values()) {
            if (type.xmlName != null && type.xmlName.equals(t)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    // Format xml name (e.g. "PoorEnvironmentConditions" -> "Poor environment conditions")
    public static String prettify(String camelCase) {
        if (camelCase == null || camelCase.trim().isEmpty()) {
            return null;
        }
        StringBuilder words = new StringBuilder();
        camelCase = camelCase.trim();

        for (int i = 0; i < camelCase.length(); i++) {
            char current = camelCase.charAt(i);

            if (Character.isUpperCase(current) && i > 0) {
                words.append(' ');
            }
            words.append(Character.toLowerCase(current));
        }
        words.setCharAt(0, Character.toUpperCase(words.charAt(0)));
        return words.toString();
    }
}
