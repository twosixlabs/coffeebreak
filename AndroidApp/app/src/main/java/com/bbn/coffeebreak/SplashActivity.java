package com.bbn.coffeebreak;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SplashActivity extends Activity {

    private final static String TAG = "[SplashActivity]";
    private static String username = "";
    private static String password = "";
    private static String amqpIp = "";
    private static String amqpPort = "";

    final private static int REQUEST_PERMISSIONS = 0;

    private LocalBroadcastManager mLocalBroadcastManager;


    // For ending the Splash Screen
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(getString(R.string.broadcast_end_splash))){
                Log.d(TAG, "Got broadcast to end splash screen");
                Intent mainIntent = new Intent(context, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }else if(intent.getAction().equals(getString(R.string.broadcast_show_settings))){
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("settingsFragment");
                if(prev != null){
                    if(prev.isVisible())
                        return;
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                DialogFragment dialogFragment = new SettingsFragment();
                dialogFragment.setCancelable(false);
                dialogFragment.show(ft, "settingsFragment");
                Toast.makeText(context, "Can't connect to AMQP server.", Toast.LENGTH_LONG).show();

            }
        }
    };



    // minimal amount of time to display the splash screen
    final long delay = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //setup sharedPreferences for configuration options
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);;
        SharedPreferences.Editor editor = sharedPreferences.edit();

        /*
        Set up all the configuration options for RapidGather via arguments passed
        on the command line
         */

        Bundle demoParams = getIntent().getExtras();
        if(demoParams != null){
            if(demoParams.containsKey(getResources().getString(R.string.username))){
                username = demoParams.getString(getResources().getString(R.string.username)).trim();
                editor.putString(getResources().getString(R.string.username), username);
                Log.d(TAG, "Setting username: " + demoParams.getString(getResources().getString(R.string.username)));
            }

            if(demoParams.containsKey(getResources().getString(R.string.password))){
                password = demoParams.getString(getResources().getString(R.string.password)).trim();
                editor.putString(getResources().getString(R.string.password), password);
                Log.d(TAG, "Setting password: " + demoParams.getString(getResources().getString(R.string.password)));
            }

            if(demoParams.containsKey(getResources().getString(R.string.amqpIp))){
                amqpIp = demoParams.getString(getResources().getString(R.string.amqpIp));
                editor.putString(getResources().getString(R.string.amqpIp), amqpIp);
                Log.d(TAG, "Setting AMQP IP: " + demoParams.getString(getResources().getString(R.string.amqpIp)));
            }

            if(demoParams.containsKey(getResources().getString(R.string.amqpPort))){
                amqpPort = demoParams.getString(getResources().getString(R.string.amqpPort));
                editor.putString(getResources().getString(R.string.amqpPort), amqpPort);
                Log.d(TAG, "Setting AMQP Port: " + demoParams.getString(getResources().getString(R.string.amqpPort)));
            }

            if(demoParams.containsKey(getString(R.string.mock_latitude))){
                editor.putFloat(getString(R.string.mock_latitude), demoParams.getFloat(getString(R.string.mock_latitude)));
            }

            if(demoParams.containsKey(getString(R.string.mock_longitude))){
                editor.putFloat(getString(R.string.mock_longitude), demoParams.getFloat(getString(R.string.mock_longitude)));
            }


            editor.commit();
        }


        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getString(R.string.broadcast_end_splash));
        mIntentFilter.addAction(getString(R.string.broadcast_show_settings));
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(SplashActivity.this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(SplashActivity.this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_NUMBERS},
                    REQUEST_PERMISSIONS);
        }
        else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void run() {
                    SetupApp();
                }
            }, delay);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void SetupApp(){
        Log.d(TAG, "Setting up the app");
        //Start AMQP communication
        SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preferences), MODE_PRIVATE);
        Intent i = new Intent(SplashActivity.this, AMQPCommunication.class);
        i.putExtra(getString(R.string.amqpIp), preferences.getString(getString(R.string.amqpIp), getString(R.string.defaultAmqpIp)));
        i.putExtra(getString(R.string.amqpPort), preferences.getString(getString(R.string.amqpPort), getString(R.string.defaultAmqpPort)));
        i.putExtra(getString(R.string.username), preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)));
        i.putExtra(getString(R.string.password), preferences.getString(getString(R.string.password), getString(R.string.defaultPassword)));
        i.putExtra(getString(R.string.mock_latitude), preferences.getFloat(getString(R.string.mock_latitude),0.0f));
        i.putExtra(getString(R.string.mock_longitude), preferences.getFloat(getString(R.string.mock_longitude),0.0f));
        i.putExtra(getString(R.string.mock_location), preferences.getBoolean(getString(R.string.mock_location), false));
        getApplicationContext().startForegroundService(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "GRANTED LOCATION PERMISSION");
                    Intent i = new Intent(SplashActivity.this, SplashActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);

                } else {
                    finish();
                }
                return;
            }

        }
    }

}
