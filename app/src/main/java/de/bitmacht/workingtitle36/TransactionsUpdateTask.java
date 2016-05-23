package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class TransactionsUpdateTask extends AsyncTask<Void, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsUpdateTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;
    private Edit edit;

    public TransactionsUpdateTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback, @NonNull Edit edit) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
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

                Long transactionId = edit.transaction;
                Long parentId = null;
                int sequence = 0;

                boolean createNewTransaction = false;
                if (transactionId != null) {
                    Cursor cursor = db.rawQuery(DBHelper.TRANSACTION_QUERY, new String[]{Long.toString(transactionId)});
                    int transactionsCount = cursor.getCount();
                    cursor.close();
                    if (transactionsCount == 0) {
                        transactionId = null;
                        createNewTransaction = true;
                    } else {
                        parentId = edit.parent;
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
                            sequence = Math.max(sequence, tmpEdit.sequence + 1);
                            if (edit.parent == tmpEdit.id) {
                                parentId = edit.parent;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                    if (parentId == null) {
                        throw new Exception("No parent found for edit " + edit.id + " in transaction " + transactionId);
                    }
                }

                cv.put(DBHelper.EDITS_KEY_PARENT, parentId);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION, transactionId);
                cv.put(DBHelper.EDITS_KEY_SEQUENCE, sequence);
                cv.put(DBHelper.EDITS_KEY_IS_PENDING, false);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.transactionTime);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.transactionDescription);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.transactionLocation);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.transactionCurrency);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT, edit.transactionAmount);
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

    @Override
    protected void onPostExecute(Boolean success) {
        UpdateFinishedCallback callback = callbackRef.get();
        if (callback != null) {
            callback.onUpdateFinished(success);
        }
    }

    interface UpdateFinishedCallback {
        void onUpdateFinished(boolean success);
    }
}
