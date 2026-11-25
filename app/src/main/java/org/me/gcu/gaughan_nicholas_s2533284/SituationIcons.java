package org.me.gcu.gaughan_nicholas_s2533284;

// Selects icon and colour situations
// Used by list and details dialog
public class SituationIcons {

    // One icon for each situation type
    public static int getIconRes(SituationType type) {
        switch (type) {
            case ACCIDENT:
                return R.drawable.ic_accident;
            case ABNORMAL_TRAFFIC:
                return R.drawable.ic_traffic;
            case GENERAL_OBSTRUCTION:
                return R.drawable.ic_obstruction;
            case POOR_ENVIRONMENT:
                return R.drawable.ic_weather;
            case ROAD_MANAGEMENT:
                return R.drawable.ic_road_management;
            case VEHICLE_OBSTRUCTION:
                return R.drawable.ic_vehicle;
            case MAINTENANCE_WORKS:
                return R.drawable.ic_roadworks;
            default:
                return R.drawable.ic_other;
        }
    }

    // red for incidents, orange for roadworks
    // values-night/colors.xml has lighter versions
    public static int getColorRes(SituationCategory category) {
        if (category == SituationCategory.ROADWORKS) {
            return R.color.roadworks;
        }
        return R.color.incident;
    }

    // Colour for the delay text in the details dialog
    // yellow, orange, red
    public static int getDelayColorRes(DelayType delay) {
        switch (delay) {
            case LONG_DELAYS:
                return R.color.delay_long;
            case VERY_LONG_DELAYS:
                return R.color.delay_very_long;
            default:
                return R.color.delay_some;
        }
    }
}
