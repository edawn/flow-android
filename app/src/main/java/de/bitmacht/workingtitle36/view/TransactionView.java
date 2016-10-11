/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        timeView.setTime(edit.transactionTime);
        setValue(edit.getValue());
        setDescription(edit.transactionDescription);
    }
}
