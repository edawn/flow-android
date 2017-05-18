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
import android.content.res.TypedArray
import android.os.Build
import android.util.AttributeSet
import android.widget.TextView

import de.bitmacht.workingtitle36.R

/**
 * Shows the description.
 */
class DescriptionView : TextView {

    /** Sets the description; null to clear */
    var description: String? = null
        set(value) {
            field = value
            updateDescription()
        }

    /**
     * Controls the brevity of this View. Correlates with the isConcise attribute.
     * If true, this View shows a short description; false otherwise
     */
    var isConcise: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateDescription()
            }
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initAttrs(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initAttrs(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initAttrs(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DescriptionView, defStyleAttr, defStyleRes)
        isConcise = a.getBoolean(R.styleable.DescriptionView_isConcise, isConcise)
        a.recycle()
    }

    private fun updateDescription() {
        var text: String? = null
        if (description != null) {
            text = description!!.trim { it <= ' ' }
            if (isConcise) {
                text = text.split("\\n".toRegex(), 2).toTypedArray()[0]
            }
        }
        setText(text)
    }
}
