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

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import java.util.ArrayList;

class TransactionsLoader extends AsyncTaskLoader<ArrayList<TransactionsModel>> {
    /**
     * The start of the interval for which the transactions shall be queried (including; in ms since the epoch; default: java.lang.Long.MIN_VALUE)
     */
    public static final String ARG_START = "interval_start";
    /**
     * The end of the interval for which the transactions shall be queried (excluding; in ms since the epoch; default: java.lang.Long.MAX_VALUE)
     */
    public static final String ARG_END = "interval_end";

    private final DBHelper dbHelper;
    private final Bundle args;
    private ArrayList<TransactionsModel> result;

    public TransactionsLoader(Context context, DBHelper dbHelper, Bundle args) {
        super(context);
        this.dbHelper = dbHelper;
        this.args = args;
    }

    @Override
    public ArrayList<TransactionsModel> loadInBackground() {
        long start = args.getLong(ARG_START, Long.MIN_VALUE);
        long end = args.getLong(ARG_END, Long.MAX_VALUE);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY, new String[]{String.valueOf(start), String.valueOf(end)});

        ArrayList<TransactionsModel> result = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            result.add(TransactionsModel.getInstanceWithEdit(cursor));
        }

        return result;
    }

    @Override
    public void deliverResult(ArrayList<TransactionsModel> result) {
        this.result = result;
        super.deliverResult(result);
    }

    @Override
    protected void onStartLoading() {
        if (result != null) {
            deliverResult(result);
        }
        if (takeContentChanged() || result == null) {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        result = null;
    }
}
