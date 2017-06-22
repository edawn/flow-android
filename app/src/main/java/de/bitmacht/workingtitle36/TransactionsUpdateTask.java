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

package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionsUpdateTask extends DBModifyingAsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsUpdateTask.class);

    private final DBHelper dbHelper;
    private Edit edit;

    public TransactionsUpdateTask(@NonNull Context context, @NonNull Edit edit) {
        super(context);
        dbHelper = new DBHelper(context);
        this.edit = edit;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues(10);

                Long transactionId = edit.getTransaction();
                Long parentId = null;
                int sequence = 0;

                boolean createNewTransaction = true;
                if (transactionId != null) {
                    Cursor cursor = db.rawQuery(DBHelper.TRANSACTION_QUERY, new String[]{Long.toString(transactionId)});
                    int transactionsCount = cursor.getCount();
                    cursor.close();
                    if (transactionsCount == 0) {
                        transactionId = null;
                    } else {
                        parentId = edit.getParent();
                        createNewTransaction = false;
                        if (BuildConfig.DEBUG) {
                            if (transactionsCount > 1) {
                                logger.warn("this should not happen: more than one resulting row");
                            }
                        }
                    }
                }

                if (createNewTransaction) {
                    // transaction does not exist; insert new
                    cv.put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, false);
                    transactionId = db.insertOrThrow(DBHelper.TRANSACTIONS_TABLE_NAME, null, cv);
                    cv.clear();
                } else {
                    Cursor cursor = db.rawQuery(DBHelper.EDITS_FOR_TRANSACTION_QUERY, new String[]{Long.toString(transactionId)});
                    try {
                        if (BuildConfig.DEBUG) {
                            logger.trace("{} edits for transaction {}", cursor.getCount(), transactionId);
                        }
                        while (cursor.moveToNext()) {
                            Edit tmpEdit = new Edit(cursor);
                            sequence = Math.max(sequence, tmpEdit.getSequence() + 1);
                            if (edit.getParent().equals(tmpEdit.getId())) {
                                parentId = edit.getParent();
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                    if (parentId == null) {
                        throw new Exception("No parent found for edit " + edit.getId() + " in transaction " + transactionId);
                    }
                }

                cv.put(DBHelper.EDITS_KEY_PARENT, parentId);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION, transactionId);
                cv.put(DBHelper.EDITS_KEY_SEQUENCE, sequence);
                cv.put(DBHelper.EDITS_KEY_IS_PENDING, false);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.getTransactionTime());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.getTransactionDescription());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.getTransactionLocation());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.getTransactionCurrency());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT, edit.getTransactionAmount());
                db.insertOrThrow(DBHelper.EDITS_TABLE_NAME, null, cv);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.error("modifying database failed", e);
            }
            return false;
        }

        if (BuildConfig.DEBUG) {
            logger.trace("finished inserting");
        }
        return true;
    }
}
