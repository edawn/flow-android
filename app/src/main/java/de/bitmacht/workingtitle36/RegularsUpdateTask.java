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

public class RegularsUpdateTask extends AsyncTask<Void, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(RegularsUpdateTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;
    private final RegularModel regular;

    public RegularsUpdateTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback, RegularModel regular) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
        this.regular = regular;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.insertWithOnConflict(DBHelper.REGULARS_TABLE_NAME, null,
                        regular.toContentValues(new ContentValues(10)), SQLiteDatabase.CONFLICT_REPLACE);
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
