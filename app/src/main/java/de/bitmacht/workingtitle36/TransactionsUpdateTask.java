package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class TransactionsUpdateTask extends AsyncTask<Edit, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsUpdateTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;

    public TransactionsUpdateTask(Context context, UpdateFinishedCallback callback) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<UpdateFinishedCallback>(callback);
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

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {

                ContentValues cv = new ContentValues();
                cv.put(DBHelper.TRANSACTIONS_KEY_CREATION_TIME, edit.getCtime());
                cv.put(DBHelper.TRANSACTIONS_KEY_ISREMOVED, false);
                long res = db.insert(DBHelper.TRANSACTIONS_TABLE_NAME, null, cv);
                if (res == -1) {
                    if (BuildConfig.DEBUG) {
                        logger.warn("inserting transaction {} failed", cv.get(DBHelper.TRANSACTIONS_KEY_CREATION_TIME));
                    }
                }

                cv.clear();
                cv.put(DBHelper.EDITS_KEY_CREATION_TIME, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_PARENT, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION, edit.getCtime());
                cv.put(DBHelper.EDITS_KEY_SEQUENCE, 0);
                cv.put(DBHelper.EDITS_KEY_ISPENDING, false);
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.getTtime());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.getTdesc());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.getTloc());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.getTcurrency());
                cv.put(DBHelper.EDITS_KEY_TRANSACTION_VALUE, edit.getTvalue());
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
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        logger.trace("closing database failed");
                    }
                }
            }
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
