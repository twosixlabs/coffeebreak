package com.bbn.coffeebreak;

// DialogSheet from: https://github.com/marcoscgdev/DialogSheet

import android.content.Context;
import com.marcoscg.dialogsheet.DialogSheet;

public class MeetingRequestDialog {

    private static DialogSheet dialogSheet;

    public static DialogSheet request(Context c, String message,
                                      DialogSheet.OnPositiveClickListener posListen,
                                      DialogSheet.OnNegativeClickListener negListen){

        dialogSheet = new DialogSheet(c)
                .setTitle(c.getString(R.string.meeting_request_title))
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(c.getString(R.string.accept), posListen)
                .setNegativeButton(c.getString(R.string.deny), negListen)
                .setButtonsColorRes(R.color.colorPrimary)
                .setBackgroundColorRes(R.color.white);
        return dialogSheet;
    }

    public static void dismiss() {
        dialogSheet.dismiss();
    }
}
