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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueInput;
import de.bitmacht.workingtitle36.view.ValueWidget;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener,
        TimePickerDialog.OnTimeSetListener, DatePickerFragment.OnDateSetListener {

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
    private ValueWidget valueWidget;
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
        valueWidget = (ValueWidget) findViewById(R.id.value);
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

        if (valueWidget instanceof ValueInput) {
            ((ValueInput) valueWidget).setValue(value, true);
        }
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
        outState.putParcelable(STATE_VALUE_KEY, valueWidget.getValue());
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
                new TransactionsUpdateTask(this, edit).execute();
                //TODO wait for the update to finish
                finish();
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
                descriptionView.getText().toString(), locationView.getText().toString(), valueWidget.getValue());
    }
}
