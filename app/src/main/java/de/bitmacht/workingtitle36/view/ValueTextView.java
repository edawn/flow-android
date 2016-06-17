package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

import de.bitmacht.workingtitle36.Value;

public class ValueTextView extends TextView implements ValueWidget {

    private Value value = null;

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

    @Override
    @NonNull
    public String setValue(@NonNull Value value) {
        this.value = value;
        String valueText = value.getString();
        setText(valueText);
        return valueText;
    }

    @Override
    @Nullable
    public Value getValue() {
        return value;
    }
}
