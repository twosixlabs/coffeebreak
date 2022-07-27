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
        // Returns true if all participants are accounted for
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
