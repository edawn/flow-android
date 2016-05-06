package de.bitmacht.workingtitle36;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Currency;
import java.util.Locale;

/**
 * Combines an amount and a ISO 4217 currency code
 */
public class Value implements Parcelable {
    /**
     * The ISO 4217 currency code associated with this object
     */
    public final String currencyCode;
    /**
     * The number of minor currency units represented by this object
     */
    public final long amount;

    /**
     * Create a new instance of Value
     * @param amount The number of minor currency units
     * @param currencyCode The ISO 4217 currency code
     */
    public Value(@NonNull String currencyCode, long amount) {
        this.currencyCode = currencyCode;
        this.amount = amount;
    }

    /**
     * Returns a new Value having the currency of this amount, but with a different amount
     */
    public Value withAmount(long amount) {
        return new Value(this.currencyCode, amount);
    }

    /**
     * Returns the Currency associated with this object's currency code
     */
    public Currency getCurrency() {
        return Currency.getInstance(currencyCode);
    }

    /**
     * Returns a new Value with the sum of this and the other Value
     * @throws CurrencyMismatchException If the currencies differ
     */
    public Value add(Value other) throws CurrencyMismatchException {
        if (!currencyCode.equals(other.currencyCode)) {
            throw new CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode);
        }
        return new Value(currencyCode, amount + other.amount);
    }

    /**
     * Return a new Value with the sum of this and all Values from others
     * @throws CurrencyMismatchException If any currency code differs from this
     */
    public Value addAll(Value[] others) throws CurrencyMismatchException {
        long sumount = amount;
        for (Value other : others) {
            if (!currencyCode.equals(other.currencyCode)) {
                throw new CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode);
            }
            sumount += other.amount;
        }
        return new Value(currencyCode, sumount);
    }

    /**
     * Return a new Value with the sum of this and all Values from others
     * @throws CurrencyMismatchException If any currency code differs from this
     */
    public Value addAll(Collection<Value> others) throws CurrencyMismatchException {
        long sumount = amount;
        for (Value other : others) {
            if (!currencyCode.equals(other.currencyCode)) {
                throw new CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode);
            }
            sumount += other.amount;
        }
        return new Value(currencyCode, sumount);
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * Uses the default locale.
     * @return The generated String
     */
    public String getString() {
        return getString(Locale.getDefault());
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * @param locale The Locale to use for the decimal separator
     * @return The generated String
     */
    public String getString(Locale locale) {
        Pair<String, String> vs = getValueAndSymbolStrings(locale);
        return vs.first + vs.second;
    }

    public Pair<String, String> getValueAndSymbolStrings(Locale locale) {
        Currency currency = getCurrency();
        int fractionDigits = currency.getDefaultFractionDigits();
        String symbol = currency.getSymbol();
        char decimalSeparator = DecimalFormatSymbols.getInstance(locale).getMonetaryDecimalSeparator();

        String sign = amount < 0 ? "-" : "";
        long umount = Math.abs(amount);
        long major = umount;
        long minor = 0;
        boolean showFraction = fractionDigits > 0;
        if (showFraction) {
            long div = (long) Math.pow(10, fractionDigits);
            major = umount / div;
            minor = umount % div;
        }

        String valueString = showFraction ? String.format("%s%d%s%02d", sign, major, decimalSeparator, minor) :
                String.format("%s%d", sign, major);

        return new Pair<>(valueString, symbol);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(currencyCode);
        dest.writeLong(amount);
    }

    public static final Parcelable.Creator<Value> CREATOR =
            new Parcelable.Creator<Value>() {
                @Override
                public Value createFromParcel(Parcel source) {
                    return new Value(source);
                }

                @Override
                public Value[] newArray(int size) {
                    return new Value[size];
                }
            };

    private Value(Parcel in) {
        currencyCode = in.readString();
        amount = in.readLong();
    }

    public static class CurrencyMismatchException extends Exception {
        public CurrencyMismatchException(String s) {
            super(s);
        }
    }

    @Override
    public String toString() {
        return currencyCode + ":" + amount;
    }
}
