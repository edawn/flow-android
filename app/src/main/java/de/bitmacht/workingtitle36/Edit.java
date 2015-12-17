package de.bitmacht.workingtitle36;

import android.database.Cursor;

/**
 * This describes an edit.
 * It relates to one row in the edits table.
 */
public class Edit {

    public static final String[] PROJECTION = {
            DBHelper.EDITS_KEY_CREATION_TIME,
            DBHelper.EDITS_KEY_TRANSACTION_TIME,
            DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION,
            DBHelper.EDITS_KEY_TRANSACTION_LOCATION,
            DBHelper.EDITS_KEY_TRANSACTION_CURRENCY,
            DBHelper.EDITS_KEY_TRANSACTION_VALUE
    };

    private long ctime;
    private long ttime;
    private String tdesc;
    private String tloc;
    private String tcurrency;
    private long tvalue;

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
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_VALUE)));
    }

    /**
     * Create a new Edit from arguments
     */
    public Edit(long ctime, long ttime, String tdesc, String tloc, String tcurrency, long tvalue) {
        this.ctime = ctime;
        this.ttime = ttime;
        this.tdesc = tdesc;
        this.tloc = tloc;
        this.tcurrency = tcurrency;
        this.tvalue = tvalue;
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

    public long getTvalue() {
        return tvalue;
    }
}
