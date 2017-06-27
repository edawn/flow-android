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
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Selection
import android.text.Spanned
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Pair
import de.bitmacht.workingtitle36.*


import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale
import java.util.regex.Pattern

class ValueEditText : AppCompatEditText, ValueWidget {

    // utilize cash register like input style
    private var registerInputStyle = false
    private var currency: Currency? = null
    private var textWatcher: ValueTextWatcher? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        registerInputStyle = Utils.getbPref(context, R.string.pref_register_key, registerInputStyle)
        updateCurrency(MyApplication.currency)
        value = Value(currency!!.currencyCode, 0)
        setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
    }

    private fun extractValueString(rawValueString: String): String {
        var rawValueString = rawValueString
        val fractionDigits = currency!!.defaultFractionDigits
        rawValueString = rawValueString.replace("[^0-9.,]+".toRegex(), "")

        val separator = DecimalFormatSymbols.getInstance().monetaryDecimalSeparator
        val splits = rawValueString.split("[$separator]".toRegex(), 2).toTypedArray()

        var major = splits[0].replace("[^0-9]+".toRegex(), "")
        if (major.isEmpty()) {
            major = "0"
        }

        var minor = if (splits.size == 2) splits[1].replace("[^0-9]+".toRegex(), "") else ""
        if (fractionDigits == 0) {
            minor = ""
        } else {
            if (0 < fractionDigits) {
                if (fractionDigits < minor.length) {
                    minor = minor.substring(0, fractionDigits)
                }
                while (minor.length < fractionDigits) {
                    minor += "0"
                }
            }
        }

        return major + minor
    }

    override var value: Value?
        get() {
            val amount = java.lang.Long.parseLong(extractValueString(text.toString()))
            return Value(currency!!.currencyCode, amount)
        }
        set(value) {
            if (value!!.currencyCode != currency!!.currencyCode) {
                updateCurrency(value.currency)
            }
            val vs = value.getValueAndSymbolStrings(Locale.getDefault())
            val valueText = vs.first + vs.second
            setText(valueText)
        }

    val valueText: CharSequence
        get() = text

    private fun updateCurrency(currency: Currency) {
        this.currency = currency

        removeTextChangedListener(textWatcher)
        if (registerInputStyle) {
            filters = arrayOf<InputFilter>()
            textWatcher = ValueTextWatcher(currency)
            addTextChangedListener(textWatcher)
        } else {
            filters = arrayOf<InputFilter>(ValueInputFilter(currency))
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (textWatcher != null && textWatcher!!.isModfiying) {
            return
        }
        val text = editableText
        if (text != null && text.isNotEmpty() && currency != null) {
            val vl = text.length - currency!!.symbol.length
            var newStart = Math.min(selStart, vl)
            var newEnd = Math.min(selEnd, vl)

            // shift the cursor one to the left, if it should lie right of the decimal separator
            //XXX breaks shifting the cursor with the right arrow key
            if (registerInputStyle && newStart == newEnd && newEnd != 0 &&
                    !Character.isDigit(text[newEnd - 1])) {
                newStart--
                newEnd--
            }

            Selection.setSelection(text, newStart, newEnd)
        }
    }

    private inner class ValueInputFilter internal constructor(currency: Currency) : InputFilter {

        private val separator: Char
        private val separatorString: String
        private val fracts: Int
        private val symbol: String
        private val pattern: Pattern
        private val separatorReplacePattern: Pattern

        init {
            separator = DecimalFormatSymbols.getInstance().monetaryDecimalSeparator
            separatorString = Character.toString(separator)
            fracts = currency.defaultFractionDigits
            symbol = currency.symbol
            if (fracts > 0) {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)?(?:\\Q$separator\\E[0-9]{0,$fracts})?\\Q$symbol\\E")
            } else {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)?\\Q$symbol\\E")
            }
            separatorReplacePattern = Pattern.compile("[,.]")
        }

        override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence {
            val sourceMod = separatorReplacePattern.matcher(source).replaceAll(separatorString)
            val result = dest.subSequence(0, dstart).toString() + sourceMod + dest.subSequence(dend, dest.length)

            if (!pattern.matcher(result).matches()) {
                return dest.subSequence(dstart, dend)
            }
            if (extractValueString(result).length > MAX_DEC_LEN) {
                return ""
            }
            return sourceMod
        }
    }

    private inner class ValueTextWatcher internal constructor(currency: Currency) : TextWatcher {
        private val separator: Char
        private val separatorString: String
        private val fracts: Int
        private val symbol: String
        private val pattern: Pattern
        var isModfiying = false

        init {
            separator = DecimalFormatSymbols.getInstance().monetaryDecimalSeparator
            separatorString = Character.toString(separator)
            fracts = currency.defaultFractionDigits
            symbol = currency.symbol
            if (fracts > 0) {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)\\Q$separator\\E[0-9]{$fracts}\\Q$symbol\\E")
            } else {
                pattern = Pattern.compile("(?:0|[1-9]+[0-9]*)\\Q$symbol\\E")
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            logd("$s start: $start count: $count after: $after")
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            logd("$s start: $start before: $before count: $count")
        }

        override fun afterTextChanged(s: Editable) {
            if (isModfiying) {
                return
            }
            isModfiying = true
            try {
                var isValid = pattern.matcher(s).matches()
                isValid = isValid && extractValueString(s.toString()).length <= MAX_DEC_LEN

                logd("$s isValid: $isValid")

                if (isValid) {
                    return
                }

                var i = 0
                while (i < s.length) {
                    logd("i: $i s: $s")
                    if (!Character.isDigit(s[i])) {
                        s.delete(i, i + 1)
                    } else {
                        i++
                    }
                }

                logd("plain: $s")

                // remove excess leading zeros
                while (s.length > fracts + 1 && s[0] == '0') {
                    s.delete(0, 1)
                }

                // fill with zeros up to one before the decimal point
                while (s.length < fracts + 1) {
                    s.insert(0, "0")
                }

                while (s.length > MAX_DEC_LEN) {
                    s.delete(s.length - 1, s.length)
                }

                // add decimal point
                if (fracts > 0) {
                    s.insert(s.length - fracts, separatorString)
                }

                // add the currency symbol
                s.append(symbol)

                logd("revalidated: $s")
            } finally {
                isModfiying = false
                logd("final")
            }
        }
    }

    companion object {

        // The maximum length of the absolute value of a decimal number that will fit into a long
        val MAX_DEC_LEN = 18
    }
}
