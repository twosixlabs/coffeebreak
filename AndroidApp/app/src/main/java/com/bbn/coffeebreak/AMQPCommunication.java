package com.bbn.coffeebreak;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class AMQPCommunication extends Service {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private final static String TAG = "[RabbitMQClient]";

    private static String AMQPHost = "127.0.0.1";
    private static int AMQPPort = 5672;

    private final static String INVITE_EXCHANGE = "invite";

    private LocalBroadcastManager mLocalBroadcastManager;
    private FusedLocationProviderClient client;
    private LocationRequest mLocationRequest;

    private static String invite_queue;

    private static String username = "";
    private static String password = "";

    private Context context;

    private int retryCount = 0;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    //private MeetingList meetingList;


    private ResultReceiver starbucksLocationReceiver = new ResultReceiver(new Handler(Looper.getMainLooper())){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if(resultCode == 1){
                try {
                    JSONObject httpResponse = new JSONObject(resultData.getString("location"));
                    JSONArray featuresArray = httpResponse.getJSONArray("features");
                    String address = featuresArray.getJSONObject(0).getString("place_name");
                    JSONArray location = featuresArray.getJSONObject(0).getJSONArray("center");
                    Log.d(TAG, "STARBUCKS ADDRESS: " + address);
                    EncodedLatLon starbucksLocation = new EncodedLatLon((float)location.getDouble(1), (float) location.getDouble(0));
                    Log.d(TAG, "STARBUCKS LOCATION: " + starbucksLocation.getLatitude() + ", " + starbucksLocation.getLongitude());

                    Intent showMeetingLocation = new Intent();
                    showMeetingLocation.putExtra("address", address);
                    showMeetingLocation.putExtra("latitude", starbucksLocation.getLatitude());
                    showMeetingLocation.putExtra("longitude", starbucksLocation.getLongitude());

                    showMeetingLocation.setAction(getString(R.string.broadcast_show_meeting_location));
                    mLocalBroadcastManager.sendBroadcast(showMeetingLocation);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                Log.d(TAG, "Got error in HTTP request for mapbox");
            }
        }
    };

    private ResultReceiver mpcResponse = new ResultReceiver(new Handler(Looper.getMainLooper())){
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);


            try{
                Log.d(TAG, "GOT MPC RESULT LOCATION: " + resultData.getFloat("latitude") + "," + resultData.getFloat("longitude"));

                //Perform Starbucks location reverse lookup
                mHandler.post(new StarbucksLocator(resultData.getFloat("latitude"),
                        resultData.getFloat("longitude"),
                        starbucksLocationReceiver));
            }catch(Exception e){
                e.printStackTrace();
            }

        }
    };

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            if (location != null) {
                Log.d(TAG, "Got location: " + location);
            }
        }
    };


    /*
    This ResultReceiver sends a local broadcast to show a meeting request dialog
     */
    private ResultReceiver showMeetingRequestDialog = new ResultReceiver(new Handler(Looper.getMainLooper())){
        @Override
        protected void onReceiveResult(final int resultCode, final Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);


            Intent notifIntent = new Intent(context, MainActivity.class);
            notifIntent.putExtra("meetingID", resultData.getString("meetingID"));
            notifIntent.putExtra("organizer", resultData.getString("organizer"));
            notifIntent.putStringArrayListExtra("attendees", resultData.getStringArrayList("attendees"));
            notifIntent.putExtra("begin", resultData.getString("begin"));
            notifIntent.putExtra("end", resultData.getString("end"));
            notifIntent.setAction(getString(R.string.broadcast_show_meeting_request));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);


            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Meeting Request")
                    .setContentText(resultData.getString("organizer") + " is requesting to meet with you")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setColor(getColor(R.color.colorPrimary))
                    .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                    .setChannelId(getString(R.string.notification_channel_id))
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // notificationId is a unique int for each notification that you must define
            if(isAppIsInBackground(context)){
                Log.d(TAG, "Building notification for meeting ID: " + resultData.getString("meetingID"));
                notificationManager.notify(Integer.parseInt(resultData.getString("meetingID")), builder.build());
                return;
            }


            Intent showMeetingRequest = new Intent();
            showMeetingRequest.putExtra("meetingID", resultData.getString("meetingID"));
            showMeetingRequest.putExtra("organizer", resultData.getString("organizer"));
            showMeetingRequest.putStringArrayListExtra("attendees", resultData.getStringArrayList("attendees"));
            showMeetingRequest.putExtra("begin", resultData.getString("begin"));
            showMeetingRequest.putExtra("end", resultData.getString("end"));
            showMeetingRequest.setAction(getString(R.string.broadcast_show_meeting_request));
            mLocalBroadcastManager.sendBroadcast(showMeetingRequest);

        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(intent.getAction().equals(getString(R.string.broadcast_send_meeting_invite))){
                Log.d(TAG, "Got broadcast to send meeting invite");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            /*
                            Send meeting invite to all participants
                             */
                            SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();

                            JSONObject meeting = new JSONObject();
                            JSONObject invite = new JSONObject(new String(intent.getStringExtra("event")));
                            meeting.put("meetingID", invite.getString("meetingID"));
                            meeting.put("size", invite.getJSONArray("attendees").length());
                            meeting.put("responses", 1);
                            meeting.put("attendees", invite.getJSONArray("attendees"));
                            meeting.put("organizer", username);
                            editor.putString(meeting.getString("meetingID"), meeting.toString());
                            editor.commit();
                            //meetingList.insertMeeting(invite);
                            for(int i = 0; i < invite.getJSONArray("attendees").length(); i++){
                                String attendee = invite.getJSONArray("attendees").get(i).toString();
                                if(!attendee.equals(username)){
                                    sendMeetingInvite(context, invite.toString(), attendee);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }else if(intent.getAction().equals(getString(R.string.broadcast_send_meeting_response))){
                Log.d(TAG, "Got broadcast to send meeting response");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            /*
                            Send meeting response back to intiator
                             */
                            JSONObject response = new JSONObject(new String(intent.getStringExtra("response")));
                            sendMeetingResponse(context, response.toString(),
                                    intent.getStringExtra("organizer"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else if(intent.getAction().equals(getString(R.string.broadcast_send_meeting_start))){

                Log.d(TAG, "Got broadcast to send meeting start message");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            /*
                            Send meeting start to all participants
                             */
                            JSONObject startMessage = new JSONObject(new String(intent.getStringExtra("event")));
                            SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.remove(startMessage.getString("meetingID"));
                            editor.commit();
                            //meetingList.removeMeeting(startMessage.getString("meetingID"));
                            ArrayList<String> attendees = new ArrayList<String>();
                            for(int i = 0; i < startMessage.getJSONArray("attendees").length(); i++){
                                String attendee = startMessage.getJSONArray("attendees").get(i).toString();
                                attendees.add(attendee);
                                if(!attendee.equals(username)){
                                    sendMeetingStart(context, startMessage.toString(), attendee);
                                }
                            }

                            /*
                            Begin MPC
                             */
                            Intent startMpc = new Intent();
                            startMpc.putExtra("meetingID", startMessage.getString("meetingID"));
                            startMpc.putExtra("organizer", username);
                            startMpc.putStringArrayListExtra("attendees", attendees);
                            //startMpc.putExtra("begin", startMessage.getJSONObject("constraints").getString("begin"));
                            //startMpc.putExtra("end", startMessage.getJSONObject("constraints").getString("end"));
                            startMpc.setAction(getString(R.string.broadcast_start_mpc));
                            mLocalBroadcastManager.sendBroadcast(startMpc);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            } else if(intent.getAction().equals(getString(R.string.broadcast_start_mpc))){

                Log.d(TAG, "starting mpc...");
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);


                if (locationManager != null) {
                    boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                    Location location = null;
                    if(preferences.getBoolean(getString(R.string.mock_location), false)){
                        location = new Location("");
                        location.setLatitude(preferences.getFloat(getString(R.string.mock_latitude), 0.0f));
                        location.setLongitude(preferences.getFloat(getString(R.string.mock_longitude), 0.0f));
                    }else{
                        if (isGpsEnabled) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }else if(isNetworkEnabled){
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }else{
                            Log.d(TAG, "No method to receive user location");
                            Toast.makeText(context, "Can't get user's location", Toast.LENGTH_LONG).show();
                        }
                    }

                    if (location != null) {
                        Log.d(TAG, "Got location: " + location);
                        int noise = preferences.getInt(getString(R.string.noise_value), 5);
                        EncodedLatLon loc = new EncodedLatLon((float)location.getLatitude(), (float)location.getLongitude());
                        EncodedLatLon noisyLocation = MapActivity.getNoisyLocation(loc, noise);
                        // Spin up the MPC thread
                        ThreadMPC mpc = new ThreadMPC(context,
                                intent.getStringExtra("meetingID"),
                                channel,
                                intent.getStringArrayListExtra("attendees"),
                                username,
                                noisyLocation.getEncodedLocation(),
                                mpcResponse);
                        mpc.setMode(ThreadMPC.Mode.STATIC_EXECUTABLES);
                        mHandler.post(mpc);
                        Intent showMpcProgress = new Intent();
                        showMpcProgress.setAction(getString(R.string.broadcast_show_mpc_progress));
                        mLocalBroadcastManager.sendBroadcast(showMpcProgress);
                    }else{
                        Toast.makeText(context, "Can't get user's location", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "No method to receive user location");
                        Intent showLocationDialog = new Intent();
                        showLocationDialog.setAction(getString(R.string.broadcast_show_location_dialog));
                        mLocalBroadcastManager.sendBroadcast(showLocationDialog);
                    }
                } else {
                    Log.d(TAG, "LocationManager is null");
                }

            }
        }
    };


    public AMQPCommunication(){return;}


    public void restartAMQPConnection (Context ctx) {
        if (channel != null) {
            try {
                channel.close();
            }
            catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setHost(AMQPHost);
        factory.setPort(AMQPPort);
        factory.setRequestedHeartbeat(5);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setConnectionTimeout(2500);
        setupAMQPConnection(ctx);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        mHandlerThread = new HandlerThread("AMQP-Handler-Thread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        client = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2500);

        client.requestLocationUpdates(mLocationRequest,
                locationCallback,
                null);

        //meetingList = new MeetingList();
        createNotificationChannel();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.broadcast_send_meeting_invite));
        mIntentFilter.addAction(getString(R.string.broadcast_send_meeting_response));
        mIntentFilter.addAction(getString(R.string.broadcast_send_meeting_start));
        mIntentFilter.addAction(getString(R.string.broadcast_start_mpc));

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Starting AMQP service...");

        Notification.Builder builder = new Notification.Builder(this, getString(R.string.notification_channel_id))
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_coffeebreak_56dp)
                .setContentText("Running RabbitMQ in background")
                .setAutoCancel(true);

        Notification notification = builder.build();
        startForeground(1, notification);

        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        if(intent != null){
            AMQPHost = intent.getStringExtra(getString(R.string.amqpIp));
            AMQPPort = Integer.valueOf(intent.getStringExtra(getString(R.string.amqpPort)));
            username = intent.getStringExtra(getString(R.string.username));
            password = intent.getStringExtra(getString(R.string.password));

            editor.putString(getString(R.string.amqpIp), AMQPHost);
            editor.putString(getString(R.string.amqpPort), String.valueOf(AMQPPort));
            editor.putString(getString(R.string.username), username);
            editor.putString(getString(R.string.password), password);


            editor.putFloat(getString(R.string.mock_latitude), intent.getFloatExtra(getString(R.string.mock_latitude), 0.0f));
            editor.putFloat(getString(R.string.mock_longitude), intent.getFloatExtra(getString(R.string.mock_longitude), 0.0f));
            editor.putBoolean(getString(R.string.mock_location), intent.getBooleanExtra(getString(R.string.mock_location), false));

            editor.commit();
        }else{
            AMQPHost = preferences.getString(getString(R.string.amqpIp), getString(R.string.defaultAmqpIp));
            AMQPPort = Integer.parseInt(preferences.getString(getString(R.string.amqpPort), getString(R.string.defaultAmqpPort)));
            username = preferences.getString(getString(R.string.username), getString(R.string.defaultUsername));
            password = preferences.getString(getString(R.string.password), getString(R.string.defaultPassword));
        }

        Log.d(TAG, "got username: " + username + " in shared preferences");

        invite_queue = "invite-" + username;

        factory = new ConnectionFactory();
        factory.setHost(AMQPHost);
        factory.setPort(AMQPPort);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setRequestedHeartbeat(5);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setConnectionTimeout(2500);
        setupAMQPConnection(context);
        return Service.START_STICKY;
    }

    public class SetupAMQPConnection extends AsyncTask<Context, Object, Object> {

        protected Object doInBackground(Context... ctx) {

            try {

                Log.d(TAG, "Attempting to log in with: " + factory.getUsername() + ":" + factory.getPassword());
                connection = factory.newConnection();

                channel = connection.createChannel();
                channel.addShutdownListener(new ShutdownListener() {
                    @Override
                    public void shutdownCompleted(ShutdownSignalException cause) {
                        Log.d(TAG, "In channel shutdown listener");
                        Log.d(TAG, cause.getMessage().toString());
                    }
                });

                channel.exchangeDeclare(INVITE_EXCHANGE, "direct");
                channel.queueDeclare(invite_queue, false, false, true, null);
                channel.queueBind(invite_queue, INVITE_EXCHANGE, username);
                startAMQPListener(context);

            } catch (IOException | TimeoutException e1) {
                retryCount++;
                if(retryCount >= 2){
                    Intent showSettingsFragment = new Intent();
                    showSettingsFragment.setAction(getString(R.string.broadcast_show_settings));
                    mLocalBroadcastManager.sendBroadcast(showSettingsFragment);
                }else{
                    e1.printStackTrace();
                    try {
                        Log.d(TAG, "Setup AMQP Connection failed... Trying again");
                        Thread.sleep(1000);
                        restartAMQPConnection(context);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(Object obj) {

            if(channel != null){
                Log.d(TAG, "Finished SetupAMQPConnection");
                Log.d(TAG, "Ending splash");
                Intent splashEndBroadcast = new Intent();
                // Send broadcast to end splash screen
                splashEndBroadcast.setAction(getString(R.string.broadcast_end_splash));
                mLocalBroadcastManager.sendBroadcast(splashEndBroadcast);
            }else{
                Log.d(TAG, "Error: Channel is NULL");
            }

        }
    }

    // setup the AMQP connection
    public void setupAMQPConnection(Context ctx) {
        new SetupAMQPConnection().execute(ctx);
    }


    // Set-up listener for topic exchange / push notification
    public void startAMQPListener(Context ctx) {

        // Consumer queue for invites
        final Consumer inviteConsumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                //channel.basicAck(envelope.getDeliveryTag(),false);

                Log.d(TAG, "Received message: " + new String(body));

                try {

                    JSONObject message = new JSONObject(new String(body));

                    if(properties.getType().equals("response")){
                        /*
                        This is an invite response message
                         */
                        Log.d(TAG, "Received invite response code: " + message.getInt("response") + " from " + properties.getReplyTo()
                            + " for meeting: " + message.getString("meetingID"));
                        if(message.getInt("response") == 1){
                            SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();

                            String meetingId = message.getString("meetingID");
                            JSONObject meeting = new JSONObject(preferences.getString(meetingId, "{}"));
                            int responseCount =  meeting.getInt("responses") + 1;
                            if(responseCount == meeting.getInt("size")){
                                /*
                                Send MPC Start message to everyone
                                 */
                                Log.d(TAG, "Got repsonses for everyone on meetingID: " + message.getString("meetingID"));
                                Intent sendStartMessage = new Intent();
                                //JSONObject details = meetingList.getMeetingDetails(message.getString("meetingID"));
                                sendStartMessage.putExtra("event", meeting.toString());
                                sendStartMessage.setAction(getString(R.string.broadcast_send_meeting_start));
                                mLocalBroadcastManager.sendBroadcast(sendStartMessage);
                            }else{
                                meeting.put("responses", responseCount);
                                editor.putString(meetingId, meeting.toString());
                                editor.commit();
                            }
                        }else{
                            /*
                            Someone denied to attend the meeting
                            TODO: Send cancel MPC messages to everyone
                             */
                        }

                    }else if(properties.getType().equals("start")){
                        /*
                        start MPC for this meeting
                         */
                        Log.d(TAG, "Received start meeting message!");

                        Intent startMpc = new Intent();
                        startMpc.putExtra("meetingID", message.getString("meetingID"));
                        startMpc.putExtra("organizer", message.getString("organizer"));
                        ArrayList<String> attendees = new ArrayList<String>();
                        for(int i = 0; i < message.getJSONArray("attendees").length(); i++){
                            attendees.add(message.getJSONArray("attendees").getString(i));
                        }
                        startMpc.putStringArrayListExtra("attendees", attendees);
                        //startMpc.putExtra("begin", message.getJSONObject("constraints").getString("begin"));
                        //startMpc.putExtra("end", message.getJSONObject("constraints").getString("end"));
                        startMpc.setAction(getString(R.string.broadcast_start_mpc));
                        mLocalBroadcastManager.sendBroadcast(startMpc);

                    }else if(properties.getType().equals("cancel")){
                        /*
                        cancel the meeting
                         */
                        Log.d(TAG, "Received cancel meeting message");

                    }else{
                        /*
                        This is an invite message
                         */

                        Bundle temp = new Bundle();
                        temp.putString("meetingID", message.getString("meetingID"));
                        temp.putString("organizer", message.getString("organizer"));

                        // Build array list from attendees
                        ArrayList<String> attendees = new ArrayList<String>();
                        for(int i = 0; i < message.getJSONArray("attendees").length(); i++){
                            attendees.add(message.getJSONArray("attendees").get(i).toString());
                        }
                        temp.putStringArrayList("attendees", attendees);

                        temp.putString("begin", message.getJSONObject("constraints").getString("begin"));
                        temp.putString("end", message.getJSONObject("constraints").getString("end"));

                        showMeetingRequestDialog.send(0, temp);
                    }



                } catch (Exception e) {
                    Log.d(TAG, "Got exception in consumer");
                    e.printStackTrace();
                }

            }
        };

        try {
            channel.basicConsume(invite_queue, true, inviteConsumer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Function for sending a message to a user
     */
    public void sendMeetingInvite(Context c, String data, String routing_key) {

            //set the message properties
            AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                    .correlationId(UUID.randomUUID().toString())
                    .type("invite")
                    .replyTo(username)
                    .build();

            if(channel == null){
                Log.d(TAG, "Error sending meeting invite");
                return;
            }

        try {
            channel.basicPublish(INVITE_EXCHANGE, routing_key, basicProperties, data.getBytes());
            Log.d(TAG, "[x] Sent meeting invite to " + routing_key);
        } catch (Exception e) {
            Log.d(TAG, "Exception on AMQP Channel");
            e.printStackTrace();
        }
    }

    public void sendMeetingResponse(Context c, String data, String routing_key) {

        //set the message properties
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .correlationId(UUID.randomUUID().toString())
                .type("response")
                .replyTo(username)
                .build();

        if(channel == null){
            Log.d(TAG, "Error sending meeting response");
            return;
        }

        try {
            channel.basicPublish(INVITE_EXCHANGE, routing_key, basicProperties, data.getBytes());
            Log.d(TAG, "[x] Sent meeting response to " + routing_key);
        } catch (Exception e) {
            Log.d(TAG, "Exception occured when sending meeting response");
            e.printStackTrace();
        }
    }

    public void sendMeetingStart(Context c, String data, String routing_key) {

        //set the message properties
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .correlationId(UUID.randomUUID().toString())
                .type("start")
                .replyTo(username)
                .build();

        if(channel == null){
            Log.d(TAG, "Error sending meeting start");
            return;
        }

        try {
            channel.basicPublish(INVITE_EXCHANGE, routing_key, basicProperties, data.getBytes());
            Log.d(TAG, "[x] Sent meeting start to " + routing_key);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(connection != null){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        mHandlerThread.quitSafely();
        client.removeLocationUpdates(locationCallback);
    }

    /*
    Create notification channel for alerting the user when the app is closed
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {

        CharSequence name = getString(R.string.notification_channel);
        String description = getString(R.string.notification_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(getString(R.string.notification_channel_id), name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

    }

    /*
    Check whether or not Coffee Break is the foreground activity
     */
    private boolean isAppIsInBackground(Context context) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo p : processes) {
            if (p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String activeProcess : p.pkgList) {
                    if (activeProcess.equals(context.getPackageName())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

}

