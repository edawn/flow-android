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
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class TransactionsUpdateTask(context: Context, private val edit: Edit) : DBModifyingAsyncTask(context) {

    private val dbHelper = DBHelper(context)

    override fun doInBackground(vararg voids: Void): Boolean? {
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                var transactionId = edit.transaction
                var parentId: Long? = null
                var sequence = 0

                var createNewTransaction = true
                if (transactionId != null) {
                    val transactionsCount =
                            db.rawQuery(DBHelper.TRANSACTION_QUERY, arrayOf(java.lang.Long.toString(transactionId))).use { it.count }
                    if (transactionsCount == 0) {
                        transactionId = null
                    } else {
                        parentId = edit.parent
                        createNewTransaction = false
                        if (transactionsCount > 1) logw("this should not happen: more than one resulting row")
                    }
                }

                if (createNewTransaction) {
                    // transaction does not exist; insert new
                    transactionId = db.insertOrThrow(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                            ContentValues().apply { put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, false) })
                } else {
                    db.rawQuery(DBHelper.EDITS_FOR_TRANSACTION_QUERY, arrayOf(java.lang.Long.toString(transactionId!!))).use { cursor ->
                        logd("${cursor.count} edits for transaction $transactionId")
                        while (cursor.moveToNext()) {
                            val tmpEdit = Edit(cursor)
                            sequence = Math.max(sequence, tmpEdit.sequence!! + 1)
                            if (edit.parent == tmpEdit.id) parentId = edit.parent
                        }
                    }
                    if (parentId == null)
                        throw Exception("No parent found for edit ${edit.id} in transaction $transactionId")
                }

                db.insertOrThrow(DBHelper.EDITS_TABLE_NAME, null, ContentValues(10).apply {
                    put(DBHelper.EDITS_KEY_PARENT, parentId)
                    put(DBHelper.EDITS_KEY_TRANSACTION, transactionId)
                    put(DBHelper.EDITS_KEY_SEQUENCE, sequence)
                    put(DBHelper.EDITS_KEY_IS_PENDING, false)
                    put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.transactionTime)
                    put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.transactionDescription)
                    put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.transactionLocation)
                    put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.transactionCurrency)
                    put(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT, edit.transactionAmount)
                })

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                db.close()
            }
        } catch (e: Exception) {
            loge("modifying database failed", e)
            return false
        }

        logd("finished inserting")
        return true
    }
}
