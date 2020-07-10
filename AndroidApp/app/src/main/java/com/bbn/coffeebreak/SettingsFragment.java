package com.bbn.coffeebreak;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsFragment extends DialogFragment {

    final private static String TAG = "[SettingsFragment]";

    private EditText amqpServerEditText;
    private EditText amqpPortEditText;
    private EditText usernameEditText;
    //private EditText passwordEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        // Set transparent background and no title
        final View v = inflater.inflate(R.layout.settings_fragment, container, false);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // Shared Preferences for storing tardis information; this is where the privacy setting will be saved
        final SharedPreferences preferences = getActivity().getSharedPreferences(getString(R.string.shared_preferences), Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();

        amqpServerEditText = (EditText) v.findViewById(R.id.amqp_server_ip);
        amqpPortEditText = (EditText) v.findViewById(R.id.amqp_server_port);
        usernameEditText = (EditText) v.findViewById(R.id.amqp_username);
        //passwordEditText = (EditText) v.findViewById(R.id.amqp_password);


        String amqpServer = preferences.getString(getString(R.string.amqpIp), getString(R.string.defaultAmqpIp));
        String amqpPort = preferences.getString(getString(R.string.amqpPort), getString(R.string.defaultAmqpPort));
        String username = PhoneNumberUtils.formatNumberToE164(preferences.getString(getString(R.string.username), getString(R.string.defaultUsername)), "US");
        String password = preferences.getString(getString(R.string.password), getString(R.string.defaultPassword));
        Log.d(TAG, "got username: " + username);
        if(username == null){
            TelephonyManager tMgr = (TelephonyManager)getContext().getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("MissingPermission")
            String mPhoneNumber = tMgr.getLine1Number();
            Log.d(TAG, "Got phone number: " + mPhoneNumber);
            if(mPhoneNumber != "")
                username = mPhoneNumber;
        }

        amqpServerEditText.setText(amqpServer);
        amqpPortEditText.setText(amqpPort);
        usernameEditText.setText(username);
        //passwordEditText.setText(password);

        // Timeout bar can be set in 15s increments, will be the invite timeout for a meeting sent by this user
        SeekBar timeoutValue = (SeekBar) v.findViewById(R.id.timeout_seek_bar);
        TextView timeoutMessage = (TextView) v.findViewById(R.id.timeout_message);

        timeoutMessage.setText("Invite timeout after: " + (preferences.getInt("timeout", 4) * 15) + "s");
        timeoutValue.setProgress(preferences.getInt("timeout", 4));
        timeoutValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeoutMessage.setText("Invite timeout after: " + String.valueOf(progress * 15) + "s");
                editor.putInt("timeout", progress);
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getContext(), "Meeting invites will timeout in " +
                        String.valueOf(preferences.getInt("timeout", 4) * 15) + "s", Toast.LENGTH_LONG).show();
            }
        });

        // Timeout bar can be set in 15s increments, will be the mpc calculation timeout for a meeting sent by this user
        SeekBar mpctimeoutValue = (SeekBar) v.findViewById(R.id.mpc_timeout_seek_bar);
        TextView mpctimeoutMessage = (TextView) v.findViewById(R.id.mpc_timeout_message);

        mpctimeoutMessage.setText("Calculation timeout after: " + (preferences.getInt("mpc_timeout", 2) * 15) + "s");
        mpctimeoutValue.setProgress(preferences.getInt("mpc_timeout", 2));
        mpctimeoutValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mpctimeoutMessage.setText("Calculation timeout after: " + String.valueOf(progress * 15) + "s");
                editor.putInt("mpc_timeout", progress);
                editor.commit();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getContext(), "Calculation stops after " +
                        String.valueOf(preferences.getInt("mpc_timeout", 4) * 15) + "s of no activity", Toast.LENGTH_LONG).show();
            }
        });

        Button submitButton = (Button) v.findViewById(R.id.settings_ok_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                editor.clear();
                editor.putString(getString(R.string.amqpIp), amqpServerEditText.getText().toString());
                editor.putString(getString(R.string.amqpPort), amqpPortEditText.getText().toString());
                editor.putString(getString(R.string.username), usernameEditText.getText().toString());
                editor.putString(getString(R.string.password), passwordEditText.getText().toString());

                editor.commit();
                */

                Intent i = new Intent(getActivity().getApplicationContext(), SplashActivity.class);
                i.putExtra(getString(R.string.amqpIp), amqpServerEditText.getText().toString());
                i.putExtra(getString(R.string.amqpPort), amqpPortEditText.getText().toString());
                if(PhoneNumberUtils.formatNumberToE164(usernameEditText.getText().toString(), "US") == null){
                    Toast.makeText(getContext(), "Not a valid US phone number", Toast.LENGTH_LONG).show();
                    return;
                }
                i.putExtra(getString(R.string.username), PhoneNumberUtils.formatNumberToE164(usernameEditText.getText().toString(), "US"));
                //i.putExtra(getString(R.string.password), passwordEditText.getText().toString());

                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                Intent amqpService = new Intent(getContext(), AMQPCommunication.class);
                getContext().stopService(amqpService);
                startActivity(i);
            }
        });


        return v;
    }

}
