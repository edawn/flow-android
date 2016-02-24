package de.bitmacht.workingtitle36;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "flow";
    public static final int DATABASE_VERSION = 1;

    /**
     * This table contains all edits. Once created, an edit is never modified.
     * Additionally an edit is never removed, unless the transaction and all edits belonging to it are removed as well.
     */
    public static final String EDITS_TABLE_NAME = "edits";
    /**
     * The creation time of this edit; also serving as the primary key of this table;
     * in ms since the epoch; if an entry with this key should already exist, increment by one and retry
     */
    public static final String EDITS_KEY_CREATION_TIME = "ctime";
    /**
     * The ctime of the parent that this entry has been derived from;
     * if this is the first edit in a transaction then this is this edit's ctime
     */
    public static final String EDITS_KEY_PARENT = "parent";
    /**
     * The ctime of the transaction this edit belongs to;
     * the referenced transaction must exist
     */
    public static final String EDITS_KEY_TRANSACTION = "transact";
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
    public static final String EDITS_KEY_ISPENDING = "ispending";
    /**
     * The user-set time of the transaction; in ms since the epoch
     */
    public static final String EDITS_KEY_TRANSACTION_TIME = "ttime";
    /**
     * The user-set description for this transaction
     */
    public static final String EDITS_KEY_TRANSACTION_DESCRIPTION = "tdesc";
    /**
     * The user-set location that this transaction took place;
     * preferably as geo URI (https://en.wikipedia.org/wiki/Geo_URI_scheme)
     */
    public static final String EDITS_KEY_TRANSACTION_LOCATION = "tloc";
    /**
     * The user-set currency for this transaction; a ISO 4217 currency code
     */
    public static final String EDITS_KEY_TRANSACTION_CURRENCY = "tcurrency";
    /**
     * The user-set transaction value; in minor currency units (usually cents); negative values indicate spending
     */
    public static final String EDITS_KEY_TRANSACTION_VALUE = "tvalue";

    /**
     * This table contains all transactions.
     */
    public static final String TRANSACTIONS_TABLE_NAME = "transactions";
    /**
     * The creation time of this transaction; also serving as the primary key of this table;
     * in ms since the epoch; if an entry with this key should already exist, increment by one and retry
     */
    public static final String TRANSACTIONS_KEY_CREATION_TIME = "ctime";
    /**
     * Indicates that this transaction has been removed and should not be processed any further
     */
    public static final String TRANSACTIONS_KEY_ISREMOVED = "isremoved";

    public static final String EDITS_TABLE_CREATE =
            "CREATE TABLE " + EDITS_TABLE_NAME + "(" +
                    EDITS_KEY_CREATION_TIME + " INTEGER PRIMARY KEY, " +
                    EDITS_KEY_PARENT + " INTEGER REFERENCES " + EDITS_TABLE_NAME + ", " +
                    EDITS_KEY_TRANSACTION + " INTEGER, " +
                    EDITS_KEY_SEQUENCE + " INTEGER, " +
                    EDITS_KEY_ISPENDING + " BOOLEAN, " +
                    EDITS_KEY_TRANSACTION_TIME + " INTEGER, " +
                    EDITS_KEY_TRANSACTION_DESCRIPTION + " TEXT, " +
                    EDITS_KEY_TRANSACTION_LOCATION + " TEXT, " +
                    EDITS_KEY_TRANSACTION_CURRENCY + " TEXT, " +
                    EDITS_KEY_TRANSACTION_VALUE + " INTEGER, " +
                    "FOREIGN KEY(" + EDITS_KEY_TRANSACTION + ") REFERENCES " + TRANSACTIONS_TABLE_NAME + "(" + TRANSACTIONS_KEY_CREATION_TIME + "), " +
                    "UNIQUE(" + EDITS_KEY_TRANSACTION + ", " + EDITS_KEY_SEQUENCE + ")" +
                    ");";

    public static final String TRANSACTIONS_TABLE_CREATE =
            "CREATE TABLE " + TRANSACTIONS_TABLE_NAME + "(" +
                    TRANSACTIONS_KEY_CREATION_TIME + " INTEGER PRIMARY KEY, " +
                    TRANSACTIONS_KEY_ISREMOVED + " BOOLEAN" +
                    ");";

    /**
     * This table contains the regular transactions
     */
    public static final String REGULARS_TABLE_NAME = "regulars";

    /**
     * The creation time of this transaction; also serving as the primary key of this table;
     * in ms since the epoch; if an entry with this key should already exist, increment by one and retry
     */
    public static final String REGULARS_KEY_CREATION_TIME = "creation_time";
    /**
     * The time this transaction will be executed for the first time;
     * in ms since the epoch
     */
    public static final String REGULARS_KEY_TIME_FIRST = "time_first";
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
     * Do not use this transaction when a new accounting period starts
     */
    public static final String REGULARS_KEY_IS_DISABLED = "is_disabled";
    /**
     * This regular transaction is virtually removed; should be used if this transaction is (still)
     * referenced in transactions_regular
     */
    public static final String REGULARS_KEY_IS_DELETED = "is_deleted";
    /**
     * The user-set description for this transaction
     */
    public static final String REGULARS_KEY_DESCRIPTION = "description";
    /**
     * The user-set currency for this transaction; a ISO 4217 currency code
     */
    public static final String REGULARS_KEY_CURRENCY = "currency";
    /**
     * The user-set transaction value; in minor currency units (usually cents); negative values indicate spending
     */
    public static final String REGULARS_KEY_VALUE = "value";

    public static final String REGULARS_TABLE_CREATE =
            "CREATE TABLE " + REGULARS_TABLE_NAME + "(" +
                    REGULARS_KEY_CREATION_TIME + " INTEGER PRIMARY KEY, " +
                    REGULARS_KEY_TIME_FIRST + " INTEGER, " +
                    REGULARS_KEY_PERIOD_TYPE + " INTEGER, " +
                    REGULARS_KEY_PERIOD_MULTIPLIER + " INTEGER, " +
                    REGULARS_KEY_IS_SPREAD + " BOOLEAN, " +
                    REGULARS_KEY_IS_DISABLED + " BOOLEAN, " +
                    REGULARS_KEY_IS_DELETED + " BOOLEAN, " +
                    REGULARS_KEY_DESCRIPTION + " TEXT, " +
                    REGULARS_KEY_CURRENCY + " TEXT, " +
                    REGULARS_KEY_VALUE + " INTEGER);";

    /**
     * Holds the already-executed regular transactions
     */
    public static final String TRANSACTIONS_REGULAR_TABLE_NAME = "transactions_regular";

    /**
     * The time at which the referenced regular transaction has been executed;
     * in ms since the epoch
     */
    public static final String TRANSACTIONS_REGULAR_KEY_EXECUTION_TIME = "execution_time";
    /**
     * The creation time (i.e. id) of the referenced regular transaction
     */
    public static final String TRANSACTIONS_REGULAR_KEY_REGULAR_ID = "regular_id";

    public static final String TRANSACTIONS_REGULAR_TABLE_CREATE =
            "CREATE TABLE " + TRANSACTIONS_REGULAR_TABLE_NAME + "(" +
                    TRANSACTIONS_REGULAR_KEY_REGULAR_ID + " INTEGER REFERENCES " + REGULARS_TABLE_NAME + " (" + REGULARS_KEY_CREATION_TIME + "), " +
                    TRANSACTIONS_REGULAR_KEY_EXECUTION_TIME + " INTEGER);";

    /**
     * Returns the most current, not pending edit for every transaction
     * TODO do not return edits from deleted transactions
     */
    public static final String LATEST_EDITS_QUERY =
            "SELECT " + EDITS_TABLE_NAME + ".* FROM " + EDITS_TABLE_NAME + " INNER JOIN " +
                    "(SELECT " + EDITS_KEY_CREATION_TIME + ", MAX(" + EDITS_KEY_SEQUENCE + ") AS maxsequence " +
                    "FROM " + EDITS_TABLE_NAME + " WHERE NOT " + EDITS_KEY_ISPENDING + " GROUP BY " +
                    EDITS_KEY_TRANSACTION + ") editsmax ON " + EDITS_TABLE_NAME + "." + EDITS_KEY_CREATION_TIME +
                    " = editsmax." + EDITS_KEY_CREATION_TIME + " ORDER BY " + EDITS_KEY_TRANSACTION_TIME;

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
        db.execSQL(TRANSACTIONS_REGULAR_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException("I don't like change.");
    }
}
