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

import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner

import org.joda.time.DateTimeConstants
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Calendar
import java.util.GregorianCalendar

import de.bitmacht.workingtitle36.view.TimeView
import de.bitmacht.workingtitle36.view.ValueWidget

class RegularEditActivity : AppCompatActivity(), View.OnClickListener, DatePickerFragment.OnDateSetListener {

    private var regularId: Long? = null
    private val timeFirst = GregorianCalendar()
    private var timeLast: Calendar? = null
    private var isLastIndefinite = true
    private var value: Value? = null

    private var toolbar: Toolbar? = null
    private var cancelButton: ImageButton? = null
    private var acceptButton: ImageButton? = null
    private var enabledSwitch: SwitchCompat? = null
    private var dateFirstView: TimeView? = null
    private var dateLastIndefButton: Button? = null
    private var dateLastContainer: LinearLayout? = null
    private var dateLastClearButton: ImageButton? = null
    private var dateLastView: TimeView? = null
    private var valueWidget: ValueWidget? = null
    private var repetitionSpinner: Spinner? = null
    private var descriptionView: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_regular_edit)

        toolbar = findViewById(R.id.toolbar) as Toolbar
        cancelButton = findViewById(R.id.cancel_button) as ImageButton
        acceptButton = findViewById(R.id.accept_button) as ImageButton
        enabledSwitch = findViewById(R.id.enabled) as SwitchCompat
        dateFirstView = findViewById(R.id.date_first) as TimeView
        dateLastIndefButton = findViewById(R.id.date_last_indef_button) as Button
        dateLastContainer = findViewById(R.id.date_last_container) as LinearLayout
        dateLastClearButton = findViewById(R.id.date_last_clear_button) as ImageButton
        dateLastView = findViewById(R.id.date_last) as TimeView
        valueWidget = findViewById(R.id.value) as ValueWidget
        repetitionSpinner = findViewById(R.id.repetition) as Spinner
        descriptionView = findViewById(R.id.description) as EditText

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        cancelButton!!.setOnClickListener(this)
        acceptButton!!.setOnClickListener(this)

        dateFirstView!!.setOnClickListener(this)

        dateLastIndefButton!!.setOnClickListener(this)
        dateLastClearButton!!.setOnClickListener(this)
        dateLastView!!.setOnClickListener(this)

        val intervalAdapter = ArrayAdapter.createFromResource(this, R.array.interval_names, android.R.layout.simple_spinner_item)
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        repetitionSpinner!!.adapter = intervalAdapter

        if (savedInstanceState == null) {
            val intent = intent
            if (intent.hasExtra(EXTRA_REGULAR)) {
                // edit an existing regular
                val regular = intent.getParcelableExtra<RegularModel>(EXTRA_REGULAR)
                regularId = regular.id
                value = regular.value
                enabledSwitch!!.isChecked = !regular.isDisabled
                timeFirst.timeInMillis = regular.timeFirst
                if (regular.timeLast < 0) {
                    isLastIndefinite = true
                } else {
                    isLastIndefinite = false
                    timeLast = GregorianCalendar()
                    timeLast!!.timeInMillis = regular.timeLast
                }

                repetitionSpinner!!.setSelection(regular.periodIndex)
                descriptionView!!.setText(regular.description)
            } else {
                value = Value(MyApplication.currency.currencyCode, 0)
                // corresponds to 'Monthly' in R.array.interval_names
                repetitionSpinner!!.setSelection(2)
            }
        } else {
            regularId = if (savedInstanceState.containsKey(DBHelper.REGULARS_KEY_ID))
                savedInstanceState.getLong(DBHelper.REGULARS_KEY_ID)
            else
                null
            timeFirst.timeInMillis = savedInstanceState.getLong(DBHelper.REGULARS_KEY_TIME_FIRST)
            isLastIndefinite = savedInstanceState.getBoolean(STATE_IS_LAST_INDEFINITE_KEY)
            if (savedInstanceState.containsKey(DBHelper.REGULARS_KEY_TIME_LAST)) {
                timeLast = GregorianCalendar()
                timeLast!!.timeInMillis = savedInstanceState.getLong(DBHelper.REGULARS_KEY_TIME_LAST)
            }
            value = savedInstanceState.getParcelable<Value>(STATE_VALUE_KEY)
        }

        valueWidget!!.value = value
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

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                val rut = RegularsUpdateTask(this, regular)
                rut.execute()
                setResult(RESULT_OK)
                finish()
                //TODO wait for the update to finish
            } else {
                finish()
            }
        } else if (id == R.id.date_first || id == R.id.date_last) {
            val frag = DatePickerFragment()
            val bundle = Bundle()
            bundle.putInt(DatePickerFragment.ARG_ID, id)
            val time: Calendar
            if (id == R.id.date_first) {
                time = timeFirst
            } else {
                time = timeLast!!
                bundle.putLong(DatePickerFragment.ARG_MIN_DATE, timeFirst.timeInMillis)
            }
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, time.timeInMillis)
            frag.arguments = bundle
            frag.show(fragmentManager, "datePicker")
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(window.decorView.windowToken, 0)
        } else if (id == R.id.date_last_indef_button) {
            isLastIndefinite = false
            updateLastTimeInput()
        } else if (id == R.id.date_last_clear_button) {
            isLastIndefinite = true
            updateLastTimeInput()
        }
    }

    override fun onDateSet(id: Int, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val dst = if (id == R.id.date_first) timeFirst else timeLast!!
        dst.set(Calendar.YEAR, year)
        dst.set(Calendar.MONTH, monthOfYear)
        dst.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        updateTimeViews()
    }

    private fun updateTimeViews() {
        dateFirstView!!.time = timeFirst.timeInMillis
        if (timeLast != null) {
            if (!timeFirst.before(timeLast)) {
                timeLast!!.timeInMillis = timeFirst.timeInMillis + 1
            }
            dateLastView!!.time = timeLast!!.timeInMillis
        }
    }

    private fun updateLastTimeInput() {
        if (isLastIndefinite) {
            dateLastIndefButton!!.visibility = View.VISIBLE
            dateLastContainer!!.visibility = View.GONE
        } else {
            dateLastIndefButton!!.visibility = View.GONE
            dateLastContainer!!.visibility = View.VISIBLE
            if (timeLast == null) {
                timeLast = GregorianCalendar()
                if (!timeFirst.before(timeLast)) {
                    timeLast!!.timeInMillis = timeFirst.timeInMillis + 1
                }
            }
            dateLastView!!.time = timeLast!!.timeInMillis
        }
    }

    private // case 0 is intrinsic
            // weekly
            // yearly
            // monthly
    val regular: RegularModel
        get() {
            val spinnerPos = repetitionSpinner!!.selectedItemPosition
            var periodType = DBHelper.REGULARS_PERIOD_TYPE_DAILY
            var periodMultiplier = 1
            when (spinnerPos) {
                1 -> periodMultiplier = DateTimeConstants.DAYS_PER_WEEK
                3 -> {
                    periodMultiplier = 12
                    periodType = DBHelper.REGULARS_PERIOD_TYPE_MONTHLY
                }
                2 -> periodType = DBHelper.REGULARS_PERIOD_TYPE_MONTHLY
            }

            val cv = valueWidget!!.value

            val regular = RegularModel(timeFirst.timeInMillis,
                    if (isLastIndefinite || timeLast == null) -1 else timeLast!!.timeInMillis,
                    periodType, periodMultiplier, false, !enabledSwitch!!.isChecked,
                    cv!!.amount, cv.currencyCode, descriptionView!!.text.toString())
            regular.id = regularId
            return regular
        }

    companion object {

        private val logger = LoggerFactory.getLogger(RegularEditActivity::class.java)

        /**
         * An optional extra containing a RegularModel that will be edited
         */
        val EXTRA_REGULAR = "regular"

        private val STATE_VALUE_KEY = "value"
        private val STATE_IS_LAST_INDEFINITE_KEY = "isLastIndefinite"
    }
}
