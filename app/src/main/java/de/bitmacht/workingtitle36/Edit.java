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

    public static final String[] PROJECTION = {
            DBHelper.EDITS_KEY_CREATION_TIME,
            DBHelper.EDITS_KEY_TRANSACTION_TIME,
            DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION,
            DBHelper.EDITS_KEY_TRANSACTION_LOCATION,
            DBHelper.EDITS_KEY_TRANSACTION_CURRENCY,
            DBHelper.EDITS_KEY_TRANSACTION_AMOUNT
    };

    private long ctime;
    private long ttime;
    private String tdesc;
    private String tloc;
    private String tcurrency;
    private long tamount;

    /**
     * Create a new Edit from a cursor
     * @throws IllegalArgumentException If a required column is missing
     */
    public Edit(Cursor cursor) {
        this(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_CREATION_TIME)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_LOCATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT)));
    }

    public Edit(long ctime, long ttime, String tdesc, String tloc, Value value) {
        this(ctime, ttime, tdesc, tloc, value.currencyCode, value.amount);
    }

    /**
     * Create a new Edit from arguments
     */
    public Edit(long ctime, long ttime, String tdesc, String tloc, String tcurrency, long tamount) {
        this.ctime = ctime;
        this.ttime = ttime;
        this.tdesc = tdesc;
        this.tloc = tloc;
        this.tcurrency = tcurrency;
        this.tamount = tamount;
    }

    public long getCtime() {
        return ctime;
    }

    public long getTtime() {
        return ttime;
    }

    public String getTdesc() {
        return tdesc;
    }

    public String getTloc() {
        return tloc;
    }

    public String getTcurrency() {
        return tcurrency;
    }

    public long getTamount() {
        return tamount;
    }

    /**
     * Return the Value that this Edit represents
     */
    public Value getValue() {
        return new Value(tcurrency, tamount);
    }

    @Override
    public String toString() {
        Calendar ccal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
        ccal.setTimeInMillis(ctime);
        Calendar tcal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
        tcal.setTimeInMillis(ttime);
        return String.format("ctime: %tFT%<tTZ, ttime: %tFT%<tTZ, tdesc: %s, tloc: %s, tcurrency: %s, tamount: %d",
                ccal, tcal, tdesc, tloc, tcurrency, tamount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(ctime);
        dest.writeLong(ttime);
        dest.writeString(tdesc);
        dest.writeString(tloc);
        dest.writeString(tcurrency);
        dest.writeLong(tamount);
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
        ctime = in.readLong();
        ttime = in.readLong();
        tdesc = in.readString();
        tloc = in.readString();
        tcurrency = in.readString();
        tamount = in.readLong();
    }


}
