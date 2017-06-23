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

import java.util.Calendar
import java.util.TimeZone

/**
 * This describes an edit.
 * It relates to one row in the edits table.
 */
class Edit : Parcelable {

    var id: Long? = null
    var parent: Long? = null
    var transaction: Long? = null
    var sequence: Int? = null
    var transactionTime: Long = 0
    var transactionDescription: String
    var transactionLocation: String
    var transactionCurrency: String
    var transactionAmount: Long = 0

    /**
     * Create a new Edit from a cursor
     * @throws IllegalArgumentException If a required column is missing
     */
    constructor(cursor: Cursor) : this(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_ID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_PARENT)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION)),
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_SEQUENCE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_TIME)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_LOCATION)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT)))

    constructor(parent: Long?, transaction: Long?, transactionTime: Long, transactionDescription: String, transactionLocation: String, value: Value) : this(null, parent, transaction, null, transactionTime, transactionDescription, transactionLocation, value.currencyCode, value.amount)

    /**
     * Create a new Edit from arguments
     */
    private constructor(id: Long?, parent: Long?, transaction: Long?, sequence: Int?, transactionTime: Long, transactionDescription: String, transactionLocation: String,
                        transactionCurrency: String, transactionAmount: Long) {
        this.id = id
        this.parent = parent
        this.transaction = transaction
        this.sequence = sequence
        this.transactionTime = transactionTime
        this.transactionDescription = transactionDescription
        this.transactionLocation = transactionLocation
        this.transactionCurrency = transactionCurrency
        this.transactionAmount = transactionAmount
    }

    /**
     * Return the Value that this Edit represents
     */
    val value: Value
        get() = Value(transactionCurrency, transactionAmount)

    override fun toString(): String {
        val tcal = Calendar.getInstance(TimeZone.getTimeZone("Z"))
        tcal.timeInMillis = transactionTime
        return String.format("id: %s, par: %s, tr: %s, seq: %s, time: %tFT%<tTZ, desc: %s, loc: %s, cur: %s, am: %d",
                id, parent, transaction, sequence, tcal, transactionDescription, transactionLocation, transactionCurrency, transactionAmount)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(id)
        dest.writeValue(parent)
        dest.writeValue(transaction)
        dest.writeValue(sequence)
        dest.writeLong(transactionTime)
        dest.writeString(transactionDescription)
        dest.writeString(transactionLocation)
        dest.writeString(transactionCurrency)
        dest.writeLong(transactionAmount)
    }

    private constructor(src: Parcel) {
        id = src.readValue(Long::class.java.classLoader) as Long
        parent = src.readValue(Long::class.java.classLoader) as Long
        transaction = src.readValue(Long::class.java.classLoader) as Long
        sequence = src.readValue(Int::class.java.classLoader) as Int
        transactionTime = src.readLong()
        transactionDescription = src.readString()
        transactionLocation = src.readString()
        transactionCurrency = src.readString()
        transactionAmount = src.readLong()
    }

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<Edit> = object : Parcelable.Creator<Edit> {
            override fun createFromParcel(source: Parcel): Edit {
                return Edit(source)
            }

            override fun newArray(size: Int): Array<Edit?> {
                return arrayOfNulls(size)
            }
        }
    }


}
