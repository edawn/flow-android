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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import de.bitmacht.workingtitle36.R;
import de.bitmacht.workingtitle36.Value;

public class ValueInput extends LinearLayout implements ValueWidget {

    private CheckBox signCheckbox;
    private ValueEditText valueEditText;

    public ValueInput(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public ValueInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public ValueInput(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ValueInput(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        LayoutInflater.from(context).inflate(R.layout.value_input, this, true);
        signCheckbox = (CheckBox) findViewById(R.id.sign_checkbox);
        valueEditText = (ValueEditText) findViewById(R.id.value_edit);
    }

    /**
     * Set the value
     * @param value The value
     * @return The textual representation of the absolute value (i.e. without a sign)
     */
    @NonNull
    @Override
    public String setValue(@NonNull Value value) {
        return setValue(value, false);
    }

    /**
     * Set the value
     * @param value The value
     * @param negativeZero Show a minus if the amount is zero
     * @return The textual representation of the absolute value (i.e. without a sign)
     */
    @NonNull
    public String setValue(@NonNull Value value, boolean negativeZero) {
        if (value.amount < 0) {
            value = value.withAmount(-value.amount);
            signCheckbox.setChecked(false);
        } else {
            signCheckbox.setChecked(!(value.amount == 0 && negativeZero));
        }
        return valueEditText.setValue(value);
    }

    @Nullable
    @Override
    public Value getValue() {
        Value value = valueEditText.getValue();
        if (!signCheckbox.isChecked()) {
            value = value.withAmount(-value.amount);
        }
        return value;
    }

    public void focusEditText() {
        valueEditText.requestFocus();
    }
}
