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
                .setBackgroundColorInt(R.color.colorPrimary)
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
                .setBackgroundColorInt(R.color.colorPrimary)
                .setBackgroundColorRes(R.color.colorPrimary)
                .setDuration(c.getResources().getInteger(R.integer.alerter_notification_duration))
                .setIcon(c.getDrawable(android.R.drawable.ic_menu_my_calendar));
    }
}
