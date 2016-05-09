package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class TransactionsUpdateTask extends AsyncTask<Edit, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsUpdateTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;

    public TransactionsUpdateTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(Edit... edits) {
        if (edits.length == 0) {
            if (BuildConfig.DEBUG) {
                logger.warn("nothing to do ... exiting");
            }
            return true;
        } else if (edits.length > 1) {
            if (BuildConfig.DEBUG) {
                logger.warn("only one Edit expected; received {}", edits.length);
            }
        }
        Edit edit = edits[0];
        if (BuildConfig.DEBUG) {
            logger.trace("inserting: {}", edit);
        }

        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues(10);

                cv.put(DBHelper.TRANSACTIONS_KEY_ID, edit.getCtime());
                cv.put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, false);
                long res = db.insert(DBHelper.TRANSACTIONS_TABLE_NAME, null, cv);
                if (res == -1) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("inserting transaction {} failed", cv.get(DBHelper.TRANSACTIONS_KEY_ID));
                    }
                }

                cv.clear();
                cv.put(DBHelper.EDITS_KEY_ID, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_PARENT, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_SEQUENCE, 0);
                cv.put(DBHelper.EDITS_KEY_IS_PENDING, false);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.getTtime());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.getTdesc());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.getTloc());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.getTcurrency());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT, edit.getTamount());
                res = db.insertOrThrow(DBHelper.EDITS_TABLE_NAME, null, cv);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.error("modifying database failed");
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
