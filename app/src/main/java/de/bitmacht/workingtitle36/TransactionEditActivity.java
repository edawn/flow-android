package de.bitmacht.workingtitle36;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueModifyView;
import de.bitmacht.workingtitle36.view.ValueView;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener,
        ValueModifyView.OnValueChangeListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEditActivity.class);

    private Currency currency;
    private long value;

    private TimeView timeView;
    private TimeView dateView;
    private ValueView valueView;
    private ValueModifyView valueModMoreView;
    private ValueModifyView valueModLessView;
    private EditText descriptionView;
    private Calendar calendar = new GregorianCalendar();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO the currency should be user-settable
        currency = Currency.getInstance(Locale.getDefault());
        value = 0;

        setContentView(R.layout.activity_transaction_edit);

        timeView = (TimeView) findViewById(R.id.time);
        dateView = (TimeView) findViewById(R.id.date);
        valueView = (ValueView) findViewById(R.id.value);
        valueModMoreView = (ValueModifyView) findViewById(R.id.value_modify_more);
        valueModLessView = (ValueModifyView) findViewById(R.id.value_modify_less);
        descriptionView = (EditText) findViewById(R.id.description);

        if (savedInstanceState != null && savedInstanceState.containsKey(DBHelper.EDITS_KEY_CREATION_TIME)) {
            calendar.setTimeInMillis(savedInstanceState.getLong(DBHelper.EDITS_KEY_CREATION_TIME));
        }
        updateTimeViews();
        timeView.setOnClickListener(this);
        dateView.setOnClickListener(this);

        valueView.setValue(currency, value);

        valueModLessView.setValue(currency, 10);
        valueModMoreView.setOnValueChangeListener(this);

        valueModMoreView.setValue(currency, 100);
        valueModLessView.setOnValueChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DBHelper.EDITS_KEY_CREATION_TIME, calendar.getTimeInMillis());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        DialogFragment frag;
        Bundle bundle = new Bundle();
        bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, calendar.getTimeInMillis());

        if (id == R.id.time) {
            frag = new TimePickerFragment();
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), "timePicker");
        } else if (id == R.id.date) {
            frag = new DatePickerFragment();
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), "datePicker");
        }

        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    @Override
    public void onValueChange(Currency currency, int cents) {
        if (BuildConfig.DEBUG) {
            logger.trace("value change: {},{}", currency, cents);
        }

        //TODO make sure that this.currency.equals(currency)
        value += cents;
        valueView.setValue(currency, value);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateTimeViews();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        updateTimeViews();
    }

    private void updateTimeViews() {
        long time = calendar.getTimeInMillis();
        timeView.setTime(time);
        dateView.setTime(time);
    }
}
