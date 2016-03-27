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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueModifyView;
import de.bitmacht.workingtitle36.view.ValueWidget;

public class RegularEditActivity extends AppCompatActivity implements View.OnClickListener,
        ValueModifyView.OnValueChangeListener, DatePickerDialog.OnDateSetListener {

    private static final Logger logger = LoggerFactory.getLogger(RegularEditActivity.class);

    private Currency currency;
    private long amount;

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

        currency = Currency.getInstance(Locale.getDefault());
        amount = 0;

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

        valueWidget.setValue(currency, amount);

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
    public void onValueChange(Currency currency, int amount) {
        if (BuildConfig.DEBUG) {
            logger.trace("value change: {},{}", currency, amount);
        }

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

    private void updateTimeViews() {
        long time = calendar.getTimeInMillis();
        dateView.setTime(time);
    }
}
