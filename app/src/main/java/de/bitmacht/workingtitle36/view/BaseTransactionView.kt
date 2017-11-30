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
import android.widget.RelativeLayout

import de.bitmacht.workingtitle36.R
import de.bitmacht.workingtitle36.Value

/**
 * A base implementation of a View showing a transaction
 */
abstract class BaseTransactionView : RelativeLayout {

    private lateinit var valueView: ValueTextView
    private lateinit var descriptionView: DescriptionView

    var value: Value? = null
        set(value) { valueView.value = value }

    var description: String? = null
        set(value) { descriptionView.description = value }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onFinishInflate() {
        super.onFinishInflate()

        valueView = findViewById(R.id.value_input)
        descriptionView = findViewById(R.id.description)
    }

    /**
     * Return the length of the text in the ValueView
     */
    val valueTextLength: Int
        get() = valueView.text.length

    /**
     * Calculates the width of the amount text
     * @return The width of the amount text in pixels
     */
    fun getValueTextWidth(): Int {
        valueView.measure(0, 0)
        return valueView.measuredWidth
    }

    fun setValueViewWidth(width: Int) {
        valueView.minWidth = width
    }
}
