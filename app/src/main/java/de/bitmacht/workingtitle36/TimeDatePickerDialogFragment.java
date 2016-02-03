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
