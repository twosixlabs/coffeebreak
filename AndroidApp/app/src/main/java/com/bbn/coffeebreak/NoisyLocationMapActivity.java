package com.bbn.coffeebreak;


/*
 * Copyright (c) 2015-2020 Raytheon BBN Technologies
 * All rights reserved.
 *
 * The distribution of this software is controlled by the export notice
 * contained in the README file in this directory.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class NoisyLocationMapActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, OnMapReadyCallback {

    private final static String TAG = "[NoisyLocationMap]";
    private MapView mMapView;
    private MapboxMap mMapboxMap;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    private SeekBar noiseLevel;
    private TextView noiseValue;
    private FusedLocationProviderClient fusedLocationClient;

    //constants for drawing circle overlays
    private static final float EARTH_RADIUS_METERS = 6366710f;
    private static final int DEGREES_IN_CIRCLE = 360;

    //Smoothness of the circle (must be greater than 0 ; the lower the number, the more smooth it is)
    private static final int DEGREES_BETWEEN_POINTS = 4;



    final ResultReceiver mReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_key));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getUsersLocation();

        setContentView(R.layout.activity_noisy_map);
        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        int noise = preferences.getInt(getString(R.string.noise_value), 5);


        noiseValue = (TextView)findViewById(R.id.noise_value);
        noiseValue.setText(String.valueOf(noise) + "km");
        noiseLevel = (SeekBar)findViewById(R.id.noise_seek_bar);
        noiseLevel.setProgress(noise);
        noiseLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                noiseValue.setText(String.valueOf(progress) + "km");
                mMapboxMap.clear();
                drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMapboxMap.clear();
                drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
        if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }
        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.noise_value), noiseLevel.getProgress());
        editor.commit();
        Toast.makeText(this, "Noise value set to: " + noiseLevel.getProgress() + "km", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }


    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
            // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional
            // parameter
            LocationLayerPlugin locationLayerPlugin = new LocationLayerPlugin(mMapView, mMapboxMap);

            // Set the plugin's camera mode
            locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
            getLifecycle().addObserver(locationLayerPlugin);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "User location permission required", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        mMapboxMap = mapboxMap;
        //For now, displaying the user's location using Mapbox SDK causes tile loading issues
        enableLocationPlugin();

        drawCircleOverlay(mMapboxMap, this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), 5000, R.color.transparent_green);

    }

    //Returns the approximate distance between two points on the Earth's surface (in meters)
    public static double getGreatCircleDistance(LatLng a, LatLng b){
        double haversine = Math.pow(Math.sin((Math.toRadians(a.getLatitude()) - Math.toRadians(b.getLatitude()))/2),2) + Math.cos(Math.toRadians(a.getLatitude()))
                * Math.cos(Math.toRadians(b.getLatitude())) * Math.pow(Math.sin((Math.toRadians(a.getLongitude()) - Math.toRadians(b.getLongitude()))/2),2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }

    //draw a circle with a specified radius (in meters) and specified color
    public static void drawCircleOverlay(MapboxMap map, Context context, LatLng location, long radiusInMeters, int colorId){

        /*
        Formula from the Aviation Formulary
            http://www.edwilliams.org/avform.htm#LL
         */

        try{
            ArrayList<LatLng> circlePoints = new ArrayList<>();

            //90 sides... approximate circle
            int numberOfPoints = DEGREES_IN_CIRCLE / DEGREES_BETWEEN_POINTS;

            double distanceRadians = radiusInMeters / EARTH_RADIUS_METERS;
            double centerLatitudeRadians = Math.toRadians(location.getLatitude());
            double centerLongitudeRadians = Math.toRadians(location.getLongitude());


            for (int index = 0; index < numberOfPoints; index++) {
                double degrees = index * DEGREES_BETWEEN_POINTS;
                double degreeRadians = Math.toRadians(degrees);

                double latitudeRadians = Math.asin(Math.sin(centerLatitudeRadians) * Math.cos(distanceRadians) +
                        Math.cos(centerLatitudeRadians) * Math.sin(distanceRadians) * Math.cos(degreeRadians));

                double dlon = Math.atan2(Math.sin(degreeRadians) * Math.sin(distanceRadians) * Math.cos(centerLatitudeRadians),
                        Math.cos(distanceRadians) - Math.sin(centerLatitudeRadians) * Math.sin(latitudeRadians));
                double longitudeRadians = ((centerLongitudeRadians - dlon + Math.PI) % (2*Math.PI)) - Math.PI;

                double pointLat = Math.toDegrees(latitudeRadians);
                double pointLon = Math.toDegrees(longitudeRadians);
                LatLng point = new LatLng(pointLat, pointLon);
                circlePoints.add(point);
            }

            map.addPolygon(new PolygonOptions().addAll(circlePoints).fillColor(context.getColor(colorId)));
        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG, "Unable to draw circle overlay");
        }
    }

    @SuppressLint("MissingPermission")
    public void getUsersLocation(){

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            originLocation = location;
                        }
                    }
        });

    }


}
