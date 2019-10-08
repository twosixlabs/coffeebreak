package com.bbn.coffeebreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(context.getString(R.string.shared_preferences), context.MODE_PRIVATE);
        Intent i = new Intent(context, AMQPCommunication.class);
        i.putExtra(context.getString(R.string.amqpIp), preferences.getString(context.getString(R.string.amqpIp), context.getString(R.string.defaultAmqpIp)));
        i.putExtra(context.getString(R.string.amqpPort), preferences.getString(context.getString(R.string.amqpPort), context.getString(R.string.defaultAmqpPort)));
        i.putExtra(context.getString(R.string.username), preferences.getString(context.getString(R.string.username), context.getString(R.string.defaultUsername)));
        i.putExtra(context.getString(R.string.password), preferences.getString(context.getString(R.string.password), context.getString(R.string.defaultPassword)));
        i.putExtra(context.getString(R.string.mock_latitude), preferences.getFloat(context.getString(R.string.mock_latitude),0.0f));
        i.putExtra(context.getString(R.string.mock_longitude), preferences.getFloat(context.getString(R.string.mock_longitude),0.0f));
        i.putExtra(context.getString(R.string.mock_location), preferences.getBoolean(context.getString(R.string.mock_location), false));
        context.startForegroundService(i);
    }
}
