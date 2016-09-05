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
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

import de.bitmacht.workingtitle36.widget.WidgetAwareAsyncTask;

public class TransactionsDeleteTask extends WidgetAwareAsyncTask {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsDeleteTask.class);

    private final DBHelper dbHelper;
    private final WeakReference<UpdateFinishedCallback> callbackRef;
    private final TransactionsModel transaction;

    public TransactionsDeleteTask(@NonNull Context context, @Nullable UpdateFinishedCallback callback, @NonNull TransactionsModel transaction) {
        super(context);
        dbHelper = new DBHelper(context);
        callbackRef = new WeakReference<>(callback);
        this.transaction = transaction;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        SQLiteDatabase db;
        try {
            db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues cv = new ContentValues(2);

                db.insertWithOnConflict(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                        transaction.toContentValues(cv), SQLiteDatabase.CONFLICT_REPLACE);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                logger.error("modifying database failed", e);
            }
            return false;
        }

        if (BuildConfig.DEBUG) {
            logger.trace("finished inserting");
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        UpdateFinishedCallback callback = callbackRef.get();
        if (callback != null) {
            callback.onUpdateFinished(success);
        }
    }

    interface UpdateFinishedCallback {
        void onUpdateFinished(boolean success);
    }
}
