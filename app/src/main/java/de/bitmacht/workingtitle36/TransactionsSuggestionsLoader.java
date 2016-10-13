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
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;

import java.util.ArrayList;

public class TransactionsSuggestionsLoader<T extends ListAdapter & Filterable> extends AsyncTaskLoader<T> {

    public static final String ARG_COLUMN = "column";

    /**
     * A valid value for {@link TransactionsSuggestionsLoader#ARG_COLUMN}
     */
    public static final String COLUMN_DESCRIPTION = DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION;
    /**
     * A valid value for {@link TransactionsSuggestionsLoader#ARG_COLUMN}
     */
    public static final String COLUMN_LOCATION = DBHelper.EDITS_KEY_TRANSACTION_LOCATION;

    private Context context;
    private final DBHelper dbHelper;
    public final String column;
    private T result;

    public TransactionsSuggestionsLoader(Context context, DBHelper dbHelper, Bundle args) {
        super(context);
        this.context = context;
        this.dbHelper = dbHelper;

        column = args.getString(ARG_COLUMN);
        if (!column.equals(COLUMN_DESCRIPTION) && !column.equals(COLUMN_LOCATION)) {
            throw new IllegalArgumentException("Unknown column name: " + column);
        }
    }

    @Override
    public T loadInBackground() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(String.format(DBHelper.SUGGESTIONS_QUERY, column), null);

        ArrayList<String> resultArray = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            FrequencyString fs = new FrequencyString(cursor);
            if (fs.text.length() == 0) {
                continue;
            }
            resultArray.add(fs.text);
        }

        cursor.close();

        return (T) new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, resultArray);
    }

    @Override
    public void deliverResult(T result) {
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

    public static class FrequencyString implements Comparable<FrequencyString> {
        public final String text;
        public final int count;

        FrequencyString(Cursor cursor) {
            text = cursor.getString(0);
            count = cursor.getInt(1);
        }

        @Override
        public int compareTo(FrequencyString o) {
            return text.compareTo(o.text);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FrequencyString && text.equals(((FrequencyString)obj).text);
        }
    }
}
