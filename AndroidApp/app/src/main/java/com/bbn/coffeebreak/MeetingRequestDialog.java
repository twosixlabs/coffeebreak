package com.bbn.coffeebreak;

// DialogSheet from: https://github.com/marcoscgdev/DialogSheet

import android.content.Context;
import android.content.DialogInterface;

import com.marcoscg.dialogsheet.DialogSheet;

public class MeetingRequestDialog {

    private static DialogSheet dialogSheet;

    public static DialogSheet request(Context c, String message,
                                      DialogSheet.OnPositiveClickListener posListen,
                                      DialogSheet.OnNegativeClickListener negListen,
                                      DialogInterface.OnDismissListener onDismissListener){

        dialogSheet = new DialogSheet(c)
                .setTitle(c.getString(R.string.meeting_request_title))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(c.getString(R.string.accept), posListen)
                .setNegativeButton(c.getString(R.string.deny), negListen)
                .setOnDismissListener(onDismissListener)
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

    public static void dismiss() {
        dialogSheet.dismiss();
    }
}
