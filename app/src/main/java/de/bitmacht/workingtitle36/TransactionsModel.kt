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
class TransactionsModel : Parcelable {

    var id: Long = 0
    var isRemoved: Boolean = false

    var mostRecentEdit: Edit? = null

    private constructor()

    constructor(cursor: Cursor) {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_ID))
        isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_IS_REMOVED)) != 0
    }

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     * *           insert into [DBHelper.TRANSACTIONS_TABLE_NAME]
     * *
     * @return the same instance from the arguments
     */
    fun toContentValues(cv: ContentValues): ContentValues {
        cv.put(DBHelper.TRANSACTIONS_KEY_ID, id)
        cv.put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, isRemoved)
        return cv
    }

    override fun toString(): String {
        return "id: $id isRe: $isRemoved moRE: $mostRecentEdit"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeInt(if (isRemoved) 1 else 0)
        dest.writeParcelable(mostRecentEdit, flags)
    }

    constructor(src: Parcel) {
        id = src.readLong()
        isRemoved = src.readInt() == 1
        mostRecentEdit = src.readParcelable<Edit>(Edit::class.java.classLoader)
    }

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
        fun getInstanceWithEdit(cursor: Cursor): TransactionsModel {
            val transaction = TransactionsModel()
            transaction.id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.EDITS_KEY_TRANSACTION))
            transaction.isRemoved = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.TRANSACTIONS_KEY_IS_REMOVED)) != 0
            transaction.mostRecentEdit = Edit(cursor)
            return transaction
        }

        @JvmField val CREATOR: Parcelable.Creator<TransactionsModel> = object : Parcelable.Creator<TransactionsModel> {
            override fun createFromParcel(source: Parcel): TransactionsModel {
                return TransactionsModel(source)
            }

            override fun newArray(size: Int): Array<TransactionsModel?> {
                return arrayOfNulls(size)
            }
        }
    }
}
