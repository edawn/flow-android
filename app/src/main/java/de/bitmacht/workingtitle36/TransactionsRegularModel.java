package de.bitmacht.workingtitle36;

import android.database.Cursor;

public class TransactionsRegularModel {

    public long regularId;
    public long executionTime;
    public int periodNumber;

    public TransactionsRegularModel(Cursor cursor) {
        regularId = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_REGULAR_ID));
        executionTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_EXECUTION_TIME));
        periodNumber = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_REGULAR_KEY_PERIOD_NUMBER));
    }
}
