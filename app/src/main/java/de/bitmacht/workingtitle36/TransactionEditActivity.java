package de.bitmacht.workingtitle36;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueModifyView;
import de.bitmacht.workingtitle36.view.ValueWidget;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener,
        ValueModifyView.OnValueChangeListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener, TransactionsUpdateTask.UpdateFinishedCallback {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEditActivity.class);

    private Currency currency;
    private long amount;

    private Toolbar toolbar;
    private ImageButton cancelButton;
    private ImageButton acceptButton;
    private TimeView timeView;
    private TimeView dateView;
    private ValueWidget valueWidget;
    private ValueModifyView valueModMoreView;
    private ValueModifyView valueModLessView;
    private EditText descriptionView;
    private Calendar calendar = new GregorianCalendar();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO the currency should be user-settable
        currency = Currency.getInstance(Locale.getDefault());
        amount = 0;

        setContentView(R.layout.activity_transaction_edit);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        cancelButton = (ImageButton) findViewById(R.id.cancel_button);
        acceptButton = (ImageButton) findViewById(R.id.accept_button);

        cancelButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);

        timeView = (TimeView) findViewById(R.id.time);
        dateView = (TimeView) findViewById(R.id.date);
        valueWidget = (ValueWidget) findViewById(R.id.value);
        valueModMoreView = (ValueModifyView) findViewById(R.id.value_modify_more);
        valueModLessView = (ValueModifyView) findViewById(R.id.value_modify_less);
        descriptionView = (EditText) findViewById(R.id.description);

        if (savedInstanceState != null && savedInstanceState.containsKey(DBHelper.EDITS_KEY_CREATION_TIME)) {
            calendar.setTimeInMillis(savedInstanceState.getLong(DBHelper.EDITS_KEY_CREATION_TIME));
        }
        updateTimeViews();
        timeView.setOnClickListener(this);
        dateView.setOnClickListener(this);

        valueWidget.setValue(currency, amount);

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
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                if (BuildConfig.DEBUG) {
                    logger.trace("edit: {}", getEdit());
                }
                TransactionsUpdateTask tut = new TransactionsUpdateTask(this, this);
                setUpdatingState(true);
                tut.execute(getEdit());
            } else {
                //TODO if there was any data entered, show confirmation dialog
                // or: save the data, finish and show a snackbar
                finish();
            }

        } else if (id == R.id.time || id == R.id.date) {
            DialogFragment frag = id == R.id.time ? new TimePickerFragment() : new DatePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, calendar.getTimeInMillis());
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), id == R.id.time ? "timePicker" : "datePicker");

            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    @Override
    public void onValueChange(Currency currency, int amount) {
        if (BuildConfig.DEBUG) {
            logger.trace("value change: {},{}", currency, amount);
        }

        //TODO make sure that this.currency.equals(currency)
        this.amount += amount;
        valueWidget.setValue(currency, this.amount);
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

    /**
     * Returns an Edit matching the currently set data
     */
    private Edit getEdit() {
        Edit edit = new Edit(System.currentTimeMillis(), calendar.getTimeInMillis(),
                descriptionView.getText().toString(), "", currency.getCurrencyCode(), amount);
        return edit;
    }

    @Override
    public void onUpdateFinished(boolean success) {
        setUpdatingState(false);
        if (success) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "update failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Modifies the UI to express that a database update is in progress.
     * @param isUpdating true if the update is about to start; false if it has ended
     */
    private void setUpdatingState(boolean isUpdating) {
        acceptButton.setEnabled(!isUpdating);
        acceptButton.setClickable(!isUpdating);
    }
}
