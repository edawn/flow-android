package de.bitmacht.workingtitle36.view;

import android.util.Pair;

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

class ValueWidgetCommon {
    /**
     * Builds an appropriate String for a given currency and amount.
     * Uses the default locale.
     * @param currency The currency
     * @param amount The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     * @return The generated String
     */
    public static String getValueString(Currency currency, long amount) {
        return getValueString(Locale.getDefault(), currency, amount);
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * @param locale The Locale to use for the decimal separator
     * @param currency The currency
     * @param amount The amount in minor currency units or in major currency units if the
     *              currency does not have minor units
     * @return The generated String
     */
    public static String getValueString(Locale locale, Currency currency, long amount) {
        Pair<String, String> vs = getValueAndSymbolStrings(locale, currency, amount);
        return vs.first + vs.second;
    }

    public static Pair<String, String> getValueAndSymbolStrings(Locale locale, Currency currency, long amount) {
        int fractionDigits = currency.getDefaultFractionDigits();
        String symbol = currency.getSymbol();
        char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getMonetaryDecimalSeparator();

        String sign = amount < 0 ? "-" : "";
        amount = Math.abs(amount);
        long major = amount;
        long minor = 0;
        boolean showFraction = fractionDigits > 0;
        if (showFraction) {
            long div = (long) Math.pow(10, fractionDigits);
            major = amount / div;
            minor = amount % div;
        }

        String valueString = showFraction ? String.format("%s%d%s%02d", sign, major, decimalSeparator, minor) :
                String.format("%s%d", sign, major);

        return new Pair<>(valueString, symbol);
    }

}
