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

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * This describes an edit.
 * It relates to one row in the edits table.
 */
public class Edit implements Parcelable {

    public Long id = null;
    public Long parent = null;
    public Long transaction = null;
    public Integer sequence = null;
    public long transactionTime;
    public String transactionDescription;
    public String transactionLocation;
    public String transactionCurrency;
    public long transactionAmount;

    /**
     * Create a new Edit from a cursor
     * @throws IllegalArgumentException If a required column is missing
     */
    public Edit(Cursor cursor) {
        this(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_ID)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_PARENT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_SEQUENCE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_LOCATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT)));
    }

    public Edit(Long parent, Long transaction, long transactionTime, String transactionDescription, String transactionLocation, Value value) {
        this(null, parent, transaction, null, transactionTime, transactionDescription, transactionLocation, value.currencyCode, value.amount);
    }

    /**
     * Create a new Edit from arguments
     */
    private Edit(Long id, Long parent, Long transaction, Integer sequence, long transactionTime, String transactionDescription, String transactionLocation,
                String transactionCurrency, long transactionAmount) {
        this.id = id;
        this.parent = parent;
        this.transaction = transaction;
        this.sequence = sequence;
        this.transactionTime = transactionTime;
        this.transactionDescription = transactionDescription;
        this.transactionLocation = transactionLocation;
        this.transactionCurrency = transactionCurrency;
        this.transactionAmount = transactionAmount;
    }

    /**
     * Return the Value that this Edit represents
     */
    public Value getValue() {
        return new Value(transactionCurrency, transactionAmount);
    }

    @Override
    public String toString() {
        Calendar tcal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
        tcal.setTimeInMillis(transactionTime);
        return String.format("id: %s, par: %s, tr: %s, seq: %s, time: %tFT%<tTZ, desc: %s, loc: %s, cur: %s, am: %d",
                id, parent, transaction, sequence, tcal, transactionDescription, transactionLocation, transactionCurrency, transactionAmount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeValue(parent);
        dest.writeValue(transaction);
        dest.writeValue(sequence);
        dest.writeLong(transactionTime);
        dest.writeString(transactionDescription);
        dest.writeString(transactionLocation);
        dest.writeString(transactionCurrency);
        dest.writeLong(transactionAmount);
    }

    public static final Parcelable.Creator<Edit> CREATOR = new Parcelable.Creator<Edit>() {
        @Override
        public Edit createFromParcel(Parcel source) {
            return new Edit(source);
        }
        @Override
        public Edit[] newArray(int size) {
            return new Edit[size];
        }
    };

    private Edit(Parcel in) {
        id = (Long) in.readValue(Long.class.getClassLoader());
        parent = (Long) in.readValue(Long.class.getClassLoader());
        transaction = (Long) in.readValue(Long.class.getClassLoader());
        sequence = (Integer) in.readValue(Integer.class.getClassLoader());
        transactionTime = in.readLong();
        transactionDescription = in.readString();
        transactionLocation = in.readString();
        transactionCurrency = in.readString();
        transactionAmount = in.readLong();
    }


}
