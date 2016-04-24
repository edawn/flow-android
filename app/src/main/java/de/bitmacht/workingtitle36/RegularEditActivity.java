package de.bitmacht.workingtitle36;

import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;

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

    private Value value;

    private Toolbar toolbar;
    private ImageButton cancelButton;
    private ImageButton acceptButton;
    private Switch enabledSwitch;
    private TimeView dateView;
    private ValueWidget valueWidget;
    private Spinner repetitionSpinner;
    private EditText descriptionView;
    private Calendar calendar = new GregorianCalendar();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        value = new Value(0);

        setContentView(R.layout.activity_regular_edit);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        cancelButton = (ImageButton) findViewById(R.id.cancel_button);
        acceptButton = (ImageButton) findViewById(R.id.accept_button);

        cancelButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);

        enabledSwitch = (Switch) findViewById(R.id.enabled);
        dateView = (TimeView) findViewById(R.id.date);
        valueWidget = (ValueWidget) findViewById(R.id.value);
        repetitionSpinner = (Spinner) findViewById(R.id.repetition);
        descriptionView = (EditText) findViewById(R.id.description);

        if (savedInstanceState != null && savedInstanceState.containsKey(DBHelper.REGULARS_KEY_CREATION_TIME)) {
            calendar.setTimeInMillis(savedInstanceState.getLong(DBHelper.REGULARS_KEY_CREATION_TIME));
        }
        updateTimeViews();
        dateView.setOnClickListener(this);

        valueWidget.setValue(value);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(this, R.array.interval_names, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repetitionSpinner.setAdapter(intervalAdapter);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DBHelper.REGULARS_KEY_CREATION_TIME, calendar.getTimeInMillis());
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
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, calendar.getTimeInMillis());
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
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateTimeViews();
    }

    private void updateTimeViews() {
        long time = calendar.getTimeInMillis();
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

        return new RegularModel(System.currentTimeMillis(), dateView.getTime(), periodType, periodMultiplier,
                false, !enabledSwitch.isChecked(), false, cv.amount, cv.currencyCode, descriptionView.getText().toString());
    }

    @Override
    public void onUpdateFinished(boolean success) {
        if (BuildConfig.DEBUG) {
            logger.trace("update finished");
        }

    }
}
