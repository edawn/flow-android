package de.bitmacht.workingtitle36;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends TimeDatePickerDialogFragment implements DatePickerDialog.OnDateSetListener {

    private final int id;

    /**
     * Create a new instance
     * @param id
     */
    public DatePickerFragment(int id) {
        this.id = id;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = getCalendarFromArguments();
        return new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Activity activity = getActivity();
        if (activity instanceof DatePickerFragment.OnDateSetListener) {
            ((OnDateSetListener) activity).onDateSet(id, year, monthOfYear, dayOfMonth);
        }
    }

    public interface OnDateSetListener {
        void onDateSet(int id, int year, int monthOfYear, int dayOfMonth);
    }
}
