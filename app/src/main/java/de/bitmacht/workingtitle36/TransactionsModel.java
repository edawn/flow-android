package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * This represents a transaction
 * @see DBHelper#TRANSACTIONS_TABLE_NAME
 */
public class TransactionsModel implements Parcelable {

    public long id;
    public boolean isRemoved;

    public Edit mostRecentEdit = null;

    private TransactionsModel() {}

    public TransactionsModel(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_ID));
        isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_IS_REMOVED)) != 0;
    }

    /**
     * Returns a new instance of a transaction with an associated Edit.
     * This is intended to be used with the results of {@link DBHelper#TRANSACTIONS_EDITS_QUERY} or {@link DBHelper#TRANSACTIONS_EDITS_TIME_SPAN_QUERY}
     * @param cursor A cursor positioned at the appropriate row.
     *               The columns should include any column from {@link DBHelper#EDITS_TABLE_NAME} and
     *               {@link DBHelper#TRANSACTIONS_KEY_IS_REMOVED} from {@link DBHelper#TRANSACTIONS_TABLE_NAME}
     * @return A new {@link TransactionsModel} with the field {@link TransactionsModel#mostRecentEdit} set.
     */
    public static TransactionsModel getInstanceWithEdit(Cursor cursor) {
        TransactionsModel transaction = new TransactionsModel();
        transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION));
        transaction.isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_IS_REMOVED)) != 0;
        transaction.mostRecentEdit = new Edit(cursor);
        return transaction;
    }

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     *           insert into {@link DBHelper#TRANSACTIONS_TABLE_NAME}
     * @return the same instance from the arguments
     */
    public ContentValues toContentValues(@NonNull ContentValues cv) {
        cv.put(DBHelper.TRANSACTIONS_KEY_ID, id);
        cv.put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, isRemoved);
        return cv;
    }

    @Override
    public String toString() {
        return "id: " + id + " isRe: " + isRemoved + " moRE: " + mostRecentEdit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(isRemoved ? 1 : 0);
        dest.writeParcelable(mostRecentEdit, flags);
    }

    public static final Parcelable.Creator<TransactionsModel> CREATOR = new Parcelable.Creator<TransactionsModel>() {
        @Override
        public TransactionsModel createFromParcel(Parcel source) {
            return new TransactionsModel(source);
        }
        @Override
        public TransactionsModel[] newArray(int size) {
            return new TransactionsModel[size];
        }
    };

    public TransactionsModel(Parcel in) {
        id = in.readLong();
        isRemoved = in.readInt() == 1;
        mostRecentEdit = in.readParcelable(Edit.class.getClassLoader());
    }
}
