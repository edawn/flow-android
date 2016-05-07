package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import de.bitmacht.workingtitle36.Edit;
import de.bitmacht.workingtitle36.R;

/**
 * Gives a concise view of a Transaction.
 */
public class TransactionView extends BaseTransactionView {

    private TimeView timeView;

    public TransactionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TransactionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TransactionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
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
