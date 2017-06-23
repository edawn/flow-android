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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import de.bitmacht.workingtitle36.DBHelper;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class InsertTest {

    @Test
    public void insert() {
        Random rnd = new Random(0);
        DateTime now = DateTime.now();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DBHelper dbHelper = new DBHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            Cursor res;

            db.execSQL("DELETE FROM " + DBHelper.Companion.getEDITS_TABLE_NAME());
            res = db.rawQuery("SELECT * FROM " + DBHelper.Companion.getEDITS_TABLE_NAME(), null);
            // The table should be empty
            assertEquals(0, res.getCount());
            res.close();

            db.execSQL("DELETE FROM " + DBHelper.Companion.getTRANSACTIONS_TABLE_NAME());
            res = db.rawQuery("SELECT * FROM " + DBHelper.Companion.getTRANSACTIONS_TABLE_NAME(), null);
            // The table should be empty
            assertEquals(0, res.getCount());
            res.close();

            db.execSQL("DELETE FROM " + DBHelper.Companion.getREGULARS_TABLE_NAME());
            res = db.rawQuery("SELECT * FROM " + DBHelper.Companion.getREGULARS_TABLE_NAME(), null);
            // The table should be empty
            assertEquals(0, res.getCount());
            res.close();

            // transactions (id, is_removed)
            // edits (id, parent, transaktion, sequence, is_pending, transaction_time, transaction_description, transaction_location, transaction_currency, transaction_amount)

            for (int i = 0; i < 100; i++) {

                db.beginTransaction();
                try {
                    ContentValues cv = new ContentValues(10);
                    cv.put(DBHelper.Companion.getTRANSACTIONS_KEY_IS_REMOVED(), false);
                    long transactionId = db.insertOrThrow(DBHelper.Companion.getTRANSACTIONS_TABLE_NAME(), null, cv);

                    cv.clear();

                    cv.put(DBHelper.Companion.getEDITS_KEY_PARENT(), (Long) null);
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION(), transactionId);
                    cv.put(DBHelper.Companion.getEDITS_KEY_SEQUENCE(), 0);
                    cv.put(DBHelper.Companion.getEDITS_KEY_IS_PENDING(), false);
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION_TIME(), now.getMillis() + rnd.nextInt());
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION_DESCRIPTION(), "hello world");
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION_LOCATION(), "planet earth");
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION_CURRENCY(), "EUR");
                    cv.put(DBHelper.Companion.getEDITS_KEY_TRANSACTION_AMOUNT(), ((int) (rnd.nextGaussian() * 10000)));
                    db.insertOrThrow(DBHelper.Companion.getEDITS_TABLE_NAME(), null, cv);

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            db.close();
        }
    }
}
