/*
 * Copyright 2021 Raytheon BBN Technologies Corp.
 * Copyright 2021 Two Six Technologies
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

import android.content.Context;
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
    private Context mContext;
    float mLat;
    float mLon;
    private ResultReceiver mReceiver;
    private Bundle mBundle;

    private final String MAPBOX_BASE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/starbucks.json?";
    private final String MAPBOX_RESULTS_LIMIT = "&limit=1";
    private final String MAPBOX_PROXIMITY = "&proximity=";
    private final String API_KEY = "&access_token=";
    private final String HTTP_REQUEST_TYPE = "GET";


    public StarbucksLocator( Context context, float lat, float lon, Bundle extras, ResultReceiver r){
        mContext = context ;
        mLat = lat;
        mLon = lon;
        mReceiver = r;
        mBundle = extras;
    }

    @Override
    public void run() {
        String request = MAPBOX_BASE_URL + MAPBOX_RESULTS_LIMIT + MAPBOX_PROXIMITY +
                String.valueOf(mLon) + "," + String.valueOf(mLat) +
                API_KEY + mContext.getString(R.string.mapbox_access_token);

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
