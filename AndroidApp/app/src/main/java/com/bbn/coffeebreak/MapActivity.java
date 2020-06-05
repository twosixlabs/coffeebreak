package com.bbn.coffeebreak;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.security.SecureRandom;
import java.util.Objects;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private static final float METERS_PER_LATITUDE = 111380f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), "YOUR_MAPBOX_ACCESS_TOKEN_GOES_HERE");
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {

                mapboxMap.setStyle(Style.LIGHT);
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(getIntent().getFloatExtra("latitude",0.0f), getIntent().getFloatExtra("longitude",0.0f)))
                        .zoom(15)
                        .tilt(20)
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 2000);
                mapboxMap.addMarker(new MarkerOptions().position(new LatLng(getIntent().getFloatExtra("latitude",0.0f),
                        getIntent().getFloatExtra("longitude",0.0f)))).setTitle(getIntent().getStringExtra("address"));

                TextView addressDisplay = (TextView) findViewById(R.id.address_display);
                addressDisplay.setText(getIntent().getStringExtra("address"));
            }
        });

        Button copy = (Button) findViewById(R.id.copy_button);
        Objects.requireNonNull(copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("address_text", getIntent().getStringExtra("address"));
                clipboard.setPrimaryClip(clip);

                Toast.makeText(getApplicationContext(), "Address Copied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Add the mapView's own lifecycle methods to the activity's lifecycle methods
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
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

    // Gets a noisy location by picking a uniform spot on a circle with a given radius (and vertex of given location)
    // radius given in kilometers
    public static EncodedLatLon getNoisyLocation(EncodedLatLon l, int radius){
        SecureRandom generator = new SecureRandom();
        double r = 1000 * radius * Math.sqrt(generator.nextDouble());
        double theta = 2 * Math.PI * generator.nextDouble();

        EncodedLatLon noisyLocation = new EncodedLatLon((float)((l.getLatitude() + r * Math.sin(theta) / METERS_PER_LATITUDE)),
                (float)(l.getLongitude() + r * Math.cos(theta) / (METERS_PER_LATITUDE * Math.cos(l.getLatitude() * Math.PI / 180.0f))));

        return noisyLocation;
    }
}