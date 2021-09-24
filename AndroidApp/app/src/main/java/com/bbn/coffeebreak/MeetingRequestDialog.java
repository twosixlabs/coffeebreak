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

    public static void reset() {
        if (dialogSheet != null) {
            dialogSheet.dismiss();
            dialogSheet = null;
        }
    }
}
