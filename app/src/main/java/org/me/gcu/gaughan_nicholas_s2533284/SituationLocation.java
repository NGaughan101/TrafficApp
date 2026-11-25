package org.me.gcu.gaughan_nicholas_s2533284;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SituationLocation {

    // Wrapper types as need to be nullable
    private Double startLatitude;
    private Double startLongitude;
    private Double endLatitude;
    private Double endLongitude;

    private final List<String> affectedRoads = new ArrayList<>();
    private Direction direction = Direction.UNKNOWN;

    public Double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(Double startLatitude) { this.startLatitude = startLatitude; }

    public Double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(Double startLongitude) { this.startLongitude = startLongitude; }

    public Double getEndLatitude() { return endLatitude; }
    public void setEndLatitude(Double endLatitude) { this.endLatitude = endLatitude; }

    public Double getEndLongitude() { return endLongitude; }
    public void setEndLongitude(Double endLongitude) { this.endLongitude = endLongitude; }

    // Both coordinates provided? Check before using
    public boolean hasStart() { return startLatitude != null && startLongitude != null; }
    public boolean hasEnd()   { return endLatitude != null && endLongitude != null; }

    // Roads -----

    public List<String> getAffectedRoads() { return affectedRoads; }

    // Skip duplicates when adding
    public void addRoad(String road) {
        if (road == null) {
            return;
        }
        road = road.trim();
        if (!road.isEmpty() && !affectedRoads.contains(road)) {
            affectedRoads.add(road);
        }
    }

    // Case-insensitive match
    public boolean matchesRoad(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true; // Empty search doesn't filter anything
        }
        String q = query.trim().toLowerCase(Locale.UK);
        for (String road : affectedRoads) {
            if (road.toLowerCase(Locale.UK).contains(q)) {
                return true;
            }
        }
        return false;
    }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    // Distance -----

    // Distance in metres from given point to the nearest end of the situation (start or end)
    // null if there are no coordinates
    public Double distanceTo(double latitude, double longitude) {
        Double nearest = null;
        if (hasStart()) {
            nearest = metresBetween(latitude, longitude, startLatitude, startLongitude);
        }
        if (hasEnd()) {
            double d = metresBetween(latitude, longitude, endLatitude, endLongitude);
            if (nearest == null || d < nearest) {
                nearest = d;
            }
        }
        return nearest;
    }

    private static double metresBetween(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1]; // distanceBetween returns an array!
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }
}
