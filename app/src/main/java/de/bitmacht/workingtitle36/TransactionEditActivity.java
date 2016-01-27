package de.bitmacht.workingtitle36;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.Locale;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.ValueModifyView;
import de.bitmacht.workingtitle36.view.ValueView;

public class TransactionEditActivity extends AppCompatActivity implements View.OnClickListener, ValueModifyView.OnValueChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEditActivity.class);

    private Currency currency;
    private long value;

    private TimeView timeView;
    private TimeView dateView;
    private ValueView valueView;
    private ValueModifyView valueModMoreView;
    private ValueModifyView valueModLessView;
    private EditText descriptionView;

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

        timeView.setTime(System.currentTimeMillis());
        timeView.setOnClickListener(this);

        dateView.setTime(System.currentTimeMillis());
        dateView.setOnClickListener(this);

        valueView.setValue(currency, value);

        valueModLessView.setValue(currency, 10);
        valueModMoreView.setOnValueChangeListener(this);

        valueModMoreView.setValue(currency, 100);
        valueModLessView.setOnValueChangeListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.time) {
            new TimePickerFragment().show(getFragmentManager(), "timePicker");
        } else if (id == R.id.date) {
            new DatePickerFragment().show(getFragmentManager(), "datePicker");
        }
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
}
