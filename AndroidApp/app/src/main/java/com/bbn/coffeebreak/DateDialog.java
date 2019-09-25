package com.bbn.coffeebreak;

import android.content.Context;

import com.borax12.materialdaterangepicker.date.DatePickerDialog;

import java.util.Calendar;

public class DateDialog {

    //The date picker dialog displays a simple date picker and performs the action in the listener
    public static DatePickerDialog datePicker(final Context c, DatePickerDialog.OnDateSetListener listener){
        Calendar now = Calendar.getInstance();
        return DatePickerDialog.newInstance(
                listener,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
    }
}
