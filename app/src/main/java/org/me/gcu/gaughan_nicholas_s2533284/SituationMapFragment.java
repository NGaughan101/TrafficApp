package org.me.gcu.gaughan_nicholas_s2533284;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

// Shows the situations ticked in the list on a Google map
public class SituationMapFragment extends Fragment implements OnMapReadyCallback {

    // Default when nothing ticked
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);
    private static final float SCOTLAND_ZOOM = 6f;
    private static final float SINGLE_POINT_ZOOM = 12f;
    private static final int BOUNDS_PADDING_DP = 48; // space between the outer markers and edge
    private static final float LINE_WIDTH = 10f;

    private GoogleMap mMap; // null until onMapReady
    private TrafficViewModel viewModel;

    public SituationMapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_situation_map, container, false);

        // Same ViewModel as MainActivity and the list to read ticked situations
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // Map fragment is inside getChildFragmentManager()
        // getMapAsync loads the map in the background and calls onMapReady on the UI thread
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return v;
    }

    // Called on UI thread when map is ready to be used
    // The map is created at launch before anything is ticked and again after each rotation
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Tapping a marker shows its title and description
        // tapping that opens the details dialog
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(@NonNull Marker marker) {
                String id = (String) marker.getTag();
                SituationDetailsDialog.newInstance(id).show(getChildFragmentManager(), "details");
            }
        });

        List<LatLng> points = drawSituations();
        moveCamera(points);
    }

    // Called by MainActivity when "View Selected On Map" is pressed
    // redraws the ticked situations and moves the camera to show them
    public void showSelectedSituations() {
        if (mMap == null) {
            return; // map not ready yet
        }
        mMap.clear();
        List<LatLng> points = drawSituations();
        if (points.isEmpty() && viewModel.hasSelection()) {
            Toast.makeText(getContext(), R.string.map_no_locations, Toast.LENGTH_SHORT).show();
        }
        moveCamera(points);
    }

    // Redraws the markers after a refresh (called by MainActivity)
    // Camera isn't moved
    public void refreshMap() {
        if (mMap == null) {
            return; // map not ready yet
        }
        mMap.clear();
        drawSituations();
    }

    // Adds a marker for each ticked situation, plus a line (if start/end point exists)
    // Returns all points so the camera can be moved to show them
    private List<LatLng> drawSituations() {
        List<LatLng> points = new ArrayList<>();

        for (Situation s : viewModel.getSelectedSituations()) {
            SituationLocation location = s.getLocation();
            LatLng start = null;
            LatLng end = null;
            if (location.hasStart()) {
                start = new LatLng(location.getStartLatitude(), location.getStartLongitude());
            }
            if (location.hasEnd()) {
                end = new LatLng(location.getEndLatitude(), location.getEndLongitude());
            }

            // Marker at the start (end if no start)
            LatLng markerPoint = start;
            if (markerPoint == null) {
                markerPoint = end;
            }
            if (markerPoint == null) {
                continue; // can't be shown
            }

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(markerPoint)
                    .title(getString(R.string.situation_title, s.getTypeDisplayName(), s.getRoadsDisplay()))
                    .snippet(s.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(s.getCategory()))));
            if (marker != null) {
                marker.setTag(s.getId()); // lets the info window listener find the situation
            }
            points.add(markerPoint);

            // Line from start to end
            if (start != null && end != null && !start.equals(end)) {
                int colour = ContextCompat.getColor(requireContext(),
                        SituationIcons.getColorRes(s.getCategory()));
                mMap.addPolyline(new PolylineOptions()
                        .add(start, end)
                        .width(LINE_WIDTH)
                        .color(colour));
                points.add(end);
            }
        }
        return points;
    }

    // Red markers for incidents
    // orange for roadworks
    private float getMarkerHue(SituationCategory category) {
        if (category == SituationCategory.ROADWORKS) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        }
        return BitmapDescriptorFactory.HUE_RED;
    }

    // Moves camera so all points are visible
    private void moveCamera(List<LatLng> points) {
        if (points.isEmpty()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, SCOTLAND_ZOOM));
        }
        else if (points.size() == 1) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), SINGLE_POINT_ZOOM));
        }
        else {
            // The smallest rectangle containing every point
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            final LatLngBounds bounds = builder.build();
            final int padding = (int) (BOUNDS_PADDING_DP * getResources().getDisplayMetrics().density);

            // newLatLngBounds needs to know the map's size on screen
            // This isn't known until the map has been drawn
            // wait for it to finish loading first
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                }
            });
        }
    }
}
