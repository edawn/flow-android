package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TransactionsRegularModel {

    public long regularId;
    public long executionTime;
    public int periodNumber;

    public TransactionsRegularModel(long regularId, long executionTime, int periodNumber) {
        this.regularId = regularId;
        this.executionTime = executionTime;
        this.periodNumber = periodNumber;
    }

    public TransactionsRegularModel(Cursor cursor) {
        regularId = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_REGULAR_ID));
        executionTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_EXECUTION_TIME));
        periodNumber = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_PERIOD_NUMBER));
    }

    public void insert(SQLiteDatabase db) throws SQLException {
        ContentValues values = new ContentValues();
        values.put(DBHelper.TRANSACTIONS_REGULAR_KEY_REGULAR_ID, regularId);
        values.put(DBHelper.TRANSACTIONS_REGULAR_KEY_EXECUTION_TIME, executionTime);
        values.put(DBHelper.TRANSACTIONS_REGULAR_KEY_PERIOD_NUMBER, periodNumber);
        db.insertOrThrow(DBHelper.TRANSACTIONS_REGULAR_TABLE_NAME, null, values);
    }
}
