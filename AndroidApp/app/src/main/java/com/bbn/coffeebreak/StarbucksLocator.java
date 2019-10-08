package com.bbn.coffeebreak;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class StarbucksLocator implements Runnable{

    private static final String TAG = "[StarbucksLocator]";
    float mLat;
    float mLon;
    private ResultReceiver mReceiver;
    private Bundle mBundle;

    private final String MAPBOX_BASE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/starbucks.json?";
    private final String MAPBOX_RESULTS_LIMIT = "&limit=1";
    private final String MAPBOX_PROXIMITY = "&proximity=";
    private final String API_KEY = "&access_token=YOUR_MAPBOX_ACCESS_TOKEN_GOES_HERE";
    private final String HTTP_REQUEST_TYPE = "GET";


    public StarbucksLocator( float lat, float lon, Bundle extras, ResultReceiver r){
        mLat = lat;
        mLon = lon;
        mReceiver = r;
        mBundle = extras;
    }

    @Override
    public void run() {
        String request = MAPBOX_BASE_URL + MAPBOX_RESULTS_LIMIT + MAPBOX_PROXIMITY +
                String.valueOf(mLon) + "," + String.valueOf(mLat) +
                API_KEY;

        try {
            URL url = new URL(request);
            Log.d(TAG,"Mapbox request sent: " + url.toString());
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setRequestMethod(HTTP_REQUEST_TYPE);
            httpCon.getInputStream();
            int responseCode = httpCon.getResponseCode();
            Log.d(TAG, "Response code: " + String.valueOf(responseCode));
            BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            httpCon.getInputStream().close();
            Log.d(TAG, "Response: " + response.toString());

            // Build result to send back

            mBundle.putString("location", response.toString());
            mBundle.putInt("responseCode", responseCode);
            mReceiver.send(1, mBundle);

        } catch (IOException e) {
            e.printStackTrace();
            mReceiver.send(0, null);
        }
    }
}
