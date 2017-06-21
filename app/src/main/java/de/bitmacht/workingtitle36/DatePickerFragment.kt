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

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import java.util.*

/**
 * Create a new instance
 */
class DatePickerFragment : TimeDatePickerDialogFragment(), DatePickerDialog.OnDateSetListener {

    private var argId = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = calendarFromArguments
        val dialog = DatePickerDialog(activity, this, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        val args = arguments
        if (args != null) {
            if (args.containsKey(ARG_MIN_DATE)) {
                dialog.datePicker.minDate = args.getLong(ARG_MIN_DATE)
            }
            if (args.containsKey(ARG_ID)) {
                argId = args.getInt(ARG_ID)
            }
        }
        return dialog
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val activity = activity
        if (activity is DatePickerFragment.OnDateSetListener) {
            activity.onDateSet(argId, year, monthOfYear, dayOfMonth)
        }
    }

    interface OnDateSetListener {
        fun onDateSet(id: Int, year: Int, monthOfYear: Int, dayOfMonth: Int)
    }

    companion object {

        /**
         * The key of an optional argument containing the id of this fragment. The id will used when
         * calling [OnDateSetListener.onDateSet]
         */
        val ARG_ID = "id"

        /**
         * The key of an optional argument to be passed to this fragment, containing the unix time (in ms)
         * of the first date to be shown in the picker
         */
        val ARG_MIN_DATE = "minDate"
    }
}
