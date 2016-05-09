package de.bitmacht.workingtitle36;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * This describes an edit.
 * It relates to one row in the edits table.
 */
public class Edit implements Parcelable {

    public long id;
    public long transactionTime;
    public String transactionDescription;
    public String transactionLocation;
    public String transactionCurrency;
    public long transactionAmount;

    /**
     * Create a new Edit from a cursor
     * @throws IllegalArgumentException If a required column is missing
     */
    public Edit(Cursor cursor) {
        this(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_LOCATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT)));
    }

    public Edit(long id, long transactionTime, String transactionDescription, String transactionLocation, Value value) {
        this(id, transactionTime, transactionDescription, transactionLocation, value.currencyCode, value.amount);
    }

    /**
     * Create a new Edit from arguments
     */
    public Edit(long id, long transactionTime, String transactionDescription, String transactionLocation,
                String transactionCurrency, long transactionAmount) {
        this.id = id;
        this.transactionTime = transactionTime;
        this.transactionDescription = transactionDescription;
        this.transactionLocation = transactionLocation;
        this.transactionCurrency = transactionCurrency;
        this.transactionAmount = transactionAmount;
    }

    /**
     * Return the Value that this Edit represents
     */
    public Value getValue() {
        return new Value(transactionCurrency, transactionAmount);
    }

    @Override
    public String toString() {
        Calendar ccal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
        ccal.setTimeInMillis(id);
        Calendar tcal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
        tcal.setTimeInMillis(transactionTime);
        return String.format("id: %tFT%<tTZ, transactionTime: %tFT%<tTZ, transactionDescription: %s, transactionLocation: %s, transactionCurrency: %s, transactionAmount: %d",
                ccal, tcal, transactionDescription, transactionLocation, transactionCurrency, transactionAmount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(transactionTime);
        dest.writeString(transactionDescription);
        dest.writeString(transactionLocation);
        dest.writeString(transactionCurrency);
        dest.writeLong(transactionAmount);
    }

    public static final Parcelable.Creator<Edit> CREATOR = new Parcelable.Creator<Edit>() {
        @Override
        public Edit createFromParcel(Parcel source) {
            return new Edit(source);
        }
        @Override
        public Edit[] newArray(int size) {
            return new Edit[size];
        }
    };

    private Edit(Parcel in) {
        id = in.readLong();
        transactionTime = in.readLong();
        transactionDescription = in.readString();
        transactionLocation = in.readString();
        transactionCurrency = in.readString();
        transactionAmount = in.readLong();
    }


}
