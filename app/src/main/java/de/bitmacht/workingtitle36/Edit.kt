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

package de.bitmacht.workingtitle36

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import java.util.*

/**
 * This describes an edit.
 * It relates to one row in the edits table.
 */
class Edit private constructor(
        val id: Long? = null,
        val parent: Long? = null,
        val transaction: Long? = null,
        val sequence: Int? = null,
        val transactionTime: Long = 0,
        val transactionDescription: String,
        val transactionLocation: String,
        val transactionCurrency: String,
        val transactionAmount: Long = 0) : Parcelable {

    /**
     * Create a new Edit from a cursor
     * @throws IllegalArgumentException If a required column is missing
     */
    constructor(cursor: Cursor) : this(
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_ID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_PARENT)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_SEQUENCE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_TIME)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_LOCATION)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT)))

    constructor(parent: Long?, transaction: Long?, transactionTime: Long, transactionDescription: String, transactionLocation: String, value: Value) :
            this(null, parent, transaction, null, transactionTime, transactionDescription, transactionLocation, value.currencyCode, value.amount)

    /**
     * Return the Value that this Edit represents
     */
    val value: Value
        get() = Value(transactionCurrency, transactionAmount)

    override fun toString(): String {
        val time = "%tFT%<tTZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z")).apply { timeInMillis = transactionTime })
        return "id: $id, par: $parent, tr: $transaction, seq: $sequence, time: $time, desc: $transactionDescription, " +
                "loc: $transactionLocation, cur: $transactionCurrency, am: $transactionAmount"
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeValue(parent)
        writeValue(transaction)
        writeValue(sequence)
        writeLong(transactionTime)
        writeString(transactionDescription)
        writeString(transactionLocation)
        writeString(transactionCurrency)
        writeLong(transactionAmount)
    }

    private constructor(src: Parcel) : this(
            src.readValue(Long::class.java.classLoader) as Long?,
            src.readValue(Long::class.java.classLoader) as Long?,
            src.readValue(Long::class.java.classLoader) as Long?,
            src.readValue(Int::class.java.classLoader) as Int?,
            src.readLong(),
            src.readString(),
            src.readString(),
            src.readString(),
            src.readLong())

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<Edit> = object : Parcelable.Creator<Edit> {
            override fun createFromParcel(source: Parcel): Edit = Edit(source)
            override fun newArray(size: Int): Array<Edit?> = arrayOfNulls(size)
        }
    }


}
