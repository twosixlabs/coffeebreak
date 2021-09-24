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