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
This class is used for representing longitude and latitudes as a unique, normalized bit string,
that provides the additive property needed for secure MPC
 */


public class EncodedLatLon {

    private static final float MULTIPLIER = 10000.0f;
    private static final float SHIFT = 180.0f;
    private float mLatitude;
    private float mLongitude;
    private long encodedLocation;

    public EncodedLatLon(float lat, float lon){
        mLatitude = lat + SHIFT;
        mLongitude = lon + SHIFT;
        encodedLocation = ((long) ((int)(mLatitude * MULTIPLIER)) << 32) + ((int)(mLongitude * MULTIPLIER));
    }

    // Takes the additive result from the MPC calculation and converts it into a floating point number
    public static EncodedLatLon convertMpcResultToEncodedLatLon(long result, int num_parties){

        float latitude = ((result >> 32) / MULTIPLIER) - SHIFT * num_parties;
        float longitude = (((int)result) / MULTIPLIER) - SHIFT * num_parties;

        EncodedLatLon location = new EncodedLatLon(latitude, longitude);

        return location;
    }

    public long getEncodedLocation(){
        return encodedLocation;
    }

    public float getLatitude(){
        return mLatitude - SHIFT;
    }

    public float getLongitude(){
        return mLongitude - SHIFT;
    }
}
