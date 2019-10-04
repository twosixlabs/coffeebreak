package com.bbn.coffeebreak;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MeetingList {

    private HashMap<String, Meeting> meetings;
    private String TAG = "[MeetingList]";

    MeetingList(){
        meetings = new HashMap<>(5);
    }

    public void insertMeeting(JSONObject details){
        try {
            Meeting m = new Meeting(details);
            meetings.put(m.meetingID, m);
            Log.d(TAG, "Inserting meeting with meeting ID: " + m.meetingID);
            if(meetings.get(m.meetingID) == null){
                Log.d(TAG, "Failed to insert meeting ID: " + m.meetingID);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean incrementMeetingParticipation(String meetingId){
        /*
        Returns true if all participants are accounted for
         */
        if(meetings.get(meetingId) != null){
            return (++meetings.get(meetingId).current == meetings.get(meetingId).size);
        }else{
            Log.d(TAG, "Received NULL response from meeting ID lookup: " + meetingId);
        }
        return false;
    }

    JSONObject getMeetingDetails(String meetingId){
        return meetings.get(meetingId).details;
    }

    public void removeMeeting(String meetingId){
        meetings.remove(meetingId);
    }

    protected class Meeting{

        protected String meetingID;
        protected int size;
        protected int current;
        protected JSONObject details;

        Meeting(JSONObject d) throws JSONException {
            meetingID = d.getString("meetingID");
            size = d.getJSONArray("attendees").length();
            current = 1;
            details = d;
        }
    }
}
