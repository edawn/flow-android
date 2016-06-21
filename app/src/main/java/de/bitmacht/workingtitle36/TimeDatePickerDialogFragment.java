/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitmacht.workingtitle36;

import android.app.DialogFragment;
import android.os.Bundle;

import java.util.Calendar;
import java.util.GregorianCalendar;

public abstract class TimeDatePickerDialogFragment extends DialogFragment {

    // The initial time that this dialog should show
    public static final String BUNDLE_TIME = "time";

    /**
     * Return a Calendar having its time set to the time passed in by the arguments
     * @return A Calendar with a time set to the time contained in the argument Bundle or to the
     * current time if the argument has not been set
     */
    final Calendar getCalendarFromArguments() {
        Bundle bundle = getArguments();
        long time = System.currentTimeMillis();
        if (bundle != null && bundle.containsKey(BUNDLE_TIME)) {
            time = bundle.getLong(BUNDLE_TIME);
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(time);
        return calendar;
    }
}
