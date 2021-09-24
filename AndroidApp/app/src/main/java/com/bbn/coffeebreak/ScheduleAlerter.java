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

// Alerter library
// Found at: https://github.com/Tapadoo/Alerter


import android.app.Activity;
import android.content.Context;

import com.tapadoo.alerter.Alerter;

import java.util.Objects;

public class ScheduleAlerter {

    //Displays an alert that a meeting was scheduled
    public static Alerter scheduled(Context c, int month, int day, int year){
        return Alerter.create((Activity)c).setTitle(c.getString(R.string.scheduled_meeting_title))
                .setText(String.valueOf(month + 1) + "/" + String.valueOf(day) + "/" +
                        String.valueOf(year))
                .setBackgroundColorInt(c.getResources().getColor(R.color.colorPrimary))
                .setBackgroundColorRes(R.color.colorPrimary)
                .setDuration(c.getResources().getInteger(R.integer.alerter_notification_duration))
                .setIcon(c.getDrawable(android.R.drawable.ic_menu_my_calendar));
    }

    //Displays an alert that a meeting is being scheduled with a list of contacts
    public static Alerter scheduling(Context c, String [] contacts){

        StringBuilder meetingContacts = new StringBuilder("with ");
        for(String s : contacts){
            meetingContacts.append(s);
            if(!Objects.equals(s, contacts[contacts.length - 1]))
                meetingContacts.append(", ");
        }
        return Alerter.create((Activity)c).setTitle(c.getString(R.string.meeting_attempt))
                .setText(meetingContacts.toString())
                .setBackgroundColorInt(c.getResources().getColor(R.color.colorPrimary))
                .setBackgroundColorRes(R.color.colorPrimary)
                .setDuration(c.getResources().getInteger(R.integer.alerter_notification_duration))
                .setIcon(c.getDrawable(android.R.drawable.ic_menu_my_calendar));
    }
}
