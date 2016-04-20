package de.bitmacht.workingtitle36.view;

import android.content.Context;

import de.bitmacht.workingtitle36.Edit;
import de.bitmacht.workingtitle36.R;

/**
 * Gives a concise view of a Transaction.
 */
public class TransactionView extends BaseTransactionView {

    private final TimeView timeView;

    public TransactionView(Context context) {
        super(context, R.layout.transaction_view);

        timeView = (TimeView) findViewById(R.id.time);
    }

    /**
     * Set the Transaction to be shown.
     * @param edit The Edit that represents the Transaction to be displayed.
     */
    public void setData(Edit edit) {
        timeView.setTime(edit.getTtime());
        setValue(edit.getValue());
        setDescription(edit.getTdesc());
    }

    public void setTimeFormat(@TimeView.TimeFormat int timeFormatStyle) {
        timeView.setTimeFormat(timeFormatStyle);
    }
}
