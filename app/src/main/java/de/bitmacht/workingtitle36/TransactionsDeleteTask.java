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

public class TransactionsDeleteTask extends AsyncTask<Void, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsDeleteTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;
    private final TransactionsModel transaction;

    public TransactionsDeleteTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback, @NonNull TransactionsModel transaction) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
        this.transaction = transaction;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues(2);

                db.insertWithOnConflict(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                        transaction.toContentValues(cv), SQLiteDatabase.CONFLICT_REPLACE);

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
