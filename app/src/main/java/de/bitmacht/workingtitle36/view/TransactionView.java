package de.bitmacht.workingtitle36.view;

import android.content.Context;
import android.widget.FrameLayout;

import de.bitmacht.workingtitle36.Edit;
import de.bitmacht.workingtitle36.R;

/**
 * Gives a concise view of a Transaction.
 */
public class TransactionView extends FrameLayout {

    private final TimeView timeView;
    private final ValueView valueView;
    private final DescriptionView descriptionView;

    public TransactionView(Context context) {
        super(context);

        inflate(getContext(), R.layout.transaction_view, this);
        timeView = (TimeView) findViewById(R.id.time);
        valueView = (ValueView) findViewById(R.id.value);
        descriptionView = (DescriptionView) findViewById(R.id.description);
    }

    /**
     * Set the Transaction to be shown.
     * @param edit The Edit that represents the Transaction to be displayed.
     */
    public void setData(Edit edit) {
        timeView.setTime(edit.getTtime());
        valueView.setValue(edit.getTvalue(), edit.getTcurrency());
        descriptionView.setDescription(edit.getTdesc());
    }
}
