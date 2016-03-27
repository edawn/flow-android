package de.bitmacht.workingtitle36.view;

import android.content.Context;
import android.widget.FrameLayout;

import java.util.Currency;

import de.bitmacht.workingtitle36.Edit;
import de.bitmacht.workingtitle36.R;

/**
 * Gives a concise view of a Transaction.
 */
public class TransactionView extends FrameLayout {

    private final TimeView timeView;
    private final ValueTextView valueView;
    private final DescriptionView descriptionView;
    private String valueText;
    private int valueTextLength;

    public TransactionView(Context context) {
        super(context);

        inflate(getContext(), R.layout.transaction_view, this);
        timeView = (TimeView) findViewById(R.id.time);
        valueView = (ValueTextView) findViewById(R.id.value);
        descriptionView = (DescriptionView) findViewById(R.id.description);

        valueTextLength = valueView.getText().length();
    }

    /**
     * Set the Transaction to be shown.
     * @param edit The Edit that represents the Transaction to be displayed.
     */
    public void setData(Edit edit) {
        timeView.setTime(edit.getTtime());
        Currency currency = Currency.getInstance(edit.getTcurrency());
        valueText = valueView.setValue(currency, edit.getTamount());
        valueTextLength = valueText.length();
        descriptionView.setDescription(edit.getTdesc());
    }

    /**
     * Return the length of the text in the ValueView
     */
    public int getValueTextLength() {
        return valueTextLength;
    }

    /**
     * Calculates the width of the value text
     * @return The width of the value text in pixels
     */
    public int getValueTextWidth() {
        valueView.measure(0, 0);
        return valueView.getMeasuredWidth();
    }

    public void setValueViewWidth(int width) {
        valueView.setWidth(width);
    }
}
