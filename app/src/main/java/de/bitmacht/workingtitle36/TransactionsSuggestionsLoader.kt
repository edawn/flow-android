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
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.ListAdapter

import java.util.ArrayList

class TransactionsSuggestionsLoader<T>(context: Context, private val dbHelper: DBHelper, args: Bundle) : AsyncTaskLoader<T>(context) where T: ListAdapter {
    val column: String
    val query: String
    private var result: T? = null

    init {
        column = args.getString(ARG_COLUMN)
        if (column != COLUMN_DESCRIPTION && column != COLUMN_LOCATION) {
            throw IllegalArgumentException("Unknown column name: " + column)
        }
        query = args.getString(ARG_QUERY)
    }

    override fun loadInBackground(): T {
        val db = dbHelper.readableDatabase

        logd("querying $query in $column")
        val cursor = db.rawQuery(String.format(DBHelper.SUGGESTIONS_QUERY, column), arrayOf("$query%"))

        val resultArray = ArrayList<String>(cursor.count)

        while (cursor.moveToNext()) {
            val fs = FrequencyString(cursor)
            logd("res: ${fs.text}:${fs.count}")
            if (fs.text.length == 0) {
                continue
            }
            resultArray.add(fs.text)
        }

        cursor.close()

        return ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, resultArray) as T
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

    class FrequencyString internal constructor(cursor: Cursor) : Comparable<FrequencyString> {
        val text: String
        val count: Int

        init {
            text = cursor.getString(0)
            count = cursor.getInt(1)
        }

        override fun compareTo(o: FrequencyString): Int {
            return text.compareTo(o.text)
        }

        override fun equals(obj: Any?): Boolean {
            return obj is FrequencyString && text == obj.text
        }
    }

    companion object {

        const val ARG_COLUMN = "column"
        const val ARG_QUERY = "query"

        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        val COLUMN_DESCRIPTION = DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION
        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        val COLUMN_LOCATION = DBHelper.EDITS_KEY_TRANSACTION_LOCATION
    }
}
