package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.EditText;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.Locale;

import de.bitmacht.workingtitle36.BuildConfig;

public class ValueEditText extends EditText implements ValueWidget {

    private static final Logger logger = LoggerFactory.getLogger(ValueEditText.class);

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        inf = new ValueInputFilter();
        setFilters(new InputFilter[]{inf});
    }

    public String setValue(Currency currency, long cents) {
        Pair<String, String> vs = ValueWidgetCommon.getValueAndSymbolStrings(Locale.getDefault(), currency, cents);
        String valueText = vs.first + vs.second;
        currencySymbol = vs.second;
        inf.setPaused(true);
        setText(valueText);
        inf.setPaused(false);
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

        DigitsKeyListener digitsFilter = DigitsKeyListener.getInstance(true, true);
        private boolean isPaused = false;

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (isPaused) {
                return source;
            }

            CharSequence dfResult = digitsFilter.filter(source, start, end, dest, dstart, dend);

            return dfResult;
        }

        //TODO replace with a real filter
        public void setPaused(boolean isPaused) {
            this.isPaused = isPaused;
        }
    }
}
