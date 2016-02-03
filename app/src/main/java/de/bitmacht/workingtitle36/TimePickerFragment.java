package de.bitmacht.workingtitle36;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TimePicker;

import java.util.Calendar;

public class TimePickerFragment extends TimeDatePickerDialogFragment implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = getCalendarFromArguments();
        return new TimePickerDialog(getActivity(), this, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), true);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Activity activity = getActivity();
        if (activity instanceof TimePickerDialog.OnTimeSetListener) {
            ((TimePickerDialog.OnTimeSetListener) activity).onTimeSet(view, hourOfDay, minute);
        }
    }
}
