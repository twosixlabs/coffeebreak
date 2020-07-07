package com.bbn.coffeebreak;

//This is used for selecting multiple contacts at once
//https://github.com/broakenmedia/MultiContactPicker

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

import com.wafflecopter.multicontactpicker.LimitColumn;
import com.wafflecopter.multicontactpicker.MultiContactPicker;

public class ContactPicker {
    public static void startContactPicker(Context c){
        new MultiContactPicker.Builder((Activity)c)
                .theme(R.style.MyCustomPickerTheme)
                .hideScrollbar(false)
                .showTrack(true)
                .searchIconColor(Color.WHITE)
                .setChoiceMode(MultiContactPicker.CHOICE_MODE_MULTIPLE)
                .handleColor(ContextCompat.getColor(c, R.color.colorPrimary))
                .bubbleColor(ContextCompat.getColor(c, R.color.colorPrimary))
                .bubbleTextColor(Color.WHITE)
                .limitToColumn(LimitColumn.PHONE)
                .showPickerForResult(c.getResources().getInteger(R.integer.contact_picker_request));
    }
}