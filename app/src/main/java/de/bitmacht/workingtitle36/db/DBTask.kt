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

package de.bitmacht.workingtitle36.db

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.support.annotation.CallSuper
import android.support.v4.content.LocalBroadcastManager
import de.bitmacht.workingtitle36.*

typealias TaskAction = (SQLiteDatabase) -> Unit

class DBTask private constructor(context: Context, private val action: TaskAction, private val actionName: String = "") : AsyncTask<Void, Void, Boolean>() {

    private val dbHelper = DBHelper(context)
    private val appContext = context.applicationContext

    override fun doInBackground(vararg voids: Void): Boolean? {
        try {
            with(dbHelper.writableDatabase) {
                beginTransaction()
                try {
                    action(this)
                    setTransactionSuccessful()
                } finally {
                    endTransaction()
                }
            }
        } catch (e: Exception) {
            loge("$actionName failed", e)
            return false
        }

        logd("finished $actionName")
        return true
    }

    @CallSuper
    override fun onPostExecute(success: Boolean?) {
        //TODO add success extra
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(Intent(ACTION_DB_MODIFIED))
    }

    companion object {

        val ACTION_DB_MODIFIED = "de.bitmacht.workingtitle36.action.DB_MODIFIED"

        fun createTransactionUpdateTask(context: Context, transaction: TransactionsModel): DBTask {
            return DBTask(context, { db: SQLiteDatabase ->
                db.insertWithOnConflict(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                        transaction.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }, "transaction update")
        }

        fun createEditUpdateTask(context: Context, edit: Edit): DBTask {
            return DBTask(context, { db: SQLiteDatabase ->
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
            }, "edit update")
        }
    }
}
