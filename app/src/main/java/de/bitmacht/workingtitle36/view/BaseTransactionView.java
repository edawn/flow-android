package de.bitmacht.workingtitle36.view;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.widget.FrameLayout;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Value;

/**
 * A base implementation of a View showing a transaction
 */
abstract public class BaseTransactionView extends FrameLayout {

    private final ValueTextView valueView;
    private final DescriptionView descriptionView;
    private String valueText;
    private int valueTextLength;

    /**
     * Create a new instance
     * @param context The context
     * @param resource A layout resource that will be inflated and attached to this FrameLayout;
     *                 it should contain a {@link ValueTextView} with the id 'value' and
     *                 a {@link DescriptionView} with the id 'description'
     */
    public BaseTransactionView(Context context, @LayoutRes int resource) {
        super(context);

        inflate(context, resource, this);
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
        valueView.setWidth(width);
    }
}
