package de.bitmacht.workingtitle36.view;

import android.content.Context;
import android.widget.TextView;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.RegularModel;

public class RegularView extends BaseTransactionView {

    private final TextView periodView;

    public RegularView(Context context) {
        super(context, R.layout.regular_view);

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
