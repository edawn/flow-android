package de.bitmacht.workingtitle36;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

//TODO merge with RegularsUpdateTask or make it an inner class of OverviewRegularsActivity
public class RegularsRemoveTask extends AsyncTask<Void, Void, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(RegularsRemoveTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;
    private final long regularId;

    public RegularsRemoveTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback, long regularId) {
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
        this.regularId = regularId;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int count = db.delete(DBHelper.REGULARS_TABLE_NAME, DBHelper.REGULARS_KEY_ID + " = ?", new String[]{String.valueOf(regularId)});
                if (BuildConfig.DEBUG) {
                    if (count != 1) {
                        logger.trace("{} rows deleted; expected one; regular id: {}", count, regularId);
                    }
                }
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
            logger.trace("finished deleting");
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
