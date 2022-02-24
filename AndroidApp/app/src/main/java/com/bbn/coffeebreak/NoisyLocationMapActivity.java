/*
 * Copyright 2021 Raytheon BBN Technologies Corp.
 * Copyright 2021 Two Six Labs, LLC DBA Two Six Technologies
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NoisyLocationMapActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener, OnMapReadyCallback {

    private final static String TAG = "[NoisyLocationMap]";
    private MapView mMapView;
    private MapboxMap mMapboxMap;

    private static boolean mMockLocation;
    private static long circleId = 0;

    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    private Button mockButton;
    private SeekBar noiseLevel;
    private TextView noiseValue;
    private LocationManager mLocationManager;

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

        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        getUsersLocation();

        setContentView(R.layout.activity_noisy_map);

        mMapView = findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        noiseValue = (TextView)findViewById(R.id.noise_value);
        mockButton = (Button)findViewById(R.id.mock_enable);
        noiseLevel = (SeekBar)findViewById(R.id.noise_seek_bar);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString("current_screen", "activity_noisy_map");
        editor.commit();

        int noise = preferences.getInt(getString(R.string.noise_value), 5);
        mMockLocation = preferences.getBoolean(getString(R.string.mock_location), getResources().getBoolean(R.bool.mock_location));
        if(mMockLocation){
            mockButton.setBackground(getDrawable(R.drawable.green_rectangle));
            mockButton.setText("Mock location: ON ");
        }

        noiseValue.setText(String.valueOf(noise) + "km");
        noiseLevel.setProgress(noise);
        noiseLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                noiseValue.setText(String.valueOf(progress) + "km");
                mMapboxMap.removeAnnotation(circleId);
                if(mMockLocation){
                    Marker m = mMapboxMap.getMarkers().get(0);
                    drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(m.getPosition().getLatitude(), m.getPosition().getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
                }else{
                    drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mMapboxMap.removeAnnotation(circleId);
                if(mMockLocation){
                    Marker m = mMapboxMap.getMarkers().get(0);
                    drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(m.getPosition().getLatitude(), m.getPosition().getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
                }else{
                    drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), seekBar.getProgress() * 1000, R.color.transparent_green);
                }

                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(getString(R.string.noise_value), noiseLevel.getProgress());
                editor.commit();

                Toast.makeText(NoisyLocationMapActivity.this, "Noise value set to: " + noiseLevel.getProgress() + "km", Toast.LENGTH_LONG).show();
            }
        });

        mockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMapboxMap.getMarkers().size() < 1){
                    AlertDialog alertDialog = new AlertDialog.Builder(NoisyLocationMapActivity.this).create();
                    alertDialog.setTitle("Setting a mock location");
                    alertDialog.setMessage("To set a mock location, long press anywhere on the map to drop a marker.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                    Toast.makeText(NoisyLocationMapActivity.this, "Long press to set mock location first", Toast.LENGTH_LONG).show();
                }else{
                    Marker mockLocation = mMapboxMap.getMarkers().get(0);
                    mMockLocation = !mMockLocation;
                    editor.putBoolean(getString(R.string.mock_location), mMockLocation);
                    editor.commit();
                    if(mMockLocation){
                        mockButton.setText("Mock location: ON ");
                        mMapboxMap.removeAnnotation(circleId);
                        mockButton.setBackground(getDrawable(R.drawable.green_rectangle));
                        drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(mockLocation.getPosition().getLatitude(), mockLocation.getPosition().getLongitude()), noiseLevel.getProgress() * 1000, R.color.transparent_green);
                    }else{
                        mockButton.setText("Mock location: OFF");
                        mMapboxMap.removeAnnotation(circleId);
                        mockButton.setBackground(getDrawable(R.drawable.grey_rectangle));

                        if (originLocation != null) {
                            drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), noiseLevel.getProgress() * 1000, R.color.transparent_green);
                        }
                    }
                }
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
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.noise_value), noiseLevel.getProgress());
        editor.commit();
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
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional
            // parameter
            LocationLayerPlugin locationLayerPlugin = new LocationLayerPlugin(mMapView, mMapboxMap);

            // Set the plugin's camera mode
            //locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
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
        noiseLevel = (SeekBar)findViewById(R.id.noise_seek_bar);
        mockButton = (Button)findViewById(R.id.mock_enable);

        // For now, displaying the user's location using Mapbox SDK causes tile loading issues
        enableLocationPlugin();

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (mMockLocation) {
            MarkerOptions m = new MarkerOptions().setTitle("Mocked location").setPosition(
                    new LatLng(preferences.getFloat(getString(R.string.mock_latitude), 0.0f),
                            preferences.getFloat(getString(R.string.mock_longitude), 0.0f)));

            mMapboxMap.addMarker(m);
            drawCircleOverlay(mMapboxMap, this, new LatLng(m.getPosition().getLatitude(), m.getPosition().getLongitude()), preferences.getInt(getString(R.string.noise_value), 5) * 1000, R.color.transparent_green);
            mMapboxMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(m.getPosition().getLatitude(), m.getPosition().getLongitude()))
                    .zoom(12)
                    .tilt(10)
                    .build());
        } else if (originLocation != null) {
            drawCircleOverlay(mMapboxMap, this, new LatLng(originLocation.getLatitude(), originLocation.getLongitude()), preferences.getInt(getString(R.string.noise_value), 5) * 1000, R.color.transparent_green);
            mMapboxMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()))
                    .zoom(12)
                    .tilt(10)
                    .build());
        }

        mapboxMap.addOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng point) {
                Log.d(TAG, point.getLatitude() + "," + point.getLongitude());

                AlertDialog alertDialog = new AlertDialog.Builder(NoisyLocationMapActivity.this).create();
                alertDialog.setTitle("Set mock location: ");
                alertDialog.setMessage("Are you sure you want to set the mocked location to: " + point.getLatitude()
                        + "," + point.getLongitude());
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Restart activity with new location
                                // clear all markers
                                for(Marker m : mMapboxMap.getMarkers()){
                                    mMapboxMap.removeMarker(m);
                                }
                                MarkerOptions marker = new MarkerOptions().setPosition(point)
                                        .setTitle("Mocked location");


                                mMapboxMap.clear();
                                drawCircleOverlay(mMapboxMap, NoisyLocationMapActivity.this, new LatLng(marker.getPosition().getLatitude(), marker.getPosition().getLongitude()), noiseLevel.getProgress() * 1000, R.color.transparent_green);
                                mMapboxMap.addMarker(marker);

                                mockButton.setText("Mock location: ON ");
                                mockButton.setBackground(getDrawable(R.drawable.green_rectangle));

                                editor.putFloat(getString(R.string.mock_latitude), (float)marker.getPosition().getLatitude());
                                editor.putFloat(getString(R.string.mock_longitude), (float)marker.getPosition().getLongitude());
                                editor.putBoolean(getString(R.string.mock_location), true);
                                mMockLocation = true;
                                editor.commit();
                                Log.d(TAG, "Setting in preferences: " + marker.getPosition());

                            }
                        });
                alertDialog.show();
            }
        });
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
            PolygonOptions circle = new PolygonOptions().addAll(circlePoints).fillColor(context.getColor(colorId));
            map.addPolygon(circle);
            circleId = circle.getPolygon().getId();
        }catch(Exception e){
            e.printStackTrace();
            Log.d(TAG, "Unable to draw circle overlay");
        }
    }

    @SuppressLint("MissingPermission")
    public void getUsersLocation() {
        if (mLocationManager != null) {
            Location lastLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLoc != null) {
                originLocation = lastLoc;
                return;
            }

            lastLoc = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (lastLoc != null) {
                originLocation = lastLoc;
            }
        }
    }
}
