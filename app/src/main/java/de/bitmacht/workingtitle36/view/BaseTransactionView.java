package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Value;

/**
 * A base implementation of a View showing a transaction
 */
abstract public class BaseTransactionView extends RelativeLayout {

    private ValueTextView valueView;
    private DescriptionView descriptionView;
    private String valueText;
    private int valueTextLength;

    public BaseTransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseTransactionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BaseTransactionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        valueView = (ValueTextView) findViewById(R.id.value);
        descriptionView = (DescriptionView) findViewById(R.id.description);

        valueTextLength = valueView.getText().length();
    }

    /**
     * Set the Value to be shown
     */
    final void setValue(Value value) {
        valueText = valueView.setValue(value);
        valueTextLength = valueText.length();
    }

    /**
     * Set the description to be shown
     */
    final void setDescription(String description) {
        descriptionView.setDescription(description);
    }

    /**
     * Return the length of the text in the ValueView
     */
    public final int getValueTextLength() {
        return valueTextLength;
    }

    /**
     * Calculates the width of the amount text
     * @return The width of the amount text in pixels
     */
    public final int getValueTextWidth() {
        valueView.measure(0, 0);
        return valueView.getMeasuredWidth();
    }

    public final void setValueViewWidth(int width) {
        valueView.setMinWidth(width);
    }
}
