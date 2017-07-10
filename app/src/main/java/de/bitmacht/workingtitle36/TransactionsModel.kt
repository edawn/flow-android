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

import android.content.ContentValues
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable

/**
 * This represents a transaction
 * @see DBHelper.TRANSACTIONS_TABLE_NAME
 */
class TransactionsModel private constructor(val id: Long = 0, var isRemoved: Boolean = false, var mostRecentEdit: Edit? = null) : Parcelable {

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     * *           insert into [DBHelper.TRANSACTIONS_TABLE_NAME]
     * *
     * @return the same instance from the arguments
     */
    fun toContentValues(cv: ContentValues = ContentValues(2)) = cv.apply {
        put(DBHelper.TRANSACTIONS_KEY_ID, id)
        put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, isRemoved)
    }

    override fun toString(): String = "id: $id isRe: $isRemoved moRE: $mostRecentEdit"

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeInt(if (isRemoved) 1 else 0)
        writeParcelable(mostRecentEdit, flags)
    }

    constructor(src: Parcel) : this(src.readLong(), src.readInt() == 1, src.readParcelable<Edit>(Edit::class.java.classLoader))

    companion object {

        /**
         * Returns a new instance of a transaction with an associated Edit.
         * This is intended to be used with the results of [DBHelper.TRANSACTIONS_EDITS_QUERY] or [DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY]
         * @param cursor A cursor positioned at the appropriate row.
         * *               The columns should include any column from [DBHelper.EDITS_TABLE_NAME] and
         * *               [DBHelper.TRANSACTIONS_KEY_IS_REMOVED] from [DBHelper.TRANSACTIONS_TABLE_NAME]
         * *
         * @return A new [TransactionsModel] with the field [TransactionsModel.mostRecentEdit] set.
         */
        fun getInstanceWithEdit(cursor: Cursor): TransactionsModel = TransactionsModel(
                cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_IS_REMOVED)) != 0,
                Edit(cursor))

        @JvmField val CREATOR: Parcelable.Creator<TransactionsModel> = object : Parcelable.Creator<TransactionsModel> {
            override fun createFromParcel(source: Parcel): TransactionsModel = TransactionsModel(source)
            override fun newArray(size: Int): Array<TransactionsModel?> = arrayOfNulls(size)
        }
    }
}
