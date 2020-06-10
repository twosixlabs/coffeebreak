package com.bbn.coffeebreak;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.RequiresApi;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoscg.dialogsheet.DialogSheet;
import com.wafflecopter.multicontactpicker.ContactResult;
import com.wafflecopter.multicontactpicker.MultiContactPicker;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;


public class MainActivity extends AppCompatActivity {


    private final static String TAG = "[MainActivity]";

    private LocalBroadcastManager mLocalBroadcastManager;

    private static Random generator;

    // For sending HTTP Requests
    private HandlerThread httpHandlerThread;
    private Handler mHandler;

    // Broadcast receiver for handling events
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(final Context context, final Intent intent) {


            if (intent.getAction().equals(getString(R.string.broadcast_show_meeting_request))) {

                /*
                For Showing a Meeting Request Dialog to a user
                 */

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
                    if (!s.equals(organizer))
                        message += s + "; ";
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
                LocalDateTime startTime = LocalDateTime.parse(intent.getStringExtra("begin"), formatter)
                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();
                LocalDateTime endTime = LocalDateTime.parse(intent.getStringExtra("end"), formatter)
                        .atZone(ZoneId.of("America/New_York")).toLocalDateTime();

                String startDate = startTime.getMonth().toString() + " " + startTime.getDayOfMonth();
                String endDate = endTime.getMonth().toString() + " " + endTime.getDayOfMonth();

                //message += "\n\nWhen: between " + startDate + " and " + endDate;
                message += "\n\nWhere: Private Starbucks location";
                message += "\nWhen: Now";

                final ObjectMapper mapper = new ObjectMapper();

                MeetingRequestDialog.request(MainActivity.this, message,
                        new DialogSheet.OnPositiveClickListener() {
                            @Override
                            public void onClick(View view) {
                                //sendBroadcast(startMpc);
                                InviteResponse response = new InviteResponse();
                                response.setMeetingID(intent.getStringExtra("meetingID"));
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
                                response.setResponse(0);
                                try {
                                    sendMeetingResponse.putExtra("response", mapper.writeValueAsString(response));
                                    mLocalBroadcastManager.sendBroadcast(sendMeetingResponse);
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).setTitle(organizer + " wants to meet").show();

            } else if (intent.getAction().equals(getString(R.string.broadcast_show_meeting_location))) {
                ProgressBar mpcProgress = (ProgressBar) findViewById(R.id.progressbar_mpc);
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
                mpcProgress.setVisibility(View.INVISIBLE);
                mpcMessage.setVisibility(View.INVISIBLE);
                if(intent.getStringExtra("address").equals("ERROR")){
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
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
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
                    alertDialog.show();
                }

            } else if (intent.getAction().equals(getString(R.string.broadcast_show_mpc_progress))) {
                Log.d(TAG, "Received broadcast to show MPC progress");
                ProgressBar mpcProgress = (ProgressBar) findViewById(R.id.progressbar_mpc);
                TextView mpcMessage = (TextView) findViewById(R.id.mpc_message);
                mpcProgress.setVisibility(View.VISIBLE);
                mpcMessage.setVisibility(View.VISIBLE);
            } else if (intent.getAction().equals(getString(R.string.broadcast_show_location_dialog))) {
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
                String meetingID = intent.getStringExtra("meetingID");
                String invitee = intent.getStringExtra("invitee");
                String message = "\n" + invitee + " has rejected the meeting invitation";
                MeetingCancelDialog.request(MainActivity.this, message,
                        new DialogSheet.OnPositiveClickListener() {
                            @Override
                            public void onClick(View view) {
                                // do nothing
                            }
                        }).setTitle(invitee + " cancelled the meeting").show();
            }
        }
    };


    //Suppressing PRIVATEDATA_SERIVCE warnings
    @SuppressLint({"ResourceType", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        httpHandlerThread = new HandlerThread("httpHandlerThread");
        httpHandlerThread.start();
        mHandler = new Handler(httpHandlerThread.getLooper());

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);

        TextView usernameDisplay = (TextView) findViewById(R.id.usernameDisplay);
        usernameDisplay.setText(preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)).toUpperCase());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_request));
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_location));
        mIntentFilter.addAction(getString(R.string.broadcast_show_mpc_progress));
        mIntentFilter.addAction(getString(R.string.broadcast_show_location_dialog));
        mIntentFilter.addAction(getString(R.string.broadcast_show_meeting_cancel));

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);


        // Random number generator
        generator = new Random();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Objects.requireNonNull(fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Note: The value of this ContactPicker is returned in the function onActivityResult
                ContactPicker.startContactPicker(MainActivity.this);
            }
        });

        FloatingActionButton pending_fab = (FloatingActionButton) findViewById(R.id.pending_fab);
        Objects.requireNonNull(pending_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent pendingActivity = new Intent(MainActivity.this, PendingActivity.class);
                startActivityForResult(pendingActivity, getResources().getInteger(R.integer.pending_request));
            }
        });

        if(getIntent().getStringExtra("meetingID") != null){
            mLocalBroadcastManager.sendBroadcast(getIntent());
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        ((TextView)(findViewById(R.id.noiseDisplay))).setText("Noise level: " + String.valueOf(preferences.getInt(getString(R.string.noise_value), 5)) + "km");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Eventually we may have multiple results returning to this Activity
        if (requestCode == getResources().getInteger(R.integer.contact_picker_request)) {

            if (resultCode == RESULT_OK) {

                final SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);

                List<ContactResult> results = MultiContactPicker.obtainResult(data);

                //Get all the names / emails of the picked contacts
                final String[] contactNames = new String[results.size()];
                //final String[] contactEmails = new String[results.size()];
                final String[] contactPhones = new String[results.size()];
                int j = 0;
                for (ContactResult c : results) {
                    contactNames[j] = c.getDisplayName().toLowerCase();
                    if (c.getPhoneNumbers().size() == 0) {
                        Toast.makeText(MainActivity.this, "No phone number associated with contact", Toast.LENGTH_LONG).show();
                        return;
                    }
                    contactPhones[j] = PhoneNumberUtils.formatNumberToE164(c.getPhoneNumbers().get(0), "US");
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
                Log.d(getString(R.string.app_tag), getString(R.string.message_date_picker_cancel));
            }
        } else if (requestCode == getResources().getInteger(R.integer.pending_request)) {
            if (data != null) {
                Log.d(getString(R.string.app_tag), "cancelling pending meeting");

                final Intent sendMeetingResponse = new Intent();
                sendMeetingResponse.putExtra("meetingID", data.getStringExtra("meetingID"));
                sendMeetingResponse.putExtra("organizer", data.getStringExtra("organizer"));
                sendMeetingResponse.putStringArrayListExtra("attendees", data.getStringArrayListExtra("attendees"));
                sendMeetingResponse.setAction(getString(R.string.broadcast_send_meeting_response));

                final ObjectMapper mapper = new ObjectMapper();

                InviteResponse response = new InviteResponse();
                response.setMeetingID(data.getStringExtra("meetingID"));
                response.setResponse(0);
                try {
                    sendMeetingResponse.putExtra("response", mapper.writeValueAsString(response));
                    mLocalBroadcastManager.sendBroadcast(sendMeetingResponse);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
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
