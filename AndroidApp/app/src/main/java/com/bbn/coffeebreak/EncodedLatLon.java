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
