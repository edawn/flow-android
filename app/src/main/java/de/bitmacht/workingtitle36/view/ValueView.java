package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormatSymbols;
import java.util.Currency;

import de.bitmacht.workingtitle36.BuildConfig;

public class ValueView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger(ValueView.class);

    private char decimalSeparator;

    public ValueView(Context context) {
        super(context);
        init();
    }

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ValueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        decimalSeparator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
    }

    /**
     * Sets the value that will be displayed
     * @param cents The value in minor currency units
     * @param currencyCode The ISO 4217 currency code
     * @return The text that will be displayed
     */
    public String setValue(long cents, String currencyCode) {
        int fractionDigits = 0;
        String symbol = "?";
        try {
            Currency currency = Currency.getInstance(currencyCode);
            fractionDigits = currency.getDefaultFractionDigits();
            symbol = currency.getSymbol();
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.warn("oops", e);
            }
        }

        long major = cents;
        long minor = 0;
        boolean showFraction = fractionDigits > 0;
        if (showFraction) {
            long div = (long) Math.pow(10, fractionDigits);
            major = cents / div;
            minor = Math.abs(cents % div);
        }

        String text = showFraction ? String.format("%d%s%02d%s", major, decimalSeparator, minor, symbol) :
                String.format("%d%s", major, symbol);

        setText(text);
        return text;
    }
}
