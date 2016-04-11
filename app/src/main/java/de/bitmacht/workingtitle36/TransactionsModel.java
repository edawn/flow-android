package de.bitmacht.workingtitle36;

import android.database.Cursor;

/**
 * This represents a transaction
 * @see DBHelper#TRANSACTIONS_TABLE_NAME
 */
public class TransactionsModel {

    public long creationTime;
    public boolean isRemoved;

    public Edit mostRecentEdit = null;

    private TransactionsModel() {}

    public TransactionsModel(Cursor cursor) {
        creationTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_CREATION_TIME));
        isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_ISREMOVED)) != 0;
    }

    /**
     * Returns a new instance of a transaction with an associated Edit.
     * This is intended to be used with the results of {@link DBHelper#TRANSACTIONS_EDITS_QUERY} or {@link DBHelper#TRANSACTIONS_EDITS_TIME_SPAN_QUERY}
     * @param cursor A cursor positioned at the appropriate row.
     *               The columns should include any column from {@link DBHelper#EDITS_TABLE_NAME} and
     *               {@link DBHelper#TRANSACTIONS_KEY_ISREMOVED} from {@link DBHelper#TRANSACTIONS_TABLE_NAME}
     * @return A new {@link TransactionsModel} with the field {@link TransactionsModel#mostRecentEdit} set.
     */
    public static TransactionsModel getInstanceWithEdit(Cursor cursor) {
        TransactionsModel transaction = new TransactionsModel();
        transaction.creationTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION));
        transaction.isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_ISREMOVED)) != 0;
        transaction.mostRecentEdit = new Edit(cursor);
        return transaction;
    }

    @Override
    public String toString() {
        return "cTme: " + creationTime + " isRe: " + isRemoved + " moRE: " + mostRecentEdit;
    }
}
