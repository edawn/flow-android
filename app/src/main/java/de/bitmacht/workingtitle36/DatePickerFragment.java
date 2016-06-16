package de.bitmacht.workingtitle36;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends TimeDatePickerDialogFragment implements DatePickerDialog.OnDateSetListener {

    /**
     * The key of an optional argument containing the id of this fragment. The id will used when
     * calling {@link OnDateSetListener#onDateSet}
     */
    public static final String ARG_ID = "id";

    /**
     * The key of an optional argument to be passed to this fragment, containing the unix time (in ms)
     * of the first date to be shown in the picker
     */
    public static final String ARG_MIN_DATE = "minDate";

    private int id = 0;

    /**
     * Create a new instance
     */
    public DatePickerFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = getCalendarFromArguments();
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_MIN_DATE)) {
                dialog.getDatePicker().setMinDate(args.getLong(ARG_MIN_DATE));
            }
            if (args.containsKey(ARG_ID)) {
                id = args.getInt(ARG_ID);
            }
        }
        return dialog;
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
