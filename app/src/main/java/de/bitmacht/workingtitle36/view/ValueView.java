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
import java.util.Locale;

import de.bitmacht.workingtitle36.BuildConfig;

public class ValueView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger(ValueView.class);

    public ValueView(Context context) {
        super(context);
    }

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValueView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Sets the value that will be displayed
     * @param currencyCode The ISO 4217 currency code
     * @param cents The value in minor currency units
     * @return The text that will be displayed
     */
    public String setValue(String currencyCode, long cents) {
        String valueText = "?";
        try {
            Currency currency = Currency.getInstance(currencyCode);
            valueText = getValueString(currency, cents);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.warn("oops", e);
            }
        }

        setText(valueText);
        return valueText;
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * Uses the default locale.
     * @param currency The currency
     * @param cents The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     * @return The generated String
     */
    public static String getValueString(Currency currency, long cents) {
        return getValueString(Locale.getDefault(), currency, cents);
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * @param locale The Locale to use for the decimal separator
     * @param currency The currency
     * @param cents The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     * @return The generated String
     */
    public static String getValueString(Locale locale, Currency currency, long cents) {
        int fractionDigits = currency.getDefaultFractionDigits();
        String symbol = currency.getSymbol();
        char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getMonetaryDecimalSeparator();

        String sign = cents < 0 ? "-" : "";
        cents = Math.abs(cents);
        long major = cents;
        long minor = 0;
        boolean showFraction = fractionDigits > 0;
        if (showFraction) {
            long div = (long) Math.pow(10, fractionDigits);
            major = cents / div;
            minor = cents % div;
        }

        String valueString = showFraction ? String.format("%s%d%s%02d%s", sign, major, decimalSeparator, minor, symbol) :
                String.format("%s%d%s", sign, major, symbol);

        return valueString;
    }
}
