package de.bitmacht.workingtitle36;

import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueEditText;
import de.bitmacht.workingtitle36.view.ValueModifyView;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener,
        ValueModifyView.OnValueChangeListener, TimePickerDialog.OnTimeSetListener, DatePickerFragment.OnDateSetListener, TransactionsUpdateTask.UpdateFinishedCallback {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEditActivity.class);

    /**
     * An optional extra containing a TransactionsModel that will be edited
     */
    public static final String EXTRA_TRANSACTION = "transaction";

    private static final String STATE_VALUE_KEY = "value";

    private Long transactionId = null;
    private Long parentId = null;
    private Calendar transactionTime = new GregorianCalendar();

    private Toolbar toolbar;
    private ImageButton cancelButton;
    private ImageButton acceptButton;
    private TimeView timeView;
    private TimeView dateView;
    private ValueEditText valueEditText;
    private ValueModifyView valueModMoreView;
    private ValueModifyView valueModLessView;
    private EditText descriptionView;
    private EditText locationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transaction_edit);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        cancelButton = (ImageButton) findViewById(R.id.cancel_button);
        acceptButton = (ImageButton) findViewById(R.id.accept_button);
        timeView = (TimeView) findViewById(R.id.time);
        dateView = (TimeView) findViewById(R.id.date);
        valueEditText = (ValueEditText) findViewById(R.id.value);
        valueModMoreView = (ValueModifyView) findViewById(R.id.value_modify_more);
        valueModLessView = (ValueModifyView) findViewById(R.id.value_modify_less);
        descriptionView = (EditText) findViewById(R.id.description);
        locationView = (EditText) findViewById(R.id.location);

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        cancelButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);

        timeView.setOnClickListener(this);
        dateView.setOnClickListener(this);

        Value value = null;
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_TRANSACTION)) {
                TransactionsModel transaction = intent.getParcelableExtra(EXTRA_TRANSACTION);
                if (transaction.mostRecentEdit != null) {
                    transactionId = transaction.id;
                    Edit parentEdit = transaction.mostRecentEdit;
                    parentId = parentEdit.id;
                    transactionTime.setTimeInMillis(parentEdit.transactionTime);
                    descriptionView.setText(parentEdit.transactionDescription);
                    locationView.setText(parentEdit.transactionLocation);
                    value = parentEdit.getValue();
                } else {
                    if (BuildConfig.DEBUG) {
                        logger.warn("A transaction without an edit: id: {}", transaction.id);
                    }
                }
            }
        } else {
            if (savedInstanceState.containsKey(DBHelper.TRANSACTIONS_KEY_ID)) {
                transactionId = savedInstanceState.getLong(DBHelper.TRANSACTIONS_KEY_ID);
                parentId = savedInstanceState.getLong(DBHelper.EDITS_KEY_PARENT);
            }
            transactionTime.setTimeInMillis(savedInstanceState.getLong(DBHelper.EDITS_KEY_TRANSACTION_TIME));
            value = savedInstanceState.getParcelable(STATE_VALUE_KEY);
        }

        updateTimeViews();

        if (value == null) {
            value = new Value(MyApplication.getCurrency().getCurrencyCode(), 0);
        }
        valueEditText.setValue(value);

        valueModLessView.setValue(value.withAmount(10));
        valueModMoreView.setOnValueChangeListener(this);

        valueModMoreView.setValue(value.withAmount(100));
        valueModLessView.setOnValueChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (transactionId != null) {
            outState.putLong(DBHelper.TRANSACTIONS_KEY_ID, transactionId);
            // the existence of parentId depends on the existence of transactionId
            outState.putLong(DBHelper.EDITS_KEY_PARENT, parentId);
        }
        outState.putLong(DBHelper.EDITS_KEY_TRANSACTION_TIME, transactionTime.getTimeInMillis());
        outState.putParcelable(STATE_VALUE_KEY, valueEditText.getValue());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                Edit edit = getEdit();
                if (BuildConfig.DEBUG) {
                    logger.trace("edit: {}", edit);
                }
                new TransactionsUpdateTask(this, this, edit).execute();
                setUpdatingState(true);
            } else {
                //TODO if there was any data entered, show confirmation dialog
                // or: save the data, finish and show a snackbar
                finish();
            }

        } else if (id == R.id.time || id == R.id.date) {
            DialogFragment frag = id == R.id.time ? new TimePickerFragment() : new DatePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, transactionTime.getTimeInMillis());
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), id == R.id.time ? "timePicker" : "datePicker");

            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    @Override
    public void onValueChange(Value difference) {
        Value value = valueEditText.getValue();
        try {
            value = value.add(difference);
            valueEditText.setValue(value);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.trace("unable to change amount", e);
            }
        }
    }

    @Override
    public void onDateSet(int id, int year, int monthOfYear, int dayOfMonth) {
        transactionTime.set(Calendar.YEAR, year);
        transactionTime.set(Calendar.MONTH, monthOfYear);
        transactionTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateTimeViews();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        transactionTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        transactionTime.set(Calendar.MINUTE, minute);
        updateTimeViews();
    }

    private void updateTimeViews() {
        long time = transactionTime.getTimeInMillis();
        timeView.setTime(time);
        dateView.setTime(time);
    }

    /**
     * Returns an Edit matching the currently set data
     */
    private Edit getEdit() {
        return new Edit(parentId, transactionId, transactionTime.getTimeInMillis(),
                descriptionView.getText().toString(), locationView.getText().toString(), valueEditText.getValue());
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
