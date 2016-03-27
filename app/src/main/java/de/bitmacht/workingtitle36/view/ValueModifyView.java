package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.R;

public class ValueModifyView extends LinearLayout implements View.OnTouchListener {

    private static final Logger logger = LoggerFactory.getLogger(ValueModifyView.class);

    private static final int[] DELAYS = {300, 200, 100, 50};

    private TextView textView;

    private OnValueChangeListener valueChangeListener;
    private Currency currency;
    private int amount;

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
        findViewById(R.id.add).setOnTouchListener(this);
        findViewById(R.id.subtract).setOnTouchListener(this);
        textView = (TextView) findViewById(R.id.text);
    }

    /**
     * Sets the stepping amount for this View
     * @param currency The currency
     * @param amount The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     */
    public void setValue(Currency currency, int amount) {
        this.currency = currency;
        this.amount = amount;
        String valueString = ValueWidgetCommon.getValueString(currency, this.amount);
        if (BuildConfig.DEBUG) {
            logger.trace("Setting step: {}, {} -> {}", currency, amount, valueString);
        }
        textView.setText(valueString);
    }

    private void changeAmount(int amount) {
        if (currency == null || amount == 0 || valueChangeListener == null) {
            return;
        }
        valueChangeListener.onValueChange(currency, amount);
    }

    public void setOnValueChangeListener(OnValueChangeListener listener) {
        this.valueChangeListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.sendMessage(handler.obtainMessage(v.getId(), 0, 0, null));
                break;
            case MotionEvent.ACTION_UP:
                handler.removeMessages(v.getId());
                break;
        }

        return false;
    }

    public interface OnValueChangeListener {
        /**
         * This will be called when the amount changes
         * @param currency The currency of the amount change
         * @param amount The amount change in the minor currency units
         */
        void onValueChange(Currency currency, int amount);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == R.id.add) {
                changeAmount(amount);
            } else if (msg.what == R.id.subtract) {
                changeAmount(-amount);
            } else {
                // this should never happen
                return;
            }
            int di = Math.min(msg.arg1, DELAYS.length - 1);
            sendMessageDelayed(obtainMessage(msg.what, di + 1, 0, null), DELAYS[di]);
        }
    };
}
