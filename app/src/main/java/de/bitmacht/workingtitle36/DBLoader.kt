/*
 * Copyright 2017 Kamil Sartys
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
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import java.util.*

typealias LoaderAction<R> = (SQLiteDatabase) -> R
//TODO possibly implement onStopLoading and onCanceled
class DBLoader<T> private constructor(context: Context, private val action: LoaderAction<T>) :
        AsyncTaskLoader<T>(context) {

    private val dbHelper = DBHelper(context)
    private var result: T? = null

    override fun loadInBackground(): T {
        return action(dbHelper.readableDatabase)
    }

    override fun deliverResult(result: T) {
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

    data class TransactionsResult(val transactions: ArrayList<TransactionsModel>, val periods: Periods)

    data class SuggestionsResult(val suggestions: ArrayList<Suggestion>, val column: String, val query: String)
    data class Suggestion(val text: String, val frequency: Int)

    companion object {

        const val ARG_PERIODS = "periods"

        const val ARG_COLUMN = "column"
        const val ARG_QUERY = "query"

        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        const val COLUMN_DESCRIPTION = DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION
        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        const val COLUMN_LOCATION = DBHelper.EDITS_KEY_TRANSACTION_LOCATION

        fun createTransactionsLoader(context: Context, periods: Periods): DBLoader<TransactionsResult> {
            return DBLoader(context, { db: SQLiteDatabase ->
                TransactionsResult(db.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY,
                        arrayOf(periods.longStart.millis.toString(), periods.longEnd.millis.toString())).use { cursor ->
                    ArrayList<TransactionsModel>(cursor.count).apply {
                        while (cursor.moveToNext())
                            add(TransactionsModel.getInstanceWithEdit(cursor))
                    }
                }, periods)
            })
        }

        fun createTransactionsLoader(context: Context, args: Bundle): DBLoader<TransactionsResult> =
                createTransactionsLoader(context, args.getParcelable<Periods>(ARG_PERIODS))


        fun createRegularsLoader(context: Context, includeDisabled: Boolean = false): DBLoader<ArrayList<RegularModel>> {
            return DBLoader(context, { db: SQLiteDatabase ->
                db.rawQuery(if (includeDisabled) DBHelper.REGULARS_QUERY else DBHelper.ACTIVE_REGULARS_QUERY, null)
                        .use { cursor ->
                            with(ArrayList<RegularModel>(cursor.count)) {
                                while (cursor.moveToNext()) add(de.bitmacht.workingtitle36.RegularModel(cursor))
                                this
                            }
                        }
            })
        }

        fun createSuggestionsLoader(context: Context, args: Bundle): DBLoader<SuggestionsResult> {
            val column = args.getString(ARG_COLUMN)
            val query = args.getString(ARG_QUERY)
            return DBLoader(context, { db: SQLiteDatabase ->
                SuggestionsResult(db.rawQuery(String.format(DBHelper.SUGGESTIONS_QUERY, column), arrayOf("$query%")).use { cursor ->
                    ArrayList<Suggestion>(cursor.count).apply {
                        while (cursor.moveToNext()) {
                            add(Suggestion(cursor.getString(0), cursor.getInt(1)))
                        }
                    }
                }, column, query)
            })
        }
    }
}
