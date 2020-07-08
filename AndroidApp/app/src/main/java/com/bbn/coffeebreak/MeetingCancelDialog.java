package com.bbn.coffeebreak;

// DialogSheet from: https://github.com/marcoscgdev/DialogSheet

import android.content.Context;
import android.content.DialogInterface;

import com.marcoscg.dialogsheet.DialogSheet;

public class MeetingCancelDialog {

    private static DialogSheet dialogSheet;

    public static DialogSheet request(Context c, String message,
                                      DialogSheet.OnPositiveClickListener posListen){
        dialogSheet =  new DialogSheet(c)
                .setTitle(c.getString(R.string.meeting_request_title))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, posListen)
                .setButtonsColorRes(R.color.colorPrimary)
                .setBackgroundColorRes(R.color.white)
                .setRoundedCorners(false);

        return dialogSheet;
    }

    public static boolean dialogExists() {
        if (dialogSheet == null) {
            return false;
        }
        return true;
    }

    public static void reset() {
        if (dialogSheet != null) {
            dialogSheet.dismiss();
            dialogSheet = null;
        }
    }
}
