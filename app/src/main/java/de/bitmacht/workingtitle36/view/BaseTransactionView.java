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
import android.widget.RelativeLayout;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Value;

/**
 * A base implementation of a View showing a transaction
 */
abstract public class BaseTransactionView extends RelativeLayout {

    private ValueTextView valueView;
    private DescriptionView descriptionView;

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
    }

    /**
     * Set the Value to be shown
     */
    final void setValue(Value value) {
        valueView.setValue(value);
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
        return valueView.getText().length();
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
