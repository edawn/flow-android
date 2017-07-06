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

import android.content.AsyncTaskLoader
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle

import java.util.ArrayList

//TODO possibly implement onStopLoading and onCanceled
class TransactionsLoader : AsyncTaskLoader<ArrayList<TransactionsModel>> {

    private val dbHelper: DBHelper
    val periods: Periods
    private var result: ArrayList<TransactionsModel>? = null

    constructor(context: Context, dbHelper: DBHelper, args: Bundle) : super(context) {
        this.dbHelper = dbHelper
        periods = args.getParcelable<Periods>(ARG_PERIODS)
    }

    constructor(context: Context, dbHelper: DBHelper, periods: Periods) : super(context) {
        this.dbHelper = dbHelper
        this.periods = periods
    }

    override fun loadInBackground(): ArrayList<TransactionsModel> {
        val db = dbHelper.readableDatabase
        val start = periods.longStart.millis
        val end = periods.longEnd.millis
        val cursor = db.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY, arrayOf(start.toString(), end.toString()))

        val result = ArrayList<TransactionsModel>(cursor.count)

        while (cursor.moveToNext()) {
            result.add(TransactionsModel.getInstanceWithEdit(cursor))
        }

        return result
    }

    override fun deliverResult(result: ArrayList<TransactionsModel>) {
        this.result = result
        super.deliverResult(result)
    }

    override fun onStartLoading() {
        if (result != null) {
            deliverResult(result!!)
        }
        if (takeContentChanged() || result == null) {
            forceLoad()
        }
    }

    override fun onReset() {
        super.onReset()
        result = null
    }

    companion object {

        val ARG_PERIODS = "periods"
    }
}
