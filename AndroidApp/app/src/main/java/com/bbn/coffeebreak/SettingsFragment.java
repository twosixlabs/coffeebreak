package com.bbn.coffeebreak;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

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
        String username = preferences.getString(getString(R.string.username), getString(R.string.defaultUsername));
        String password = preferences.getString(getString(R.string.password), getString(R.string.defaultPassword));

        amqpServerEditText.setText(amqpServer);
        amqpPortEditText.setText(amqpPort);
        usernameEditText.setText(username);
        //passwordEditText.setText(password);

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
                i.putExtra(getString(R.string.username), usernameEditText.getText().toString());
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
