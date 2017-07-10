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
import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Months
import org.joda.time.Period

class RegularModel(
        val id: Long? = null,
        val timeFirst: Long = 0,
        val timeLast: Long = 0,
        @PeriodType
        val periodType: Int = DBHelper.REGULARS_PERIOD_TYPE_DAILY,
        val periodMultiplier: Int = 0,
        val isSpread: Boolean = false,
        val isDisabled: Boolean = false,
        val amount: Long = 0,
        val currency: String,
        val description: String)
    : Parcelable {

    @IntDef(DBHelper.REGULARS_PERIOD_TYPE_DAILY.toLong(), DBHelper.REGULARS_PERIOD_TYPE_MONTHLY.toLong())
    @Retention(AnnotationRetention.SOURCE)
    annotation class PeriodType

    val period: org.joda.time.base.BaseSingleFieldPeriod by lazy {
        when (periodType) {
            DBHelper.REGULARS_PERIOD_TYPE_DAILY -> Days.days(periodMultiplier)
            DBHelper.REGULARS_PERIOD_TYPE_MONTHLY -> Months.months(periodMultiplier)
            else -> throw IllegalStateException("Unknown period type: $periodType")
        }
    }

    val value: Value by lazy { Value(currency, amount) }

    /**
     * Creates a new instance
     */
    constructor(timeFirst: Long, timeLast: Long, periodType: Int, periodMultiplier: Int, isSpread: Boolean,
                isDisabled: Boolean, amount: Long, currency: String, description: String) :
            this(null, timeFirst, timeLast, periodType, periodMultiplier, isSpread, isDisabled, amount, currency, description)

    constructor(cursor: Cursor) : this(
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_ID)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_FIRST)),
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_LAST)),
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_TYPE)),
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER)),
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_SPREAD)) != 0,
            cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_DISABLED)) != 0,
            cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_AMOUNT)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_CURRENCY)),
            cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_DESCRIPTION))
    )

    private fun getPeriodNumber(time: DateTime): Int {
        if (time.isEqual(timeFirst)) return 0

        val dtFirst = DateTime(timeFirst)

        val period = period
        val periodsBetween = when (period) {
            is Days -> Days.daysBetween(dtFirst, time).days / period.days
            is Months -> Months.monthsBetween(dtFirst, time).months / period.months
            else -> throw IllegalArgumentException("Unsupported period type")
        }

        return if (time.isBefore(dtFirst) || dtFirst.plus(Period(period).multipliedBy(periodsBetween)).isEqual(time))
            periodsBetween
        else
            periodsBetween + 1
    }

    /**
     * Returns the cumulative value of this regular for a given time span
     * @param start The beginning of the time span; including
     * *
     * @param end The end of the time span; excluding
     */
    fun getCumulativeValue(start: DateTime, end: DateTime): Value {
        if (!start.isBefore(end))
            throw IllegalArgumentException("start must be before end")

        val pnStart = if (!start.isAfter(timeFirst)) 0 else getPeriodNumber(start)
        val pnEnd = Math.max(pnStart,
                if (timeLast >= 0 && !end.isBefore(timeLast)) getPeriodNumber(DateTime(timeLast))
                else getPeriodNumber(end))

        if (BuildConfig.DEBUG) {
            if (pnStart < 0 || pnEnd < 0) {
                loge("pnStart: $pnStart pnEnd: $pnEnd; neither should be below zero")
            }
        }

        val value = Value(currency, amount * (pnEnd - pnStart))

        if (BuildConfig.DEBUG) {
            val sb = StringBuilder()
            var pn = pnStart
            val dtFirst = DateTime(timeFirst)
            val period = Period(period)
            while (pn < pnEnd) {
                val dt = dtFirst.plus(period.multipliedBy(pn))
                sb.append(dt).append(" / ")
                pn++
            }
            logd("regular: $description in: $start - $end:\nvalue: $value\n$sb")
        }
        return value
    }

    /**
     * Return a textual representation of this period
     */
    fun getPeriodString(context: Context): String {
        val i = periodIndex
        return if (i == -1)
        //TODO return something like "every n days/months"
            "$periodType:$periodMultiplier"
        else
            context.resources.getStringArray(R.array.interval_names)[i]
    }

    /**
     * Return the index corresponding to [R.array.interval_names] and representing this period
     * @return An index; if the stored period number does not have a corresponding entry in
     * * [R.array.interval_names], -1 will be returned.
     */
    val periodIndex: Int by lazy { periodIndexMap[periodType]?.get(periodMultiplier) ?: -1 }

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     * *           insert into [DBHelper.REGULARS_TABLE_NAME]
     * *
     * @return the same instance from the arguments
     */
    fun toContentValues(cv: ContentValues = ContentValues(10)) = cv.apply {
        put(DBHelper.REGULARS_KEY_ID, id)
        put(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst)
        put(DBHelper.REGULARS_KEY_TIME_LAST, timeLast)
        put(DBHelper.REGULARS_KEY_PERIOD_TYPE, periodType)
        put(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER, periodMultiplier)
        put(DBHelper.REGULARS_KEY_IS_SPREAD, isSpread)
        put(DBHelper.REGULARS_KEY_IS_DISABLED, isDisabled)
        put(DBHelper.REGULARS_KEY_DESCRIPTION, description)
        put(DBHelper.REGULARS_KEY_CURRENCY, currency)
        put(DBHelper.REGULARS_KEY_AMOUNT, amount)
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeValue(id)
        writeLong(timeFirst)
        writeLong(timeLast)
        writeInt(periodType)
        writeInt(periodMultiplier)
        writeBooleanArray(booleanArrayOf(isSpread))
        writeBooleanArray(booleanArrayOf(isDisabled))
        writeLong(amount)
        writeString(currency)
        writeString(description)
    }

    private constructor(src: Parcel) : this(src.readValue(Long::class.java.classLoader) as Long,
            src.readLong(), src.readLong(), src.readInt(), src.readInt(),
            BooleanArray(1).also { src.readBooleanArray(it) }[0],
            BooleanArray(1).also { src.readBooleanArray(it) }[0],
            src.readLong(), src.readString(), src.readString())

    override fun equals(other: Any?): Boolean = id != null && other is RegularModel && other.id != null && id == other.id

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<RegularModel> = object : Parcelable.Creator<RegularModel> {
            override fun createFromParcel(source: Parcel): RegularModel = RegularModel(source)
            override fun newArray(size: Int): Array<RegularModel?> = arrayOfNulls(size)
        }

        val periodIndexMap = mapOf(
                DBHelper.REGULARS_PERIOD_TYPE_DAILY to mapOf(1 to 0, 7 to 1),
                DBHelper.REGULARS_PERIOD_TYPE_MONTHLY to mapOf(1 to 2, 12 to 3)
        )
    }
}
