package com.bbn.coffeebreak;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MeetingList {

    private HashMap<String, Meeting> meetings;
    private List<Meeting> meeting_array_list;
    private String TAG = "[MeetingList]";

    MeetingList(){
        meetings = new HashMap<>(5);
        meeting_array_list = new ArrayList<Meeting>();
    }

    public void insertMeeting(JSONObject details){
        try {
            Meeting m = new Meeting(details);
            meetings.put(m.meetingID, m);
            meeting_array_list.add(m);
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

    Meeting getMeeting(String meetingId){
        return meetings.get(meetingId);
    }

    public List<Meeting> getMeetingArrayList() {
        return meeting_array_list;
    }

    public void removeMeeting(String meetingId){
        Meeting m = meetings.remove(meetingId);
        meeting_array_list.remove(m);
    }

    protected class Meeting{

        protected String meetingID;
        protected int size;
        protected int current;
        protected String organizer;
        protected JSONArray attendees;
        protected List<String> pending_invites;
        protected JSONObject details;

        Meeting(JSONObject d) throws JSONException {
            meetingID = d.getString("meetingID");
            size = d.getJSONArray("attendees").length();
            current = 1;
            organizer = d.getString("organizer");
            attendees = d.getJSONArray("attendees");
            details = d;

            initPendingInvites();
        }

        void initPendingInvites() throws JSONException {
            pending_invites = new ArrayList<String>();

            for (int i = 0; i < attendees.length(); i++) {
                String member = attendees.getString(i);

                if (!member.equals(organizer)) {
                    pending_invites.add(member);
                }
            }
        }

        void removePendingInvite(String member) {
            pending_invites.remove(member);
        }
    }
}
