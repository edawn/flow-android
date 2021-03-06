/*
 * Copyright 2016 Kamil Sartys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bitmacht.workingtitle36;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.util.ArrayList;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "flow";
    public static final int DATABASE_VERSION = 1;

    /**
     * This table contains all transactions.
     */
    public static final String TRANSACTIONS_TABLE_NAME = "transactions";
    /**
     * The id of this transaction
     */
    public static final String TRANSACTIONS_KEY_ID = "id";
    /**
     * Indicates that this transaction has been removed and should not be processed any further
     */
    public static final String TRANSACTIONS_KEY_IS_REMOVED = "is_removed";

    public static final String TRANSACTIONS_TABLE_CREATE =
            "CREATE TABLE " + TRANSACTIONS_TABLE_NAME + "(" +
                    TRANSACTIONS_KEY_ID + " INTEGER PRIMARY KEY, " +
                    TRANSACTIONS_KEY_IS_REMOVED + " BOOLEAN" +
                    ");";

    /**
     * This table contains all edits. Once created, an edit is never modified.
     * Additionally an edit is never removed, unless the transaction and all edits belonging to it are removed as well.
     */
    public static final String EDITS_TABLE_NAME = "edits";
    /**
     * The id of this edit
     */
    public static final String EDITS_KEY_ID = "id";
    /**
     * The id of the parent that this entry has been derived from;
     * if this is the first edit in a transaction then this is NULL
     */
    public static final String EDITS_KEY_PARENT = "parent";
    /**
     * The id of the transaction that this edit belongs to;
     * the referenced transaction must exist
     * Note: "transaction" is an sqlite keyword
     */
    public static final String EDITS_KEY_TRANSACTION = "transaktion";
    /**
     * This identifies an edit in a transaction; for the first edit this is zero;
     * a new edit gets the highest sequence number of any edit in the related transaction, incremented by one;
     * the edit with the highest sequence number which is not pending is used for the calculation of the flow
     */
    public static final String EDITS_KEY_SEQUENCE = "sequence";
    /**
     * Indicate that the user has not finished editing this entry; this may happen when the app is put
     * in the background before the user clicks "accept" or the like
     */
    public static final String EDITS_KEY_IS_PENDING = "is_pending";
    /**
     * The user-set time of the transaction; in ms since the epoch
     */
    public static final String EDITS_KEY_TRANSACTION_TIME = "transaction_time";
    /**
     * The user-set description for this transaction
     */
    public static final String EDITS_KEY_TRANSACTION_DESCRIPTION = "transaction_description";
    /**
     * The user-set location that this transaction took place;
     * preferably as geo URI (https://en.wikipedia.org/wiki/Geo_URI_scheme)
     */
    public static final String EDITS_KEY_TRANSACTION_LOCATION = "transaction_location";
    /**
     * The user-set currency for this transaction; a ISO 4217 currency code
     */
    public static final String EDITS_KEY_TRANSACTION_CURRENCY = "transaction_currency";
    /**
     * The user-set transaction amount in minor currency units (usually cents); negative values indicate spending
     */
    public static final String EDITS_KEY_TRANSACTION_AMOUNT = "transaction_amount";

    public static final String EDITS_TABLE_CREATE =
            "CREATE TABLE " + EDITS_TABLE_NAME + "(" +
                    EDITS_KEY_ID + " INTEGER PRIMARY KEY, " +
                    EDITS_KEY_PARENT + " INTEGER REFERENCES " + EDITS_TABLE_NAME + ", " +
                    EDITS_KEY_TRANSACTION + " INTEGER, " +
                    EDITS_KEY_SEQUENCE + " INTEGER, " +
                    EDITS_KEY_IS_PENDING + " BOOLEAN, " +
                    EDITS_KEY_TRANSACTION_TIME + " INTEGER, " +
                    EDITS_KEY_TRANSACTION_DESCRIPTION + " TEXT, " +
                    EDITS_KEY_TRANSACTION_LOCATION + " TEXT, " +
                    EDITS_KEY_TRANSACTION_CURRENCY + " TEXT, " +
                    EDITS_KEY_TRANSACTION_AMOUNT + " INTEGER, " +
                    "FOREIGN KEY(" + EDITS_KEY_TRANSACTION + ") REFERENCES " + TRANSACTIONS_TABLE_NAME + "(" + TRANSACTIONS_KEY_ID + "), " +
                    "UNIQUE(" + EDITS_KEY_TRANSACTION + ", " + EDITS_KEY_SEQUENCE + ")" +
                    ");";

    /**
     * This table contains the regular transactions
     */
    public static final String REGULARS_TABLE_NAME = "regulars";

    /**
     * The id of this transaction
     */
    public static final String REGULARS_KEY_ID = "id";
    /**
     * The time this transaction will be executed for the first time;
     * in ms since the epoch; including
     */
    public static final String REGULARS_KEY_TIME_FIRST = "time_first";
    /**
     * The time this transaction will be executed for the last time;
     * in ms since the epoch; excluding; a negative value indicates that this transaction will happen indefinitely
     */
    public static final String REGULARS_KEY_TIME_LAST = "time_last";
    /**
     * Defines the way this transaction is supposed to repeat;
     * 0: daily 1: monthly
     */
    public static final String REGULARS_KEY_PERIOD_TYPE = "period_type";
    /**
     * A multiplier for the period; useful to create weekly, quarterly or yearly expenses for instance
     */
    public static final String REGULARS_KEY_PERIOD_MULTIPLIER = "period_multiplier";
    /**
     * Spread this transaction across multiple accounting periods (this makes only sense
     * if it overlays multiple accounting periods)
     */
    public static final String REGULARS_KEY_IS_SPREAD = "is_spread";
    /**
     * Do not use this transaction when calculating the balance
     */
    public static final String REGULARS_KEY_IS_DISABLED = "is_disabled";
    /**
     * The user-set description for this transaction
     */
    public static final String REGULARS_KEY_DESCRIPTION = "description";
    /**
     * The user-set currency for this transaction; a ISO 4217 currency code
     */
    public static final String REGULARS_KEY_CURRENCY = "currency";
    /**
     * The user-set transaction amount; in minor currency units (usually cents); negative values indicate spending
     */
    public static final String REGULARS_KEY_AMOUNT = "amount";

    /**
     * Relates to {@link DBHelper#REGULARS_KEY_PERIOD_TYPE}; this transactions will be executed every day
     */
    public static final int REGULARS_PERIOD_TYPE_DAILY = 0;
    /**
     * Relates to {@link DBHelper#REGULARS_KEY_PERIOD_TYPE}; this transactions will be executed every month
     */
    public static final int REGULARS_PERIOD_TYPE_MONTHLY = 1;

    public static final String REGULARS_TABLE_CREATE =
            "CREATE TABLE " + REGULARS_TABLE_NAME + "(" +
                    REGULARS_KEY_ID + " INTEGER PRIMARY KEY, " +
                    REGULARS_KEY_TIME_FIRST + " INTEGER, " +
                    REGULARS_KEY_TIME_LAST + " INTEGER, " +
                    REGULARS_KEY_PERIOD_TYPE + " INTEGER, " +
                    REGULARS_KEY_PERIOD_MULTIPLIER + " INTEGER, " +
                    REGULARS_KEY_IS_SPREAD + " BOOLEAN, " +
                    REGULARS_KEY_IS_DISABLED + " BOOLEAN, " +
                    REGULARS_KEY_DESCRIPTION + " TEXT, " +
                    REGULARS_KEY_CURRENCY + " TEXT, " +
                    REGULARS_KEY_AMOUNT + " INTEGER);";

    /**
     * Returns the most current, not pending edit for every transaction
     */
    public static final String LATEST_EDITS_QUERY =
            "SELECT " + EDITS_TABLE_NAME + ".* FROM " + EDITS_TABLE_NAME + " INNER JOIN " +
                    "(SELECT " + EDITS_KEY_ID + ", MAX(" + EDITS_KEY_SEQUENCE + ") AS maxsequence " +
                    "FROM " + EDITS_TABLE_NAME + " WHERE NOT " + EDITS_KEY_IS_PENDING + " GROUP BY " +
                    EDITS_KEY_TRANSACTION + ") editsmax ON " + EDITS_TABLE_NAME + "." + EDITS_KEY_ID +
                    " = editsmax." + EDITS_KEY_ID + " ORDER BY " + EDITS_KEY_TRANSACTION_TIME;

    /**
     * Returns the most current, not removed transaction/edit join for every transaction
     */
    public static final String TRANSACTIONS_EDITS_QUERY =
            "SELECT " + EDITS_TABLE_NAME + ".*, " + TRANSACTIONS_KEY_IS_REMOVED + " FROM " + EDITS_TABLE_NAME + " INNER JOIN " +
                    "(SELECT " + TRANSACTIONS_KEY_IS_REMOVED +
                    ", " + EDITS_TABLE_NAME + "." + EDITS_KEY_ID + ", MAX(" + EDITS_KEY_SEQUENCE + ") AS maxsequence " +
                    "FROM " + TRANSACTIONS_TABLE_NAME + ", " + EDITS_TABLE_NAME + " WHERE " + TRANSACTIONS_TABLE_NAME + "." + TRANSACTIONS_KEY_ID +
                    " = " + EDITS_KEY_TRANSACTION + " AND NOT " + TRANSACTIONS_KEY_IS_REMOVED + " GROUP BY " + EDITS_KEY_TRANSACTION + ") editsmax ON " +
                    EDITS_TABLE_NAME + "." + EDITS_KEY_ID + " = editsmax." + EDITS_KEY_ID +
                    " ORDER BY " + EDITS_KEY_TRANSACTION_TIME;

    /**
     * Returns the most current, not removed transaction/edit join for every transaction in a defined time span
     */
    public static final String TRANSACTIONS_EDITS_TIME_SPAN_QUERY =
            "SELECT " + EDITS_TABLE_NAME + ".*, " + TRANSACTIONS_KEY_IS_REMOVED + " FROM " + EDITS_TABLE_NAME + " INNER JOIN " +
                    "(SELECT " + TRANSACTIONS_KEY_IS_REMOVED +
                    ", " + EDITS_TABLE_NAME + "." + EDITS_KEY_ID + ", MAX(" + EDITS_KEY_SEQUENCE + ") AS maxsequence " +
                    "FROM " + TRANSACTIONS_TABLE_NAME + ", " + EDITS_TABLE_NAME + " WHERE " + TRANSACTIONS_TABLE_NAME + "." + TRANSACTIONS_KEY_ID +
                    " = " + EDITS_KEY_TRANSACTION + " AND NOT " + TRANSACTIONS_KEY_IS_REMOVED +
                    " AND " + EDITS_KEY_TRANSACTION_TIME + " >= ? AND " + EDITS_KEY_TRANSACTION_TIME + " < ? " +
                    " GROUP BY " + EDITS_KEY_TRANSACTION + ") editsmax ON " +
                    EDITS_TABLE_NAME + "." + EDITS_KEY_ID + " = editsmax." + EDITS_KEY_ID +
                    " ORDER BY " + EDITS_KEY_TRANSACTION_TIME;

    /**
     * Query a transaction by id
     */
    public static final String TRANSACTION_QUERY =
            "SELECT * FROM " + TRANSACTIONS_TABLE_NAME + " WHERE " + TRANSACTIONS_KEY_ID + " = ?";

    /**
     * Returns distinct fields and their frequency in the given column selected from
     * the most current, not pending edit for every transaction
     */
    public static final String SUGGESTIONS_QUERY =
            "SELECT TRIM(%s) AS result, COUNT(*) FROM " + EDITS_TABLE_NAME + " INNER JOIN " +
                    "(SELECT " + EDITS_KEY_ID + ", MAX(" + EDITS_KEY_SEQUENCE + ") AS maxsequence " +
                    "FROM " + EDITS_TABLE_NAME + " WHERE NOT " + EDITS_KEY_IS_PENDING + " GROUP BY " +
                    EDITS_KEY_TRANSACTION + ") editsmax ON " + EDITS_TABLE_NAME + "." + EDITS_KEY_ID +
                    " = editsmax." + EDITS_KEY_ID + " GROUP BY result ORDER BY result";

    /**
     * Returns all edits for a transaction (selected by the id of its associated transaction id)
     */
    public static final String EDITS_FOR_TRANSACTION_QUERY =
            "SELECT * FROM " + EDITS_TABLE_NAME + " WHERE " + EDITS_KEY_TRANSACTION + " = ?";

    /**
     * Returns all regular transactions
     */
    public static final String REGULARS_QUERY =
            "SELECT * FROM " + REGULARS_TABLE_NAME;

    /**
     * Returns all active regular transactions
     */
    public static final String ACTIVE_REGULARS_QUERY =
            "SELECT * FROM " + REGULARS_TABLE_NAME + " WHERE NOT " + REGULARS_KEY_IS_DISABLED;

     public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TRANSACTIONS_TABLE_CREATE);
        db.execSQL(EDITS_TABLE_CREATE);
        db.execSQL(REGULARS_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException("I don't like change.");
    }

    /**
     * Query the database for active regular transactions
     * @see DBHelper#ACTIVE_REGULARS_QUERY
     */
    public static ArrayList<RegularModel> queryRegulars(DBHelper dbHelper) {
        return queryRegulars(dbHelper, false);
    }

    /**
     * Query the database for regular transactions
     * @see DBHelper#ACTIVE_REGULARS_QUERY
     */
    public static ArrayList<RegularModel> queryRegulars(DBHelper dbHelper, boolean includeDisabled) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(includeDisabled ? REGULARS_QUERY : ACTIVE_REGULARS_QUERY, null);

        ArrayList<RegularModel> regulars = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            regulars.add(new RegularModel(cursor));
        }
        cursor.close();
        return regulars;
    }
}
