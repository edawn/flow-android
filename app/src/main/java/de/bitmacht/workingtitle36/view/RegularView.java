package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.RegularModel;

public class RegularView extends BaseTransactionView {

    private TextView periodView;

    public RegularView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegularView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RegularView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        periodView = (TextView) findViewById(R.id.period);
    }

    public void setData(RegularModel regularModel) {
        setValue(regularModel.getValue());
        periodView.setText(getPeriodText(regularModel));
        setDescription(regularModel.description);
    }

    private CharSequence getPeriodText(RegularModel regularModel) {
        return String.valueOf(regularModel.periodType) + ":" + String.valueOf(regularModel.periodMultiplier);
    }
}
