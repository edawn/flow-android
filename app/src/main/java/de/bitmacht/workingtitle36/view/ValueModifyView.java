package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.R;

public class ValueModifyView extends LinearLayout implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(ValueModifyView.class);

    private TextView textView;

    private OnValueChangeListener valueChangeListener;
    private Currency currency;
    private int value;

    public ValueModifyView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ValueModifyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ValueModifyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueModifyView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        inflate(context, R.layout.value_modify, this);
        findViewById(R.id.add).setOnClickListener(this);
        findViewById(R.id.subtract).setOnClickListener(this);
        textView = (TextView) findViewById(R.id.text);
    }

    /**
     * Sets the stepping amount for this View
     * @param currency The currency
     * @param cents The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     */
    public void setValue(Currency currency, int cents) {
        this.currency = currency;
        this.value = cents;
        String valueString = ValueView.getValueString(currency, value);
        if (BuildConfig.DEBUG) {
            logger.trace("Setting step: {}, {} -> {}", currency, cents, valueString);
        }
        textView.setText(valueString);
    }

    @Override
    public void onClick(View v) {
        if (currency == null || value == 0 || valueChangeListener == null) {
            return;
        }
        valueChangeListener.onValueChange(currency, v.getId() == R.id.add ? value : -value);
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.valueChangeListener = listener;
    }

    public interface OnValueChangeListener {
        /**
         * This will be called when the value changes
         * @param currency The currency of the value change
         * @param cents The value change in the minor currency units
         */
        void onValueChange(Currency currency, int cents);
    }
}
