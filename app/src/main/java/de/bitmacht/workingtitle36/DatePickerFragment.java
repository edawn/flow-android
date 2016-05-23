package de.bitmacht.workingtitle36;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends TimeDatePickerDialogFragment implements DatePickerDialog.OnDateSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = getCalendarFromArguments();
        return new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Activity activity = getActivity();
        if (activity instanceof DatePickerDialog.OnDateSetListener) {
            ((DatePickerDialog.OnDateSetListener) activity).onDateSet(view, year, monthOfYear, dayOfMonth);
        }
    }
}
