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


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoscg.dialogsheet.DialogSheet;
import com.onegravity.contactpicker.contact.Contact;
import com.onegravity.contactpicker.contact.ContactDescription;
import com.onegravity.contactpicker.contact.ContactSortOrder;
import com.onegravity.contactpicker.core.ContactPickerActivity;
import com.onegravity.contactpicker.picture.ContactPictureType;

import org.json.JSONException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;


public class MainActivity extends AppCompatActivity {


    private final static String TAG = "[MainActivity]";

    private LocalBroadcastManager mLocalBroadcastManager;

    private static Random generator;

    // For sending HTTP Requests
    private HandlerThread httpHandlerThread;

    private Map<String, String> phoneNumberMap;

    private CountDownTimer progress_timer;

    // Broadcast receiver for handling events
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(final Context context, final Intent intent) {


            if (intent.getAction().equals(getString(R.string.broadcast_show_meeting_request))) {
                // Receives broadcast to show a meeting dialog request, checks if user in another meeting, if not shows dialog

                Log.d(TAG, "Got broadcast to show meeting dialog");

                final Intent sendMeetingResponse = new Intent();
                sendMeetingResponse.putExtra("meetingID", intent.getStringExtra("meetingID"));
                sendMeetingResponse.putExtra("organizer", intent.getStringExtra("organizer"));
                sendMeetingResponse.putStringArrayListExtra("attendees", intent.getStringArrayListExtra("attendees"));
                sendMeetingResponse.putExtra("begin", intent.getStringExtra("begin"));
                sendMeetingResponse.putExtra("end", intent.getStringExtra("end"));
                sendMeetingResponse.setAction(getString(R.string.broadcast_send_meeting_response));
                final String organizer = intent.getStringExtra("organizer");

                List<String> attendees = intent.getStringArrayListExtra("attendees");
                String message = "Invites: ";
                for (String s : attendees) {
                    if (!s.equals(organizer)) {
                        if (phoneNumberMap.get(s) != null) {
                            message += phoneNumberMap.get(s) + ", ";
                        } else {
                            message += s + ", ";
                        }
                    }
                }
                message = message.substring(0, message.length() - 2);

                message += "\n\nWhere: Private Starbucks location";
                message += "\nWhen: Now";

                final ObjectMapper mapper = new ObjectMapper();
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);

                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);

                if (!intent.getBooleanExtra("location", true)) {
                    // Location not found, happens if location services are off and no mock location is set, cancel meeting
                    Intent setLocation = new Intent();
                    setLocation.putExtra("meetingID", intent.getStringExtra("meetingID"));
                    setLocation.putExtra("organizer", intent.getStringExtra("organizer"));
                    setLocation.putStringArrayListExtra("attendees", intent.getStringArrayListExtra("attendees"));
                    setLocation.setAction(getString(R.string.broadcast_show_location_dialog));
                    mLocalBroadcastManager.sendBroadcast(setLocation);

                    cancelMeeting(intent.getStringExtra("meetingID"), 2);
                    return;
                } else if (MeetingRequestDialog.dialogExists() && preferences.getString("status", "").equals("Meeting " +
                        intent.getStringExtra("meetingID") + " waiting for response...")) {
                    // Invite already showing for this meeting
                } else if (MeetingRequestDialog.dialogExists()) {
                    // Phone currently displaying an invite for another meeting
                    Log.d(TAG, "meeting overlap 1");

                    cancelMeeting(intent.getStringExtra("meetingID"), 1);
                    return;
                } else if (mpcMessage.getVisibility() == View.VISIBLE) {
                    // Phone currently waiting on pending invites or waiting on mpc calculation for another meeting
                    Log.d(TAG, "meeting overlap 2");

                    cancelMeeting(intent.getStringExtra("meetingID"), 1);
                    return;
                }

                mpcMessage.setText("Meeting " + intent.getStringExtra("meetingID") + " waiting for response...");

                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("status", mpcMessage.getText().toString());
                editor.commit();

                String title = "";
                if (phoneNumberMap.get(organizer) != null) {
                    title = phoneNumberMap.get(organizer) + " wants to meet";
                } else {
                    title = organizer + " wants to meet";
                }

                // Display meeting request dialog
                MeetingRequestDialog.request(MainActivity.this, message,
                        new DialogSheet.OnPositiveClickListener() {
                            @Override
                            public void onClick(View view) {
                                InviteResponse response = new InviteResponse();
                                response.setMeetingID(intent.getStringExtra("meetingID"));
                                response.setAdditionalProperty("attendees", intent.getStringArrayListExtra("attendees"));
                                response.setAdditionalProperty("organizer", intent.getStringExtra("organizer"));
                                Log.d(TAG, "Sending meeting response for meetingID: " + intent.getStringExtra("meetingID"));
                                response.setResponse(1);
                                try {
                                    sendMeetingResponse.putExtra("response", mapper.writeValueAsString(response));
                                    mLocalBroadcastManager.sendBroadcast(sendMeetingResponse);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new DialogSheet.OnNegativeClickListener() {
                            @Override
                            public void onClick(View view) {
                                InviteResponse response = new InviteResponse();
                                response.setMeetingID(intent.getStringExtra("meetingID"));
                                response.setAdditionalProperty("attendees", intent.getStringArrayListExtra("attendees"));
                                response.setAdditionalProperty("organizer", intent.getStringExtra("organizer"));
                                response.setResponse(0);
                                try {
                                    sendMeetingResponse.putExtra("response", mapper.writeValueAsString(response));
                                    mLocalBroadcastManager.sendBroadcast(sendMeetingResponse);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                MeetingRequestDialog.reset();
                            }
                        }).setTitle(title).show();
            } else if (intent.getAction().equals(getString(R.string.broadcast_show_meeting_location))) {
                // Received broadcast to show meeting location dialog or error dialog, resets homescreen

                // Updating visibility of UI widgets
                ProgressBar mpcProgress = (ProgressBar) findViewById(R.id.progressbar_mpc);
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);

                mpcMessage.setText("Start");
                mpcProgress.setVisibility(View.INVISIBLE);
                mpcMessage.setVisibility(View.INVISIBLE);

                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("status", mpcMessage.getText().toString());
                editor.commit();

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setEnabled(true);
                fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

                if(intent.getStringExtra("address").equals("ERROR")){
                    // Error in mpc calculation

                    AlertDialog errorDialog = new AlertDialog.Builder(MainActivity.this).create();
                    errorDialog.setTitle("Error performing MPC");
                    errorDialog.setMessage("Was unable to determine meeting location");
                    errorDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    return;
                                }
                            });
                    errorDialog.show();
                }else{
                    // Meeting location address was found, dialog with address is displayed

                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setCancelable(false);
                    alertDialog.setTitle("Secure Meeting Location");
                    alertDialog.setMessage(intent.getStringExtra("address"));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Show on map",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent showMap = new Intent(MainActivity.this, MapActivity.class);
                                    showMap.putExtra("address", intent.getStringExtra("address"));
                                    showMap.putExtra("latitude", intent.getFloatExtra("latitude", 0.0f));
                                    showMap.putExtra("longitude", intent.getFloatExtra("longitude", 0.0f));

                                    startActivity(showMap);
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Dismiss",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    return;
                                }
                            });
                    alertDialog.show();
                }

            } else if (intent.getAction().equals(getString(R.string.broadcast_show_mpc_progress))) {
                // Received broadcast to show MPC progress, updates homescreen

                Log.d(TAG, "Received broadcast to show MPC progress");

                // Updating visibility of UI widgets
                ProgressBar mpcProgress = (ProgressBar) findViewById(R.id.progressbar_mpc);
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
                Button cancelButton = (Button) findViewById(R.id.cancel_meeting_button);
                ProgressBar timeoutProgress = (ProgressBar) findViewById(R.id.timeout_progress);

                mpcMessage.setText("Performing secure multi-party computation");
                mpcProgress.setVisibility(View.VISIBLE);
                mpcMessage.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
                cancelButton.setEnabled(false);
                timeoutProgress.setVisibility(View.INVISIBLE);

                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("status", mpcMessage.getText().toString());
                editor.commit();

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setEnabled(false);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));

            } else if (intent.getAction().equals(getString(R.string.broadcast_show_location_dialog))) {
                // Received broadcast to show location dialog, user location not found

                MeetingRequestDialog.reset();

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Can't get GPS location");
                alertDialog.setMessage("Go outside to get a GPS location or set a mock location");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Set mock location", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent noisyMapActivity = new Intent(MainActivity.this, NoisyLocationMapActivity.class);
                        startActivity(noisyMapActivity);
                    }
                });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
            } else if(intent.getAction().equals(getString(R.string.broadcast_show_meeting_cancel))){
                // Received broadcast that meeting was cancelled, resets home screens and displays cancel dialog

                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
                Button cancelButton = (Button) findViewById(R.id.cancel_meeting_button);
                ProgressBar mpcProgress = (ProgressBar) findViewById(R.id.progressbar_mpc);
                ProgressBar timeoutProgress = (ProgressBar) findViewById(R.id.timeout_progress);

                String meetingID = intent.getStringExtra("meetingID");
                String organizer = intent.getStringExtra("organizer");
                String username = intent.getStringExtra("username");
                String user_cancelled = intent.getStringExtra("user_cancelled");
                String message = "\n" + intent.getStringExtra("message");

                if (user_cancelled != null) {
                    if (phoneNumberMap.get(user_cancelled) != null) {
                        message = "\n" + phoneNumberMap.get(user_cancelled) + intent.getStringExtra("message");
                    } else {
                        message = "\n" + user_cancelled + intent.getStringExtra("message");
                    }
                }

                Log.d(TAG, "message: " + intent.getStringExtra("message"));

                // Resets home screen if phone is not in another meeting
                if (!message.contains("Meeting invite not sent") || (mpcMessage.getText().toString()).contains(meetingID)) {
                    mpcMessage.setVisibility(View.INVISIBLE);
                    cancelButton.setVisibility(View.INVISIBLE);
                    cancelButton.setEnabled(false);
                    mpcProgress.setVisibility(View.INVISIBLE);
                    timeoutProgress.setVisibility(View.INVISIBLE);

                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    fab.setEnabled(true);
                    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                }

                // Displays meeting cancelled dialog, only sends cancel dialog if there isn't already one for the meeting
                if (!message.contains("Meeting invite not sent") || username.equals(organizer) && !MeetingCancelDialog.dialogExists()) {
                    // Meeting was cancelled or timed out, or the organizer receives a cancel message
                    MeetingCancelDialog.request(MainActivity.this, message,
                            new DialogSheet.OnPositiveClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // do nothing
                                    MeetingCancelDialog.reset();
                                }
                            }).setTitle("Meeting Cancelled").show();
                } else if (!MeetingCancelDialog.dialogExists()) {
                    // Error with meeting invite received by invitee
                    MeetingCancelDialog.request(MainActivity.this, "Not all invitees available",
                            new DialogSheet.OnPositiveClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // do nothing
                                    MeetingCancelDialog.reset();
                                }
                            }).setTitle("Meeting Cancelled").show();
                }

                // Removes old meeting request dialogs unless they are from a different ongoing meeting
                if (!message.contains("Meeting invite not sent") || (mpcMessage.getText().toString()).contains(meetingID)) {
                    Log.d(TAG, "dismissing dialogs - meetingID: " + intent.getStringExtra("meetingID"));
                    MeetingRequestDialog.reset();
                    mpcMessage.setText("Start");

                    SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putString("status", mpcMessage.getText().toString());
                    editor.commit();
                }
            } else if (intent.getAction().equals(getString(R.string.broadcast_show_meeting_pending))) {
                // Receives broadcast to show meeting pending screen, including the cancel button

                Log.d(TAG, "Received broadcast to show pending meeting");

                MeetingList meetingList = AMQPCommunication.getMeetingList();
                String meetingID = intent.getStringExtra("meetingID");
                MeetingList.Meeting meeting = meetingList.getMeeting(meetingID);

                // Constructing the message with the names of the pending users
                String message = "Meeting: " + meetingID + "\nWaiting on ";

                String pen = meeting.pending_invites.toString();
                pen = pen.substring(1, pen.length() - 1);
                String[] invitees = pen.split(", ");

                for (String i : invitees) {
                    if (phoneNumberMap.get(i) != null) {
                        message += phoneNumberMap.get(i) + ", ";
                    } else {
                        message += i + ", ";
                    }
                }
                message = message.substring(0, message.length() - 2);

                // Updating visibility of UI widgets
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
                mpcMessage.setText(message);
                mpcMessage.setVisibility(View.VISIBLE);

                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();

                editor.putString("status", mpcMessage.getText().toString());
                editor.commit();

                Button cancelButton = (Button) findViewById(R.id.cancel_meeting_button);
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setEnabled(true);

                ProgressBar timeoutProgress = (ProgressBar) findViewById(R.id.timeout_progress);
                timeoutProgress.setVisibility(View.VISIBLE);
                timeoutProgress.setMin(0);

                int timeout = intent.getIntExtra("timeout", 60) / 1000;
                int timeLeft = intent.getIntExtra("timeLeft", 60) / 1000;
                timeoutProgress.setMax(timeout);
                timeoutProgress.setProgress(timeout - timeLeft);

                if (progress_timer != null) {
                    progress_timer.cancel();
                }

                // Increments invite timeout progress bar
                progress_timer = new CountDownTimer(timeout * 1000, 1000) {
                    @Override
                    public void onTick(long l) {
                        timeoutProgress.incrementProgressBy(1);
                    }

                    @Override
                    public void onFinish() { }
                }.start();

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                fab.setEnabled(false);
                fab.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));

                // Cancel button OnClick listener
                Objects.requireNonNull(cancelButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "cancelling pending meeting");

                        cancelMeeting(meetingID, 0);
                    }
                });
            } else if (intent.getAction().equals(getString(R.string.broadcast_update_meeting_pending))) {
                // Received broadcast to update the meeting pending screen - new users accepted the invite

                Log.d(TAG, "Received broadcast to update pending meeting invites");

                MeetingList meetingList = AMQPCommunication.getMeetingList();
                String meetingID = intent.getStringExtra("meetingID");
                MeetingList.Meeting meeting = meetingList.getMeeting(meetingID);

                // Constructing the message with the names of the pending users
                String message = "Meeting: " + meetingID + "\nWaiting on ";

                String pen = meeting.pending_invites.toString();
                pen = pen.substring(1, pen.length() - 1);
                String[] invitees = pen.split(", ");

                for (String i : invitees) {
                    if (phoneNumberMap.get(i) != null) {
                        message += phoneNumberMap.get(i) + ", ";
                    } else {
                        message += i + ", ";
                    }
                }
                message = message.substring(0, message.length() - 2);

                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);

                // If the user has not responded yet, the message does not get displayed
                if (!mpcMessage.getText().toString().contains("waiting for response...")) {
                    mpcMessage.setText(message);

                    SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putString("status", mpcMessage.getText().toString());
                    editor.commit();
                }
            }
        }
    };

    private void cancelMeeting(String meetingID, int code) {
        // Cancelling the pending meeting

        Log.d(TAG, "cancelling pending meeting");

        MeetingList meetingList = AMQPCommunication.getMeetingList();
        MeetingList.Meeting meeting = meetingList.getMeeting(meetingID);

        if (meeting == null) {
            // Meeting already cancelled
            Log.d(TAG, "meeting is null");
            return;
        }

        // Reset home screen unless user is in another meeting
        if (code != 1) {
            // Updating visibility of UI widgets
            TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
            Button cancelButton = (Button) findViewById(R.id.cancel_meeting_button);
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            ProgressBar timeoutProgress = (ProgressBar) findViewById(R.id.timeout_progress);

            mpcMessage.setText("Start");
            mpcMessage.setVisibility(View.INVISIBLE);

            cancelButton.setVisibility(View.INVISIBLE);
            cancelButton.setEnabled(false);

            timeoutProgress.setVisibility(View.INVISIBLE);

            fab.setEnabled(true);
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));

            SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            editor.putString("status", mpcMessage.getText().toString());
            editor.commit();
        }

        final Intent sendMeetingResponse = new Intent();
        sendMeetingResponse.putExtra("meetingID", meetingID);
        sendMeetingResponse.putExtra("organizer", meeting.organizer);

        ArrayList<String> attendees = new ArrayList<String>();
        for(int i = 0; i < meeting.attendees.length(); i++){
            try {
                String attendee = meeting.attendees.get(i).toString();
                attendees.add(attendee);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        sendMeetingResponse.putStringArrayListExtra("attendees", attendees);
        sendMeetingResponse.setAction(getString(R.string.broadcast_send_meeting_response));

        final ObjectMapper mapper = new ObjectMapper();

        InviteResponse response = new InviteResponse();
        response.setMeetingID(meetingID);
        response.setAdditionalProperty("attendees", attendees);
        response.setAdditionalProperty("organizer", meeting.organizer);

        if (code == 0) {
            response.setResponse(0);
        } else if (code == 1) {
            response.setResponse(3);
            response.setAdditionalProperty("error", "invitees are in another meeting");
        } else {
            response.setResponse(3);
            response.setAdditionalProperty("error", "cannot get location for all invitees");
        }

        try {
            sendMeetingResponse.putExtra("response", mapper.writeValueAsString(response));
            mLocalBroadcastManager.sendBroadcast(sendMeetingResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // Gets list of contact names with phone numbers and puts them into phoneNumberMap
    private void getContactList() {
        phoneNumberMap = new HashMap<String,String>();

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);

                    while (pCur.moveToNext()) {
                        String phoneNum = PhoneNumberUtils.formatNumberToE164(pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER)), "US");

                        // Returns 0 if default, 1 if not default
                        Log.d(TAG, "IS_SUPER_PRIMARY: " + pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)));

                        Log.i(TAG, "Name: " + name);
                        Log.i(TAG, "Phone Number: " + phoneNum);

                        phoneNumberMap.put(phoneNum, name);
                    }
                    pCur.close();
                }
            }
        }

        if(cur!=null){
            cur.close();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        httpHandlerThread = new HandlerThread("httpHandlerThread");
        httpHandlerThread.start();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("current_screen", "activity_main");

        Log.d(TAG, "preferences: " + preferences.getAll());

        TextView usernameDisplay = (TextView) findViewById(R.id.usernameDisplay);
        usernameDisplay.setText(preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)).toUpperCase());

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_request));
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_location));
        mIntentFilter.addAction(getString(R.string.broadcast_show_mpc_progress));
        mIntentFilter.addAction(getString(R.string.broadcast_show_location_dialog));
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_cancel));
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_pending));
        mIntentFilter.addAction(getString(R.string.broadcast_update_meeting_pending));

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        // Random number generator
        generator = new Random();

        // Fills phoneNumberMap with the contacts
        getContactList();

        // Displays whether or not the mock location is on
        TextView location_type = (TextView) findViewById(R.id.location_type);
        if (preferences.getBoolean(getString(R.string.mock_location), true)) {
            location_type.setText("Mock Location On");
            location_type.setTextColor(Color.RED);
        } else {
            location_type.setText("Mock Location Off");
            location_type.setTextColor(Color.DKGRAY);
        }

        // ContactPicker Button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setEnabled(true);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
        Objects.requireNonNull(fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Note: The value of this ContactPicker is returned in the function onActivityResult
                // Old contact picker - ContactPicker.startContactPicker(MainActivity.this);
                // https://github.com/1gravity/Android-ContactPicker

                Intent intent = new Intent(MainActivity.this, ContactPickerActivity.class)
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_BADGE_TYPE, ContactPictureType.ROUND.name())
                        .putExtra(ContactPickerActivity.EXTRA_SHOW_CHECK_ALL, false)
                        .putExtra(ContactPickerActivity.EXTRA_ONLY_CONTACTS_WITH_PHONE, true)
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION, ContactDescription.PHONE.name())
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_DESCRIPTION_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)
                        .putExtra(ContactPickerActivity.EXTRA_SELECT_CONTACTS_LIMIT, 9)
                        .putExtra(ContactPickerActivity.EXTRA_LIMIT_REACHED_MESSAGE, "Select up to 9 contacts")
                        .putExtra(ContactPickerActivity.EXTRA_CONTACT_SORT_ORDER, ContactSortOrder.AUTOMATIC.name());
                startActivityForResult(intent, getResources().getInteger(R.integer.contact_picker_request));
            }
        });

        if(getIntent().getStringExtra("meetingID") != null){
            mLocalBroadcastManager.sendBroadcast(getIntent());
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "in onResume");

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("current_screen", "activity_main");
        editor.commit();

        TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
        mpcMessage.setText(preferences.getString("status", ""));
        mpcMessage.setVisibility(View.INVISIBLE);

        // Displays whether or not the mock location is on
        TextView location_type = (TextView) findViewById(R.id.location_type);
        if (preferences.getBoolean(getString(R.string.mock_location), true)) {
            location_type.setText("Mock Location On");
            location_type.setTextColor(Color.RED);
        } else {
            location_type.setText("Mock Location Off");
            location_type.setTextColor(Color.DKGRAY);
        }

        ((TextView)(findViewById(R.id.noiseDisplay))).setText("Noise level: " + String.valueOf(preferences.getInt(getString(R.string.noise_value), 5)) + "km");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Eventually we may have multiple results returning to this Activity
        if (requestCode == getResources().getInteger(R.integer.contact_picker_request)) {
            // Received results from ContactPicker

            if (resultCode == RESULT_OK) {
                final SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);

                // Process contacts
                List<Contact> contacts = (List<Contact>) data.getSerializableExtra(ContactPickerActivity.RESULT_CONTACT_DATA);
                final String[] contactNames = new String[contacts.size()];
                final String[] contactPhones = new String[contacts.size()];

                if (contacts.size() == 0) {
                    return;
                } else if (contacts.size() == 1) {
                    String phoneNum = PhoneNumberUtils.formatNumberToE164((contacts.get(0).getPhone(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)), "US");
                    if (phoneNum.equals(preferences.getString("username",""))) {
                        return;
                    }
                }

                int j = 0;
                for (Contact contact : contacts) {
                    contactNames[j] = contact.getDisplayName().toLowerCase();

                    if (contact.getPhone(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN) == null) {
                        Toast.makeText(MainActivity.this, "No phone number associated with contact", Toast.LENGTH_LONG).show();
                        return;
                    }
                    contactPhones[j] = PhoneNumberUtils.formatNumberToE164((contact.getPhone(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN)), "US");
                    if (contactPhones[j] == null) {
                        Toast.makeText(MainActivity.this, "Contact has invalid US phone number", Toast.LENGTH_LONG).show();
                        return;
                    }
                    j++;
                }

                LocalDateTime beginDate = LocalDateTime.now();
                ZonedDateTime zBeginDate = beginDate.atZone(ZoneId.systemDefault());

                LocalDateTime endDate = beginDate.plusDays(7);
                ZonedDateTime zEndDate = endDate.atZone(ZoneId.systemDefault());

                if (zEndDate.isBefore(zBeginDate)) {
                    Log.d(TAG, "Invalid date interval... exiting");
                    Toast.makeText(MainActivity.this, "Invalid date interval", Toast.LENGTH_LONG).show();
                    return;
                }

                ZonedDateTime utcBegin = zBeginDate.withZoneSameInstant(ZoneId.of("UTC"));
                ZonedDateTime utcEnd = zEndDate.withZoneSameInstant(ZoneId.of("UTC"));

                String beginTime = utcBegin.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + utcBegin.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";
                String endTime = utcEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "T" + utcEnd.format(DateTimeFormatter.ofPattern("HHmmss")) + "Z";

                Log.d(TAG, "Invite Start Time Zulu: " + beginTime);
                Log.d(TAG, "Invite End Time Zulu: " + endTime);

                // Build meeting invite to send to scheduling server
                Invite i = new Invite();
                i.setMeetingID(String.valueOf(generator.nextInt() & Integer.MAX_VALUE));
                i.setTitle("Meeting");
                // set the organizer as the user of the device
                i.setOrganizer(preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)));
                i.setLocation("10 Moulton Street");
                i.setDuration("PT1H");
                i.setGranularity("PT15M");
                Set<String> attendees = new HashSet<>(2);

                for (String phoneNumber : contactPhones){
                    Log.d(TAG, "Adding " + phoneNumber + " to attendee list");
                    attendees.add(phoneNumber);
                }

                attendees.add(PhoneNumberUtils.formatNumberToE164(preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)), "US"));
                i.setAttendees(attendees);
                Constraints c = new Constraints();


                c.setBegin(beginTime);
                c.setEnd(endTime);
                i.setConstraints(c);

                // Used for writing the JSON Object as a string
                ObjectMapper mapper = new ObjectMapper();

                try {
                    // Send off meeting invite
                    Log.d("Sending invite: ", mapper.writeValueAsString(i));
                    Intent sendMeetingInvite = new Intent();
                    sendMeetingInvite.putExtra("event", mapper.writeValueAsString(i));
                    sendMeetingInvite.setAction(getString(R.string.broadcast_send_meeting_invite));
                    mLocalBroadcastManager.sendBroadcast(sendMeetingInvite);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, getString(R.string.message_date_picker_cancel));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Inflate the menu; this adds items to the action bar if it is present.
        switch (item.getItemId()) {
            case R.id.server_settings:
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("settingsFragment");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                DialogFragment dialogFragment = new SettingsFragment();

                dialogFragment.show(ft, "settingsFragment");
                return true;
            case R.id.noise_settings:
                Intent noisyMapActivity = new Intent(MainActivity.this, NoisyLocationMapActivity.class);
                startActivity(noisyMapActivity);
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        httpHandlerThread.quitSafely();
    }
}
