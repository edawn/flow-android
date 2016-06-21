/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitmacht.workingtitle36.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.MyApplication;
import de.bitmacht.workingtitle36.Value;

public class ValueEditText extends AppCompatEditText implements ValueWidget {

    private static final Logger logger = LoggerFactory.getLogger(ValueEditText.class);

    private String currencyCode;
    String currencySymbol = "";

    private ValueInputFilter inf;

    public ValueEditText(Context context) {
        super(context);
        init();
    }

    public ValueEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ValueEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Currency currency = MyApplication.getCurrency();
        currencyCode = currency.getCurrencyCode();
        currencySymbol = currency.getSymbol();
        setText(currencySymbol);
        setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        inf = new ValueInputFilter(currency);
        setFilters(new InputFilter[]{inf});
    }

    @Override
    @NonNull
    public Value getValue() {
        String text = getText().toString();

        Currency currency = Currency.getInstance(currencyCode);
        int fractionDigits = currency.getDefaultFractionDigits();
        text = text.replaceAll("[^0-9.,+-]+","");

        char separator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
        String[] splits = text.split("[" + separator +"]", 2);

        String major = splits[0].replaceAll("[^0-9+-]+", "");
        String minor = splits.length == 2 ? splits[1].replaceAll("[^0-9]+","") : "";

        if (fractionDigits == 0) {
            minor = "";
        } else {
            if (0 < fractionDigits) {
                if (fractionDigits < minor.length()) {
                    minor = minor.substring(0, fractionDigits);
                }
                while (minor.length() < fractionDigits) {
                    minor = minor + "0";
                }
            }
        }
        long amount = Long.parseLong(major + minor);
        return new Value(currencyCode, amount);
    }

    @Override
    @NonNull
    public String setValue(@NonNull Value value) {
        currencyCode = value.currencyCode;
        if (!inf.getCurrency().getCurrencyCode().equals(currencyCode)) {
            inf = new ValueInputFilter(Currency.getInstance(currencyCode));
            setFilters(new InputFilter[]{inf});
        }
        Pair<String, String> vs = value.getValueAndSymbolStrings(Locale.getDefault());
        String valueText = vs.first + vs.second;
        currencySymbol = vs.second;
        setText(valueText);
        return valueText;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        try {
            Editable text = getEditableText();
            if (text != null) {
                int tl = text.length();
                int sl = currencySymbol == null ? 0 : currencySymbol.length();
                int vl = tl - sl;
                int newStart = Math.min(selStart, vl);
                int newEnd = Math.min(selEnd, vl);

                Selection.setSelection(text, newStart, newEnd);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.trace("oops", e);
            }
        }
        super.onSelectionChanged(selStart, selEnd);
    }

    private class ValueInputFilter implements InputFilter {

        private final Currency currency;
        private final char separator;
        private final int fracts;
        private final String symbol;
        private final Pattern pattern;

        public ValueInputFilter(Currency currency) {
            this.currency = currency;
            separator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
            fracts = currency.getDefaultFractionDigits();
            symbol = currency.getSymbol();
            if (fracts > 0) {
                pattern = Pattern.compile("-?(?:0|[1-9]+[0-9]*)?(?:\\Q" + separator + "\\E[0-9]{0," + fracts + "})?\\Q" + symbol + "\\E");
            } else {
                pattern = Pattern.compile("-?(?:0|[1-9]+[0-9]*)?\\Q" + symbol + "\\E");
            }
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String result = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());
            Matcher matcher = pattern.matcher(result);
            if (!matcher.matches()) {
                return dest.subSequence(dstart, dend);
            }
            return null;
        }

        public Currency getCurrency() {
            return currency;
        }
    }
}
