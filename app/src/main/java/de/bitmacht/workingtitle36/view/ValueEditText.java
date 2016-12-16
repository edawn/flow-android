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
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.MyApplication;
import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Utils;
import de.bitmacht.workingtitle36.Value;

public class ValueEditText extends AppCompatEditText implements ValueWidget {

    private static final Logger logger = LoggerFactory.getLogger(ValueEditText.class);

    // The maximum length of the absolute value of a decimal number that will fit into a long
    public static final int MAX_DEC_LEN = 18;

    // utilize cash register like input style
    private boolean registerInputStyle = false;
    private Currency currency;
    private ValueTextWatcher textWatcher;

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
        registerInputStyle = Utils.getbPref(getContext(), R.string.pref_register_key, registerInputStyle);
        updateCurrency(MyApplication.getCurrency());
        setValue(new Value(currency.getCurrencyCode(), 0));
        setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private String extractValueString(String rawValueString) {
        int fractionDigits = currency.getDefaultFractionDigits();
        rawValueString = rawValueString.replaceAll("[^0-9.,]+","");

        char separator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
        String[] splits = rawValueString.split("[" + separator +"]", 2);

        String major = splits[0].replaceAll("[^0-9]+", "");
        if (major.length() == 0) {
            major = "0";
        }

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

        return major + minor;
    }

    @Override
    @NonNull
    public Value getValue() {
        long amount = Long.parseLong(extractValueString(getText().toString()));
        return new Value(currency.getCurrencyCode(), amount);
    }

    @Override
    @NonNull
    public String setValue(@NonNull Value value) {
        if (!value.currencyCode.equals(currency.getCurrencyCode())) {
            updateCurrency(value.getCurrency());
        }
        Pair<String, String> vs = value.getValueAndSymbolStrings(Locale.getDefault());
        String valueText = vs.first + vs.second;
        setText(valueText);
        return valueText;
    }

    private void updateCurrency(@NonNull Currency currency) {
        this.currency = currency;

        removeTextChangedListener(textWatcher);
        if (registerInputStyle) {
            setFilters(new InputFilter[]{});
            textWatcher = new ValueTextWatcher(currency);
            addTextChangedListener(textWatcher);
        } else {
            setFilters(new InputFilter[]{new ValueInputFilter(currency)});
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (textWatcher != null && textWatcher.isModfiying) {
            return;
        }
        Editable text = getEditableText();
        if (text != null && text.length() != 0 && currency != null) {
            int vl = text.length() - currency.getSymbol().length();
            int newStart = Math.min(selStart, vl);
            int newEnd = Math.min(selEnd, vl);

            // shift the cursor one to the left, if it should lie right of the decimal separator
            //XXX breaks shifting the cursor with the right arrow key
            if (registerInputStyle && newStart == newEnd && newEnd != 0 &&
                    !Character.isDigit(text.charAt(newEnd - 1))) {
                newStart--;
                newEnd--;
            }

            Selection.setSelection(text, newStart, newEnd);
        }
    }

    private class ValueInputFilter implements InputFilter {

        private final char separator;
        private final String separatorString;
        private final int fracts;
        private final String symbol;
        private final Pattern pattern;
        private final Pattern separatorReplacePattern;

        ValueInputFilter(Currency currency) {
            separator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
            separatorString = Character.toString(separator);
            fracts = currency.getDefaultFractionDigits();
            symbol = currency.getSymbol();
            if (fracts > 0) {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)?(?:\\Q" + separator + "\\E[0-9]{0," + fracts + "})?\\Q" + symbol + "\\E");
            } else {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)?\\Q" + symbol + "\\E");
            }
            separatorReplacePattern = Pattern.compile("[,.]");
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            String sourceMod = separatorReplacePattern.matcher(source).replaceAll(separatorString);
            String result = dest.subSequence(0, dstart) + sourceMod + dest.subSequence(dend, dest.length());

            if (!pattern.matcher(result).matches()) {
                return dest.subSequence(dstart, dend);
            }
            if (extractValueString(result).length() > MAX_DEC_LEN) {
                return "";
            }
            return sourceMod;
        }
    }

    private class ValueTextWatcher implements TextWatcher {
        private final char separator;
        private final String separatorString;
        private final int fracts;
        private final String symbol;
        private final Pattern pattern;
        private boolean isModfiying = false;

        ValueTextWatcher(Currency currency) {
            separator = DecimalFormatSymbols.getInstance().getMonetaryDecimalSeparator();
            separatorString = Character.toString(separator);
            fracts = currency.getDefaultFractionDigits();
            symbol = currency.getSymbol();
            if (fracts > 0) {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)\\Q" + separator + "\\E[0-9]{" + fracts + "}\\Q" + symbol + "\\E");
            } else {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)\\Q" + symbol + "\\E");
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (BuildConfig.DEBUG) {
                logger.trace("{} start: {} count: {} after: {}", s, start, count, after);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (BuildConfig.DEBUG) {
                logger.trace("{} start: {} before: {} count: {}", s, start, before, count);
            }
}

        @Override
        public void afterTextChanged(Editable s) {
            if (isModfiying) {
                return;
            }
            isModfiying = true;
            try {
                boolean isValid = pattern.matcher(s).matches();
                isValid = isValid && extractValueString(s.toString()).length() <= MAX_DEC_LEN;

                if (BuildConfig.DEBUG) {
                    logger.trace("{} isValid: {}", s, isValid);
                }

                if (isValid) {
                    return;
                }

                int i = 0;
                while (i < s.length()) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("i: {} s: {}", i, s);
                    }
                    if (!Character.isDigit(s.charAt(i))) {
                        s.delete(i, i + 1);
                    } else {
                        i++;
                    }
                }

                if (BuildConfig.DEBUG) {
                    logger.trace("plain: {}", s);
                }

                // remove excess leading zeros
                while (s.length() > fracts + 1 && s.charAt(0) == '0') {
                    s.delete(0, 1);
                }

                // fill with zeros up to one before the decimal point
                while (s.length() < fracts + 1) {
                    s.insert(0, "0");
                }

                while (s.length() > MAX_DEC_LEN) {
                    s.delete(s.length() - 1, s.length());
                }

                // add decimal point
                if (fracts > 0) {
                    s.insert(s.length() - fracts, separatorString);
                }

                // add the currency symbol
                s.append(symbol);

                if (BuildConfig.DEBUG) {
                    logger.trace("revalidated: {}", s);
                }
            } finally {
                isModfiying = false;
                if (BuildConfig.DEBUG) {
                    logger.trace("final");
                }
            }
        }
    }
}
