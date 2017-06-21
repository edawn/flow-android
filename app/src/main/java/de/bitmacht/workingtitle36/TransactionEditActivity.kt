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
import android.app.LoaderManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TimePicker

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Calendar
import java.util.GregorianCalendar

import de.bitmacht.workingtitle36.view.TimeView
import de.bitmacht.workingtitle36.view.ValueInput
import de.bitmacht.workingtitle36.view.ValueWidget

class TransactionEditActivity : AppCompatActivity(), View.OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerFragment.OnDateSetListener {

    private var transactionId: Long? = null
    private var parentId: Long? = null
    private val transactionTime = GregorianCalendar()

    private var dbHelper: DBHelper? = null

    private var toolbar: Toolbar? = null
    private var cancelButton: ImageButton? = null
    private var acceptButton: ImageButton? = null
    private var timeView: TimeView? = null
    private var dateView: TimeView? = null
    private var valueWidget: ValueWidget? = null
    private var descriptionView: EditText? = null
    private var locationView: EditText? = null
    private var focusValueInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DBHelper(this)

        setContentView(R.layout.activity_transaction_edit)

        toolbar = findViewById(R.id.toolbar) as Toolbar
        cancelButton = findViewById(R.id.cancel_button) as ImageButton
        acceptButton = findViewById(R.id.accept_button) as ImageButton
        timeView = findViewById(R.id.time) as TimeView
        dateView = findViewById(R.id.date) as TimeView
        valueWidget = findViewById(R.id.value) as ValueWidget
        descriptionView = findViewById(R.id.description) as EditText
        locationView = findViewById(R.id.location) as EditText

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        cancelButton!!.setOnClickListener(this)
        acceptButton!!.setOnClickListener(this)

        timeView!!.setOnClickListener(this)
        dateView!!.setOnClickListener(this)

        var value: Value? = null
        if (savedInstanceState == null) {
            val intent = intent
            if (intent.hasExtra(EXTRA_TRANSACTION)) {
                val transaction = intent.getParcelableExtra<TransactionsModel>(EXTRA_TRANSACTION)
                if (transaction.mostRecentEdit != null) {
                    transactionId = transaction.id
                    val parentEdit = transaction.mostRecentEdit
                    parentId = parentEdit.id
                    transactionTime.timeInMillis = parentEdit.transactionTime
                    descriptionView!!.setText(parentEdit.transactionDescription)
                    locationView!!.setText(parentEdit.transactionLocation)
                    value = parentEdit.value
                } else {
                    if (BuildConfig.DEBUG) {
                        logger.warn("A transaction without an edit: id: {}", transaction.id)
                    }
                }
            } else {
                // focus value input only when not editing an existing transaction
                focusValueInput = true
            }
        } else {
            if (savedInstanceState.containsKey(DBHelper.TRANSACTIONS_KEY_ID)) {
                transactionId = savedInstanceState.getLong(DBHelper.TRANSACTIONS_KEY_ID)
                parentId = savedInstanceState.getLong(DBHelper.EDITS_KEY_PARENT)
            }
            transactionTime.timeInMillis = savedInstanceState.getLong(DBHelper.EDITS_KEY_TRANSACTION_TIME)
            value = savedInstanceState.getParcelable<Value>(STATE_VALUE_KEY)
        }

        updateTimeViews()

        if (value == null) {
            value = Value(MyApplication.currency.currencyCode, 0)
        }

        if (valueWidget is ValueInput) {
            valueWidget!!.value = value
        }

        var args = Bundle()
        args.putString(TransactionsSuggestionsLoader.ARG_COLUMN, TransactionsSuggestionsLoader.COLUMN_DESCRIPTION)
        loaderManager.initLoader(0, args, suggestionsListener)

        args = Bundle()
        args.putString(TransactionsSuggestionsLoader.ARG_COLUMN, TransactionsSuggestionsLoader.COLUMN_LOCATION)
        loaderManager.initLoader(1, args, suggestionsListener)
    }

    override fun onResume() {
        super.onResume()
        if (focusValueInput) {
            (valueWidget as ValueInput).focusEditText()
            focusValueInput = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (transactionId != null) {
            outState.putLong(DBHelper.TRANSACTIONS_KEY_ID, transactionId!!)
            // the existence of parentId depends on the existence of transactionId
            outState.putLong(DBHelper.EDITS_KEY_PARENT, parentId!!)
        }
        outState.putLong(DBHelper.EDITS_KEY_TRANSACTION_TIME, transactionTime.timeInMillis)
        outState.putParcelable(STATE_VALUE_KEY, valueWidget!!.value)
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.accept_button || id == R.id.cancel_button) {
            if (id == R.id.accept_button) {
                val edit = edit
                if (BuildConfig.DEBUG) {
                    logger.trace("edit: {}", edit)
                }
                TransactionsUpdateTask(this, edit).execute()
                //TODO wait for the update to finish
                finish()
            } else {
                //TODO if there was any data entered, show confirmation dialog
                // or: save the data, finish and show a snackbar
                finish()
            }

        } else if (id == R.id.time || id == R.id.date) {
            val frag = if (id == R.id.time) TimePickerFragment() else DatePickerFragment()
            val bundle = Bundle()
            bundle.putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, transactionTime.timeInMillis)
            frag.arguments = bundle
            frag.show(fragmentManager, if (id == R.id.time) "timePicker" else "datePicker")

            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(window.decorView.windowToken, 0)
        }
    }

    override fun onDateSet(id: Int, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        transactionTime.set(Calendar.YEAR, year)
        transactionTime.set(Calendar.MONTH, monthOfYear)
        transactionTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        updateTimeViews()
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        transactionTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
        transactionTime.set(Calendar.MINUTE, minute)
        updateTimeViews()
    }

    private fun updateTimeViews() {
        val time = transactionTime.timeInMillis
        timeView!!.time = time
        dateView!!.time = time
    }

    /**
     * Returns an Edit matching the currently set data
     */
    private val edit: Edit
        get() = Edit(parentId, transactionId, transactionTime.timeInMillis,
                descriptionView!!.text.toString(), locationView!!.text.toString(), valueWidget!!.value)

    private val suggestionsListener = object : LoaderManager.LoaderCallbacks<ArrayAdapter<String>> {
        override fun onCreateLoader(id: Int, args: Bundle): Loader<ArrayAdapter<String>> {
            return TransactionsSuggestionsLoader(this@TransactionEditActivity, dbHelper, args)
        }

        override fun onLoadFinished(loader: Loader<ArrayAdapter<String>>, data: ArrayAdapter<String>) {
            val destinationView = (if ((loader as TransactionsSuggestionsLoader<*>).column == TransactionsSuggestionsLoader.COLUMN_DESCRIPTION)
                descriptionView
            else
                locationView) as AutoCompleteTextView
            destinationView.setAdapter(data)
        }

        override fun onLoaderReset(loader: Loader<ArrayAdapter<String>>) {}
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TransactionEditActivity::class.java)

        /**
         * An optional extra containing a TransactionsModel that will be edited
         */
        val EXTRA_TRANSACTION = "transaction"

        private val STATE_VALUE_KEY = "value"
    }
}
