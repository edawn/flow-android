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

package de.bitmacht.workingtitle36

import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.accept_dismiss_toolbar.*
import kotlinx.android.synthetic.main.activity_regular_edit.*
import org.joda.time.DateTimeConstants
import java.util.*

class RegularEditActivity : AppCompatActivity(), DatePickerFragment.OnDateSetListener {

    private var regularId: Long? = null
    private val timeFirst = GregorianCalendar()
    private var timeLast: Calendar? = null
    private var isLastIndefinite = true
    private var value: Value? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_regular_edit)

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        cancel_button.setOnClickListener({ finish() })
        accept_button.setOnClickListener({
            RegularsUpdateTask(this, getRegular()).execute()
            setResult(Activity.RESULT_OK)
            finish()
        })

        class DateClickListener(val timeGetter: () -> Calendar, val timeMin: Calendar? = null) : View.OnClickListener {
            override fun onClick(v: View) {
                val args = Bundle()
                args.putInt(DatePickerFragment.ARG_ID, v.id)
                timeMin?.let { args.putLong(DatePickerFragment.ARG_MIN_DATE, it.timeInMillis) }
                args.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, timeGetter().timeInMillis)
                DatePickerFragment().apply { arguments = args }.show(fragmentManager, "datePicker")
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(window.decorView.windowToken, 0)
            }
        }
        date_first.setOnClickListener(DateClickListener({ timeFirst }))
        date_last.setOnClickListener(DateClickListener({ timeLast!! }, timeFirst))

        date_last_indef_button.setOnClickListener({
            isLastIndefinite = false
            updateLastTimeInput()
        })
        date_last_clear_button.setOnClickListener({
            isLastIndefinite = true
            updateLastTimeInput()
        })

        repetition.adapter =
                ArrayAdapter.createFromResource(this, R.array.interval_names, android.R.layout.simple_spinner_item)
                        .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        if (savedInstanceState == null) {
            if (intent.hasExtra(EXTRA_REGULAR)) {
                // edit an existing regular
                val regular = intent.getParcelableExtra<RegularModel>(EXTRA_REGULAR)
                regularId = regular.id
                value = regular.value
                enabled.isChecked = !regular.isDisabled
                timeFirst.timeInMillis = regular.timeFirst
                if (regular.timeLast < 0) {
                    isLastIndefinite = true
                } else {
                    isLastIndefinite = false
                    timeLast = GregorianCalendar()
                    timeLast!!.timeInMillis = regular.timeLast
                }

                repetition.setSelection(regular.periodIndex)
                description.setText(regular.description)
            } else {
                value = Value(MyApplication.currency.currencyCode, 0)
                // corresponds to 'Monthly' in R.array.interval_names
                repetition.setSelection(2)
            }
        } else {
            with(savedInstanceState) {
                regularId = if (containsKey(DBHelper.REGULARS_KEY_ID))
                    getLong(DBHelper.REGULARS_KEY_ID)
                else
                    null
                timeFirst.timeInMillis = getLong(DBHelper.REGULARS_KEY_TIME_FIRST)
                isLastIndefinite = getBoolean(STATE_IS_LAST_INDEFINITE_KEY)
                if (containsKey(DBHelper.REGULARS_KEY_TIME_LAST)) {
                    timeLast = GregorianCalendar().apply {
                        timeInMillis = getLong(DBHelper.REGULARS_KEY_TIME_LAST)
                    }
                }
                value = getParcelable<Value>(STATE_VALUE_KEY)
            }
        }

        value_input.value = value
        updateTimeViews()
        updateLastTimeInput()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (regularId != null) {
            outState.putLong(DBHelper.REGULARS_KEY_ID, regularId!!)
        }
        outState.putLong(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst.timeInMillis)
        outState.putBoolean(STATE_IS_LAST_INDEFINITE_KEY, isLastIndefinite)
        if (timeLast != null) {
            outState.putLong(DBHelper.REGULARS_KEY_TIME_LAST, timeLast!!.timeInMillis)
        }
        outState.putParcelable(STATE_VALUE_KEY, value)
    }

    override fun onDateSet(id: Int, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        with(if (id == R.id.date_first) timeFirst else timeLast!!) {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOfYear)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        updateTimeViews()
    }

    private fun updateTimeViews() {
        date_first.time = timeFirst.timeInMillis
        if (timeLast != null) {
            if (!timeFirst.before(timeLast)) {
                timeLast!!.timeInMillis = timeFirst.timeInMillis + 1
            }
            date_last.time = timeLast!!.timeInMillis
        }
    }

    private fun updateLastTimeInput() {
        if (isLastIndefinite) {
            date_last_indef_button.visibility = View.VISIBLE
            date_last_container.visibility = View.GONE
        } else {
            date_last_indef_button.visibility = View.GONE
            date_last_container.visibility = View.VISIBLE
            if (timeLast == null) {
                timeLast = GregorianCalendar()
                if (!timeFirst.before(timeLast)) {
                    timeLast!!.timeInMillis = timeFirst.timeInMillis + 1
                }
            }
            date_last.time = timeLast!!.timeInMillis
        }
    }

    private fun getRegular(): RegularModel {
        val spinnerPos = repetition.selectedItemPosition
        var periodType = DBHelper.REGULARS_PERIOD_TYPE_DAILY
        var periodMultiplier = 1
        when (spinnerPos) {
            1 -> periodMultiplier = DateTimeConstants.DAYS_PER_WEEK
            2 -> periodType = DBHelper.REGULARS_PERIOD_TYPE_MONTHLY
            3 -> {
                periodMultiplier = 12; periodType = DBHelper.REGULARS_PERIOD_TYPE_MONTHLY
            }
        }

        val cv = value_input.value

        return RegularModel(regularId, timeFirst.timeInMillis,
                if (isLastIndefinite || timeLast == null) -1 else timeLast!!.timeInMillis,
                periodType, periodMultiplier, false, !enabled.isChecked,
                cv!!.amount, cv.currencyCode, description.text.toString())
    }

    companion object {

        /**
         * An optional extra containing a RegularModel that will be edited
         */
        val EXTRA_REGULAR = "regular"

        private val STATE_VALUE_KEY = "value"
        private val STATE_IS_LAST_INDEFINITE_KEY = "isLastIndefinite"
    }
}
