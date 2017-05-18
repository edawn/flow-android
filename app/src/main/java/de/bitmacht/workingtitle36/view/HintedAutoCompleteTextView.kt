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

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.design.widget.TextInputLayout
import android.util.AttributeSet
import android.view.View
import android.view.ViewParent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.AutoCompleteTextView

/**
 * Brings hints to AutoCompleteTextView in IME extract mode
 * (similar to [android.support.design.widget.TextInputEditText]).
 */
class HintedAutoCompleteTextView : AutoCompleteTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    @RequiresApi(api = Build.VERSION_CODES.N)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int, popupTheme: Resources.Theme) : super(context, attrs, defStyleAttr, defStyleRes, popupTheme)

    // Copied from android.support.design.widget.TextInputEditText
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        val ic = super.onCreateInputConnection(outAttrs)
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            var parent = parent
            while (parent is View) {
                if (parent is TextInputLayout) {
                    outAttrs.hintText = parent.hint
                    break
                }
                parent = parent.getParent()
            }
        }
        return ic
    }
}
