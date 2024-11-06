package com.example.projectpgpbacara9;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.graphics.BitmapFactory;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import android.location.Location;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationComponent locationComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Mapbox instance
        Mapbox.getInstance(this);

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(mapboxMap -> {
            this.mapboxMap = mapboxMap;

            // Set minimum and maximum zoom levels
            mapboxMap.setMinZoomPreference(5);
            mapboxMap.setMaxZoomPreference(18);

            // Define the TMS URL and style JSON for map layer
            String tmsUrl = "https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}";
            String styleJson = "{\n" +
                    "  \"version\": 8,\n" +
                    "  \"sources\": {\n" +
                    "    \"tms-tiles\": {\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"tiles\": [\"" + tmsUrl + "\"],\n" +
                    "      \"tileSize\": 256\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"layers\": [\n" +
                    "    {\n" +
                    "      \"id\": \"tms-tiles\",\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"source\": \"tms-tiles\",\n" +
                    "      \"minzoom\": 0,\n" +
                    "      \"maxzoom\": 22\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            mapboxMap.setStyle(new Style.Builder().fromJson(styleJson), style -> {

                // Add markers and enable location component
                style.addImage("marker-icon-id", BitmapFactory.decodeResource(getResources(), R.drawable.iconslocation));
                addMarkers(style);

                // Enable location tracking
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    enableLocationComponent(mapboxMap, style);
                }
            });

            // Set up zoom and location controls
            setupZoomControls();
            setupLocationButton();
        });
    }

    private void setupZoomControls() {
        FloatingActionButton zoomInButton = findViewById(R.id.btn_zoom_in);
        FloatingActionButton zoomOutButton = findViewById(R.id.btn_zoom_out);

        zoomInButton.setOnClickListener(v -> {
            if (mapboxMap != null) {
                mapboxMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (mapboxMap != null) {
                mapboxMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });
    }


    private void addMarkers(Style style) {
        List<LatLng> locations = Arrays.asList(
                new LatLng(-6.597411747869523, 106.79954502190401),
                new LatLng(-6.5951501278058995, 106.79134899885425),
                new LatLng(-6.552345730663087, 106.72280278334873)
        );

        Feature[] features = new Feature[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            LatLng location = locations.get(i);
            features[i] = Feature.fromGeometry(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
        }
        FeatureCollection featureCollection = FeatureCollection.fromFeatures(features);
        GeoJsonSource geoJsonSource = new GeoJsonSource("marker-source", featureCollection);
        style.addSource(geoJsonSource);

        SymbolLayer symbolLayer = new SymbolLayer("marker-layer", "marker-source")
                .withProperties(
                        PropertyFactory.iconImage("marker-icon-id"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true)
                );
        style.addLayer(symbolLayer);
    }

    private void setupLocationButton() {
        FloatingActionButton locationButton = findViewById(R.id.btn_location);
        locationButton.setOnClickListener(v -> {
            if (locationComponent != null && locationComponent.getLastKnownLocation() != null) {
                Location lastKnownLocation = locationComponent.getLastKnownLocation();
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                        .zoom(15) // Adjust zoom level as desired
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableLocationComponent(MapboxMap mapboxMap, Style style) {
        locationComponent = this.mapboxMap.getLocationComponent();
        locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, style).build()
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        }
    }

    // Other methods (onRequestPermissionsResult, addMarkers, etc.) remain unchanged



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mapView.getMapAsync(mapboxMap -> mapboxMap.getStyle(style -> enableLocationComponent(mapboxMap, style)));
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
