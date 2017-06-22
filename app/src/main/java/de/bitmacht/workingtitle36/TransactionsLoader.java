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

//TODO possibly implement onStopLoading and onCanceled
public class TransactionsLoader extends AsyncTaskLoader<ArrayList<TransactionsModel>> {

    public static final String ARG_PERIODS = "periods";

    private final DBHelper dbHelper;
    private Periods periods;
    private ArrayList<TransactionsModel> result;

    public TransactionsLoader(Context context, DBHelper dbHelper, Bundle args) {
        super(context);
        this.dbHelper = dbHelper;
        periods = args.getParcelable(ARG_PERIODS);
    }

    public TransactionsLoader(Context context, DBHelper dbHelper, Periods periods) {
        super(context);
        this.dbHelper = dbHelper;
        this.periods = periods;
    }

    public Periods getPeriods() {
        return periods;
    }

    @Override
    public ArrayList<TransactionsModel> loadInBackground() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long start = periods.getLongStart().getMillis();
        long end = periods.getLongEnd().getMillis();
        Cursor cursor = db.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY, new String[]{String.valueOf(start), String.valueOf(end)});

        ArrayList<TransactionsModel> result = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            result.add(TransactionsModel.Companion.getInstanceWithEdit(cursor));
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
