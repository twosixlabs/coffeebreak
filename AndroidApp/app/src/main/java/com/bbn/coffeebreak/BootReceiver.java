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
