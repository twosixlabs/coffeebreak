package com.bbn.coffeebreak;

// DialogSheet from: https://github.com/marcoscgdev/DialogSheet

import android.content.Context;
import com.marcoscg.dialogsheet.DialogSheet;

public class MeetingCancelDialog {

    public static DialogSheet request(Context c, String message,
                                      DialogSheet.OnPositiveClickListener posListen){
        return new DialogSheet(c)
                .setTitle(c.getString(R.string.meeting_request_title))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, posListen)
                .setButtonsColorRes(R.color.colorPrimary)
                .setBackgroundColorRes(R.color.white);
    }
}
