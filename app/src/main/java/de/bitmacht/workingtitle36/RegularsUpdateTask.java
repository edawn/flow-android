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

public class RegularsUpdateTask extends AsyncTask<RegularModel, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(RegularsUpdateTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;

    public RegularsUpdateTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(RegularModel... regulars) {
        if (regulars.length != 1) {
            throw new IllegalArgumentException("only one regular transaction allowed");
        }

        RegularModel regular = regulars[0];
        if (BuildConfig.DEBUG) {
            logger.trace("inserting: {}", regular);
        }

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.insertOrThrow(DBHelper.REGULARS_TABLE_NAME, null,
                        regular.toContentValues(new ContentValues(10)));

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
