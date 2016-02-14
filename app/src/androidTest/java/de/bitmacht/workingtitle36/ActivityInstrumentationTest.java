package de.bitmacht.workingtitle36;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;

public class ActivityInstrumentationTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public ActivityInstrumentationTest() {
        super(MainActivity.class);
    }

    public void testDatabaseInserts() {
        DBHelper dbHelper = new DBHelper(getActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor res;

        db.execSQL("DELETE FROM " + DBHelper.EDITS_TABLE_NAME);
        res = db.rawQuery("SELECT * FROM " + DBHelper.EDITS_TABLE_NAME, null);
        // The table should be empty
        assertEquals(res.getCount(), 0);
        res.close();

        db.execSQL("DELETE FROM " + DBHelper.TRANSACTIONS_TABLE_NAME);
        res = db.rawQuery("SELECT * FROM " + DBHelper.TRANSACTIONS_TABLE_NAME, null);
        // The table should be empty
        assertEquals(res.getCount(), 0);
        res.close();

        // transactions (ctime, isremoved)
        // edits (ctime, parent, transact, sequence, ispending, ttime, tdesc, tloc, tcurrency, tvalue)

        db.execSQL("INSERT INTO " + DBHelper.TRANSACTIONS_TABLE_NAME + " VALUES (1448274596500, 0)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448275556167, 1448275556167, 1448274596500, 0, 0, 1448272456167, 'the mayor''s favor', 'geo:52.518611,13.408333', 'EUR', -666)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448275566167, 1448275556167, 1448274596500, 1, 0, 1448272456167, 'the mayor''s favor is cheap', 'geo:52.518611,13.408333', 'EUR', -66)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448275576167, 1448275566167, 1448274596500, 2, 0, 1448272456167, 'the mayor''s favor is cheaper', 'geo:52.518611,13.408333', 'EUR', -6)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448275586167, 1448275566167, 1448274596500, 3, 1, 1448272456167, 'the mayor''s favor isn''t that cheap', 'geo:52.518611,13.408333', 'EUR', -6666)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448275596167, 1448275566167, 1448274596500, 4, 0, 1448272456167, 'mayor''s favor\nis broken', 'geo:52.518611,13.408333', 'EUR', 4242)");

        // this transaction is removed
        db.execSQL("INSERT INTO " + DBHelper.TRANSACTIONS_TABLE_NAME + " VALUES (1448287119924, 1)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448287119924, 1448287119924, 1448287119924, 0, 0, 1448287119924, 'big spender', 'geo:52.518611,13.408333', 'EUR', -234)");

        // only one pending edit
        db.execSQL("INSERT INTO " + DBHelper.TRANSACTIONS_TABLE_NAME + " VALUES (1448287249137, 0)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448287249137, 1448287249137, 1448287249137, 0, 1, 1448287049137, 'not quite sure yet', 'geo:52.518611,13.408333', 'EUR', -10000)");

        // we can also receive an amount
        db.execSQL("INSERT INTO " + DBHelper.TRANSACTIONS_TABLE_NAME + " VALUES (1448287420716, 0)");
        db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                "(1448287420716, 1448287420716, 1448287420716, 0, 0, 1448287420716, 'collections', 'geo:52.5186,13.3763', 'EUR', 8800)");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                // this should fail (foreign key)
                db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                        "(1448287420717, 1448287420717, 1448287420715, 0, 0, 1448287420716, 'collections', 'geo:52.5186,13.3763', 'EUR', 8800)");
                fail("INSERT should have failed due to the foreign key constraint");
            } catch (SQLiteConstraintException e) {
                // expected
            }
        }

        try {
            // this should fail (primary key not unique)
            db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                    "(1448287420716, 1448287420716, 1448287420716, 1, 0, 1448287420716, 'collections', 'geo:52.5186,13.3763', 'EUR', 8800)");
            fail("INSERT should have failed due to the primary key constraint");
        } catch (SQLException e) {
            // expected
        }

        try {
            // this should fail (an edit with this sequence number already exists in this transaction)
            db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                    "(1448287420718, 1448287420716, 1448287420716, 0, 0, 1448287420716, 'collections', 'geo:52.5186,13.3763', 'EUR', 8800)");
            fail("INSERT should have failed due to the unique constraint");
        } catch (SQLException e) {
            // expected
        }

        // bulk insert
        final int expenseCount = 100;
        long now = System.currentTimeMillis();

        for (int i = 1; i <= expenseCount; i++) {
            long expenseTime = now - (expenseCount - i) * 13 * 60 * 1000;
            long value = (i - (expenseCount/2)) * 23;
            db.execSQL("INSERT INTO " + DBHelper.TRANSACTIONS_TABLE_NAME + " VALUES (" + expenseTime + ", 0)");
            db.execSQL("INSERT INTO " + DBHelper.EDITS_TABLE_NAME + " VALUES " +
                    "(" + expenseTime + ", " + expenseTime + ", " + expenseTime + ", 0, 0, " + expenseTime + ", 'expense no. " + i + "', 'geo:52.518611,13.408333', 'EUR', " + value + ")");
        }

    }
}
