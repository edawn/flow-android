package de.bitmacht.workingtitle36;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionsCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsCursorLoader.class);

    private final DBHelper dbHelper;

    private Cursor cursor;

    public TransactionsCursorLoader(Context context, DBHelper dbHelper) {
        super(context);
        this.dbHelper = dbHelper;
    }

    @Override
    public Cursor loadInBackground() {
        if (BuildConfig.DEBUG) {
            logger.trace(">");
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(DBHelper.LATEST_EDITS_QUERY, null);
    }

    @Override
    public void deliverResult(Cursor data) {
        if (BuildConfig.DEBUG) {
            logger.trace(">");
        }
        cursor = data;
        super.deliverResult(data);
    }

    @Override
    protected void onStartLoading() {
        if (BuildConfig.DEBUG) {
            logger.trace(">");
        }
        if (cursor != null) {
            deliverResult(cursor);
        }

        if (takeContentChanged() || cursor == null) {
            forceLoad();
        }
    }
}
