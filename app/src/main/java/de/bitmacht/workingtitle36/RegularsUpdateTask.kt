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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase


class RegularsUpdateTask(context: Context, private val regular: RegularModel) : DBModifyingAsyncTask(context) {

    private val dbHelper: DBHelper

    init {
        dbHelper = DBHelper(context)
    }

    override fun doInBackground(vararg voids: Void): Boolean? {
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.insertWithOnConflict(DBHelper.REGULARS_TABLE_NAME, null,
                        regular.toContentValues(ContentValues(10)), SQLiteDatabase.CONFLICT_REPLACE)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            loge("modifying database failed")
            return false
        }

        logd("finished inserting")
        return true
    }
}
