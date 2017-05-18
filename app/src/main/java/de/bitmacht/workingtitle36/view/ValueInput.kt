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

package de.bitmacht.workingtitle36.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout

import de.bitmacht.workingtitle36.R
import de.bitmacht.workingtitle36.Value
import kotlinx.android.synthetic.main.value_input.view.*

class ValueInput : LinearLayout, ValueWidget {

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private lateinit var signCheckbox: CheckBox
    private lateinit var valueEditText: ValueEditText
    private var negativeZero = false

    var valueText: CharSequence = ""
        get() = valueEditText.valueText
        private set

    override var value: Value?
        get() {
            val outvalue = valueEditText.value
            return if (signCheckbox.isChecked) outvalue else outvalue!!.withAmount(-outvalue.amount)
        }
        set(value) {
            var outvalue = value
            if (value != null) {
                if (value.amount < 0) {
                    outvalue = value.withAmount(-value.amount)
                    signCheckbox.isChecked = false
                } else {
                    signCheckbox.isChecked = !(outvalue!!.amount == 0L && negativeZero)
                }
            }
            valueEditText.value = outvalue
        }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        LayoutInflater.from(context).inflate(R.layout.value_input, this, true)
        signCheckbox = sign_checkbox
        valueEditText = value_edit

        val a = context.obtainStyledAttributes(attrs, R.styleable.ValueInput)
        negativeZero = a.getBoolean(R.styleable.ValueInput_negativeZero, negativeZero)
        a.recycle()
    }

    fun focusEditText() {
        valueEditText.requestFocus()
    }
}
