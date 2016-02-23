package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Currency;

public class ValueTextView extends TextView implements ValueWidget {

    public ValueTextView(Context context) {
        super(context);
    }

    public ValueTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValueTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public String setValue(Currency currency, long cents) {
        String valueText = ValueWidgetCommon.getValueString(currency, cents);
        setText(valueText);
        return valueText;
    }
}
