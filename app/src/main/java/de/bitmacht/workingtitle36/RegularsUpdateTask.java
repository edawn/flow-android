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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegularsUpdateTask extends DBModifyingAsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(RegularsUpdateTask.class);

    private final DBHelper dbHelper;
    private final RegularModel regular;

    public RegularsUpdateTask(@NonNull Context context, RegularModel regular) {
        super(context);
        dbHelper = new DBHelper(context);
        this.regular = regular;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.insertWithOnConflict(DBHelper.REGULARS_TABLE_NAME, null,
                        regular.toContentValues(new ContentValues(10)), SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.error("modifying database failed");
            }
            return false;
        }
        if (BuildConfig.DEBUG) {
            logger.trace("finished inserting");
        }
        return true;
    }
}
