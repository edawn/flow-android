package de.bitmacht.workingtitle36;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueModifyView;
import de.bitmacht.workingtitle36.view.ValueWidget;

public class RegularEditActivity extends AppCompatActivity implements View.OnClickListener,
        ValueModifyView.OnValueChangeListener, DatePickerDialog.OnDateSetListener, RegularsUpdateTask.UpdateFinishedCallback {

    private static final Logger logger = LoggerFactory.getLogger(RegularEditActivity.class);

    /**
     * An optional extra containing a RegularModel that will be edited
     */
    public static final String EXTRA_REGULAR = "regular";

    private static final String STATE_VALUE_KEY = "value";

    private long creationTime;
    private Calendar timeFirst = new GregorianCalendar();
    private Value value;

    private Toolbar toolbar;
    private ImageButton cancelButton;
    private ImageButton acceptButton;
    private SwitchCompat enabledSwitch;
    private TimeView dateView;
    private ValueWidget valueWidget;
    private Spinner repetitionSpinner;
    private EditText descriptionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_regular_edit);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        cancelButton = (ImageButton) findViewById(R.id.cancel_button);
        acceptButton = (ImageButton) findViewById(R.id.accept_button);
        enabledSwitch = (SwitchCompat) findViewById(R.id.enabled);
        dateView = (TimeView) findViewById(R.id.date);
        valueWidget = (ValueWidget) findViewById(R.id.value);
        repetitionSpinner = (Spinner) findViewById(R.id.repetition);
        descriptionView = (EditText) findViewById(R.id.description);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        cancelButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);

        dateView.setOnClickListener(this);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(this, R.array.interval_names, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repetitionSpinner.setAdapter(intervalAdapter);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_REGULAR)) {
                // edit an existing regular
                RegularModel regular = intent.getParcelableExtra(EXTRA_REGULAR);
                creationTime = regular.creationTime;
                value = regular.getValue();
                enabledSwitch.setChecked(!regular.isDisabled);
                timeFirst.setTimeInMillis(regular.timeFirst);
                int pos = 0;
                if (regular.periodType == DBHelper.REGULARS_PERIOD_TYPE_DAILY) {
                    if (regular.periodMultiplier == DateTimeConstants.DAYS_PER_WEEK) {
                        pos = 1;
                    }
                } else {
                    if (regular.periodMultiplier == 1) {
                        pos = 2;
                    } else if (regular.periodMultiplier == 12) {
                        pos = 3;
                    }
                }
                repetitionSpinner.setSelection(pos);
                descriptionView.setText(regular.description);
            } else {
                creationTime = System.currentTimeMillis();
                value = new Value(MyApplication.getCurrency().getCurrencyCode(), 0);
            }
        } else {
            creationTime = savedInstanceState.getLong(DBHelper.REGULARS_KEY_CREATION_TIME);
            timeFirst.setTimeInMillis(savedInstanceState.getLong(DBHelper.REGULARS_KEY_TIME_FIRST));
            value = savedInstanceState.getParcelable(STATE_VALUE_KEY);
        }

        valueWidget.setValue(value);
        updateTimeViews();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DBHelper.REGULARS_KEY_CREATION_TIME, creationTime);
        outState.putLong(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst.getTimeInMillis());
        outState.putParcelable(STATE_VALUE_KEY, value);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                RegularsUpdateTask rut = new RegularsUpdateTask(this, this);
                rut.execute(getRegular());
            } else {
                finish();
            }
        } else if (id == R.id.date) {
            DialogFragment frag = new DatePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, timeFirst.getTimeInMillis());
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), "datePicker");

            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    @Override
    public void onValueChange(Value value) {
        if (BuildConfig.DEBUG) {
            logger.trace("value change: {}", value);
        }

        try {
            this.value = this.value.add(value);
            valueWidget.setValue(this.value);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("Unable to change amount", e);
            }
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        timeFirst.set(Calendar.YEAR, year);
        timeFirst.set(Calendar.MONTH, monthOfYear);
        timeFirst.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateTimeViews();
    }

    private void updateTimeViews() {
        long time = timeFirst.getTimeInMillis();
        dateView.setTime(time);
    }

    private RegularModel getRegular() {
        int spinnerPos = repetitionSpinner.getSelectedItemPosition();
        int periodType = DBHelper.REGULARS_PERIOD_TYPE_DAILY;
        int periodMultiplier = 1;
        switch(spinnerPos) { // case 0 is intrinsic
            case 1: // weekly
                periodMultiplier = DateTimeConstants.DAYS_PER_WEEK;
                break;
            case 3: // yearly
                periodMultiplier = 12;
            case 2: // monthly
                periodType = DBHelper.REGULARS_PERIOD_TYPE_MONTHLY;
                break;
        }

        Value cv = valueWidget.getValue();
        if (BuildConfig.DEBUG) {
            logger.trace("value: {}", cv);
        }

        return new RegularModel(creationTime, dateView.getTime(), periodType, periodMultiplier,
                false, !enabledSwitch.isChecked(), false, cv.amount, cv.currencyCode, descriptionView.getText().toString());
    }

    @Override
    public void onUpdateFinished(boolean success) {
        if (success) {
            setResult(RESULT_OK);
        }
        finish();
    }
}
