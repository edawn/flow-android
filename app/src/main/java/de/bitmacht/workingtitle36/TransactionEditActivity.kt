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
import android.content.Loader
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TimePicker
import kotlinx.android.synthetic.main.accept_dismiss_toolbar.*
import kotlinx.android.synthetic.main.activity_transaction_edit.*
import java.util.*

class TransactionEditActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener, DatePickerFragment.OnDateSetListener {

    private var transactionId: Long? = null
    private var parentId: Long? = null
    private val transactionTime = GregorianCalendar()

    private var dbHelper: DBHelper? = null

    private var focusValueInput: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = DBHelper(this)

        setContentView(R.layout.activity_transaction_edit)

        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        //TODO if there was any data entered, show confirmation dialog
        // or: save the data, finish and show a snackbar
        cancel_button.setOnClickListener({ finish() })
        accept_button.setOnClickListener({
            logd("edit: $edit")
            DBTask.createEditUpdateTask(this, edit).execute()
            //TODO wait for the update to finish
            finish()
        })

        class TimeDateClickListener(val frag: DialogFragment) : View.OnClickListener {
            override fun onClick(v: View?) {
                frag.arguments = Bundle()
                        .apply { putLong(TimeDatePickerDialogFragment.BUNDLE_TIME, transactionTime.timeInMillis) }
                frag.show(fragmentManager, "TimeDatePicker")
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(window.decorView.windowToken, 0)
            }}
        time.setOnClickListener(TimeDateClickListener(TimePickerFragment()))
        date.setOnClickListener(TimeDateClickListener(DatePickerFragment()))

        var value: Value? = null
        if (savedInstanceState == null) {
            if (intent.hasExtra(EXTRA_TRANSACTION)) {
                val transaction = intent.getParcelableExtra<TransactionsModel>(EXTRA_TRANSACTION)
                if (transaction.mostRecentEdit != null) {
                    transactionId = transaction.id
                    val parentEdit = transaction.mostRecentEdit
                    parentId = parentEdit!!.id
                    transactionTime.timeInMillis = parentEdit.transactionTime
                    description.setText(parentEdit.transactionDescription)
                    location.setText(parentEdit.transactionLocation)
                    value = parentEdit.value
                } else {
                    logw("A transaction without an edit: id: ${transaction.id}")
                }
            } else {
                // focus value input only when not editing an existing transaction
                focusValueInput = true
            }
        } else {
            with(savedInstanceState) {
                if (containsKey(DBHelper.TRANSACTIONS_KEY_ID)) {
                    transactionId = getLong(DBHelper.TRANSACTIONS_KEY_ID)
                    parentId = getLong(DBHelper.EDITS_KEY_PARENT)
                }
                transactionTime.timeInMillis = getLong(DBHelper.EDITS_KEY_TRANSACTION_TIME)
                value = getParcelable<Value>(STATE_VALUE_KEY)
            }
        }

        updateTimeViews()

        if (value == null) {
            value = Value(MyApplication.currency.currencyCode, 0)
        }

        value_input.value = value

        loaderManager.initLoader(0, Bundle().apply {
            putString(TransactionsSuggestionsLoader.ARG_COLUMN, TransactionsSuggestionsLoader.COLUMN_DESCRIPTION)
        }, suggestionsListener)

        loaderManager.initLoader(1, Bundle().apply {
            putString(TransactionsSuggestionsLoader.ARG_COLUMN, TransactionsSuggestionsLoader.COLUMN_LOCATION)
        }, suggestionsListener)
    }

    override fun onResume() {
        super.onResume()
        if (focusValueInput) {
            value_input.focusEditText()
            focusValueInput = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            if (transactionId != null) {
                putLong(DBHelper.TRANSACTIONS_KEY_ID, transactionId!!)
                // the existence of parentId depends on the existence of transactionId
                putLong(DBHelper.EDITS_KEY_PARENT, parentId!!)
            }
            putLong(DBHelper.EDITS_KEY_TRANSACTION_TIME, transactionTime.timeInMillis)
            putParcelable(STATE_VALUE_KEY, value_input.value)
        }
    }

    override fun onDateSet(id: Int, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        with(transactionTime) {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOfYear)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        updateTimeViews()
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        with(transactionTime) {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
        }
        updateTimeViews()
    }

    private fun updateTimeViews() {
        val timeMillis = transactionTime.timeInMillis
        time.time = timeMillis
        date.time = timeMillis
    }

    /**
     * Returns an Edit matching the currently set data
     */
    private val edit: Edit
        get() = Edit(parentId, transactionId, transactionTime.timeInMillis,
                description.text.toString(), location.text.toString(), value_input.value!!)

    private val suggestionsListener = object : LoaderManager.LoaderCallbacks<ArrayAdapter<String>> {
        override fun onCreateLoader(id: Int, args: Bundle): Loader<ArrayAdapter<String>> =
                TransactionsSuggestionsLoader(this@TransactionEditActivity, dbHelper!!, args)

        override fun onLoadFinished(loader: Loader<ArrayAdapter<String>>, data: ArrayAdapter<String>) =
                (if ((loader as TransactionsSuggestionsLoader<*>).column == TransactionsSuggestionsLoader.COLUMN_DESCRIPTION)
                    description else location).setAdapter(data)

        override fun onLoaderReset(loader: Loader<ArrayAdapter<String>>) {}
    }

    companion object {

        /**
         * An optional extra containing a TransactionsModel that will be edited
         */
        val EXTRA_TRANSACTION = "transaction"

        private val STATE_VALUE_KEY = "value"
    }
}
