package de.bitmacht.workingtitle36;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
        ValueModifyView.OnValueChangeListener, DatePickerFragment.OnDateSetListener, RegularsUpdateTask.UpdateFinishedCallback {

    private static final Logger logger = LoggerFactory.getLogger(RegularEditActivity.class);

    /**
     * An optional extra containing a RegularModel that will be edited
     */
    public static final String EXTRA_REGULAR = "regular";

    private static final String STATE_VALUE_KEY = "value";
    private static final String STATE_IS_LAST_INDEFINITE_KEY = "isLastIndefinite";

    private Long regularId = null;
    private Calendar timeFirst = new GregorianCalendar();
    private Calendar timeLast = null;
    private boolean isLastIndefinite = true;
    private Value value;

    private Toolbar toolbar;
    private ImageButton cancelButton;
    private ImageButton acceptButton;
    private SwitchCompat enabledSwitch;
    private TimeView dateFirstView;
    private Button dateLastIndefButton;
    private LinearLayout dateLastContainer;
    private ImageButton dateLastClearButton;
    private TimeView dateLastView;
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
        dateFirstView = (TimeView) findViewById(R.id.date_first);
        dateLastIndefButton = (Button) findViewById(R.id.date_last_indef_button);
        dateLastContainer = (LinearLayout) findViewById(R.id.date_last_container);
        dateLastClearButton = (ImageButton) findViewById(R.id.date_last_clear_button);
        dateLastView = (TimeView) findViewById(R.id.date_last);
        valueWidget = (ValueWidget) findViewById(R.id.value);
        repetitionSpinner = (Spinner) findViewById(R.id.repetition);
        descriptionView = (EditText) findViewById(R.id.description);

        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        cancelButton.setOnClickListener(this);
        acceptButton.setOnClickListener(this);

        dateFirstView.setOnClickListener(this);

        dateLastIndefButton.setOnClickListener(this);
        dateLastClearButton.setOnClickListener(this);
        dateLastView.setOnClickListener(this);

        ArrayAdapter<CharSequence> intervalAdapter = ArrayAdapter.createFromResource(this, R.array.interval_names, android.R.layout.simple_spinner_item);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repetitionSpinner.setAdapter(intervalAdapter);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_REGULAR)) {
                // edit an existing regular
                RegularModel regular = intent.getParcelableExtra(EXTRA_REGULAR);
                regularId = regular.id;
                value = regular.getValue();
                enabledSwitch.setChecked(!regular.isDisabled);
                timeFirst.setTimeInMillis(regular.timeFirst);
                if (regular.timeLast < 0) {
                    isLastIndefinite = true;
                } else {
                    isLastIndefinite = false;
                    timeLast = new GregorianCalendar();
                    timeLast.setTimeInMillis(regular.timeLast);
                }

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
                value = new Value(MyApplication.getCurrency().getCurrencyCode(), 0);
            }
        } else {
            regularId = savedInstanceState.containsKey(DBHelper.REGULARS_KEY_ID) ?
                    savedInstanceState.getLong(DBHelper.REGULARS_KEY_ID) : null;
            timeFirst.setTimeInMillis(savedInstanceState.getLong(DBHelper.REGULARS_KEY_TIME_FIRST));
            isLastIndefinite = savedInstanceState.getBoolean(STATE_IS_LAST_INDEFINITE_KEY);
            if (savedInstanceState.containsKey(DBHelper.REGULARS_KEY_TIME_LAST)) {
                timeLast = new GregorianCalendar();
                timeLast.setTimeInMillis(savedInstanceState.getLong(DBHelper.REGULARS_KEY_TIME_LAST));
            }
            value = savedInstanceState.getParcelable(STATE_VALUE_KEY);
        }

        valueWidget.setValue(value);
        updateTimeViews();
        updateLastTimeInput();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (regularId != null) {
            outState.putLong(DBHelper.REGULARS_KEY_ID, regularId);
        }
        outState.putLong(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst.getTimeInMillis());
        outState.putBoolean(STATE_IS_LAST_INDEFINITE_KEY, isLastIndefinite);
        if (timeLast != null) {
            outState.putLong(DBHelper.REGULARS_KEY_TIME_LAST, timeLast.getTimeInMillis());
        }
        outState.putParcelable(STATE_VALUE_KEY, value);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                RegularsUpdateTask rut = new RegularsUpdateTask(this, this, getRegular());
                rut.execute();
            } else {
                finish();
            }
        } else if (id == R.id.date_first || id == R.id.date_last) {
            DialogFragment frag = new DatePickerFragment();
            Bundle bundle = new Bundle();
            bundle.putInt(DatePickerFragment.ARG_ID, id);
            Calendar time;
            if (id == R.id.date_first) {
                time = timeFirst;
            } else {
                time = timeLast;
                bundle.putLong(DatePickerFragment.ARG_MIN_DATE, timeFirst.getTimeInMillis());
            }
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, time.getTimeInMillis());
            frag.setArguments(bundle);
            frag.show(getFragmentManager(), "datePicker");
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        } else if (id == R.id.date_last_indef_button) {
            isLastIndefinite = false;
            updateLastTimeInput();
        } else if (id == R.id.date_last_clear_button) {
            isLastIndefinite = true;
            updateLastTimeInput();
        }
    }

    @Override
    public void onValueChange(Value difference) {
        if (BuildConfig.DEBUG) {
            logger.trace("value change: {}", difference);
        }

        try {
            this.value = this.value.add(difference);
            valueWidget.setValue(this.value);
        } catch (Value.CurrencyMismatchException e) {
            if (BuildConfig.DEBUG) {
                logger.warn("Unable to change amount", e);
            }
        }
    }

    @Override
    public void onDateSet(int id, int year, int monthOfYear, int dayOfMonth) {
        Calendar dst = id == R.id.date_first ? timeFirst : timeLast;
        dst.set(Calendar.YEAR, year);
        dst.set(Calendar.MONTH, monthOfYear);
        dst.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateTimeViews();
    }

    private void updateTimeViews() {
        dateFirstView.setTime(timeFirst.getTimeInMillis());
        if (timeLast != null) {
            if (!timeFirst.before(timeLast)) {
                timeLast.setTimeInMillis(timeFirst.getTimeInMillis() + 1);
            }
            dateLastView.setTime(timeLast.getTimeInMillis());
        }
    }

    private void updateLastTimeInput() {
        if (isLastIndefinite) {
            dateLastIndefButton.setVisibility(View.VISIBLE);
            dateLastContainer.setVisibility(View.GONE);
        } else {
            dateLastIndefButton.setVisibility(View.GONE);
            dateLastContainer.setVisibility(View.VISIBLE);
            if (timeLast == null) {
                timeLast = new GregorianCalendar();
                if (!timeFirst.before(timeLast)) {
                    timeLast.setTimeInMillis(timeFirst.getTimeInMillis() + 1);
                }
            }
            dateLastView.setTime(timeLast.getTimeInMillis());
        }
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

        RegularModel regular = new RegularModel(timeFirst.getTimeInMillis(),
                isLastIndefinite || timeLast == null ? -1 : timeLast.getTimeInMillis(),
                periodType, periodMultiplier, false, !enabledSwitch.isChecked(),
                cv.amount, cv.currencyCode, descriptionView.getText().toString());
        regular.id = regularId;
        return regular;
    }

    @Override
    public void onUpdateFinished(boolean success) {
        if (success) {
            setResult(RESULT_OK);
        }
        finish();
    }
}
