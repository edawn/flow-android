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
import org.joda.time.DateTimeConstants
import org.joda.time.Days
import org.joda.time.Months
import org.joda.time.Period
import org.joda.time.base.BaseSingleFieldPeriod

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class RegularModel : Parcelable {

    @IntDef(DBHelper.REGULARS_PERIOD_TYPE_DAILY.toLong(), DBHelper.REGULARS_PERIOD_TYPE_MONTHLY.toLong())
    @Retention(RetentionPolicy.SOURCE)
    annotation class PeriodType

    var id: Long? = null
    var timeFirst: Long = 0
    var timeLast: Long = 0
    @PeriodType
    var periodType: Int = 0
    var periodMultiplier: Int = 0
    var isSpread = false
    var isDisabled = false
    var amount: Long = 0
    var currency: String
    var description: String

    /**
     * Creates a new instance
     */
    constructor(timeFirst: Long, timeLast: Long, @PeriodType periodType: Int, periodMultiplier: Int,
                isSpread: Boolean, isDisabled: Boolean, amount: Long, currency: String, description: String) {
        this.timeFirst = timeFirst
        this.timeLast = timeLast
        this.periodType = periodType
        this.periodMultiplier = periodMultiplier
        this.isSpread = isSpread
        this.isDisabled = isDisabled
        this.amount = amount
        this.currency = currency
        this.description = description
    }

    constructor(cursor: Cursor) {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_ID))
        timeFirst = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_FIRST))
        timeLast = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_LAST))

        periodType = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_TYPE))
        periodMultiplier = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER))
        isSpread = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_SPREAD)) != 0
        isDisabled = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_DISABLED)) != 0
        amount = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_AMOUNT))
        currency = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_CURRENCY))
        description = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_DESCRIPTION))
    }

    //TODO merge getPeriodNumberForStart and getPeriodNumberForEnd into getPeriodNumberRange
    private fun getPeriodNumberForStart(dtStart: DateTime): Int {
        if (dtStart.isEqual(timeFirst)) {
            return 0
        }

        val dtFirst = DateTime(timeFirst)
        val periodsBetween: Int
        val period = period

        if (period is Days) {
            periodsBetween = Days.daysBetween(dtFirst, dtStart).days / period.days
        } else if (period is Months) {
            periodsBetween = Months.monthsBetween(dtFirst, dtStart).months / period.months
        } else {
            throw IllegalArgumentException("Unsupported period type")
        }

        val periodPeriod = Period(period)
        val discreteToStart = dtFirst.plus(periodPeriod.multipliedBy(periodsBetween))
        if (dtStart.isBefore(dtFirst)) {
            return periodsBetween
        } else {
            return if (discreteToStart.isEqual(dtStart)) periodsBetween else periodsBetween + 1
        }
    }

    private fun getPeriodNumberForEnd(dtEnd: DateTime): Int {
        if (dtEnd.isEqual(timeFirst)) {
            return 0
        }

        val dtFirst = DateTime(timeFirst)
        val periodsBetween: Int
        val period = period

        if (period is Days) {
            periodsBetween = Days.daysBetween(dtFirst, dtEnd).days / period.days
        } else if (period is Months) {
            periodsBetween = Months.monthsBetween(dtFirst, dtEnd).months / period.months
        } else {
            throw IllegalArgumentException("Unsupported period type")
        }

        val periodPeriod = Period(period)
        val discreteToEnd = dtFirst.plus(periodPeriod.multipliedBy(periodsBetween))
        if (dtEnd.isBefore(dtFirst)) {
            return periodsBetween
        } else {
            return if (discreteToEnd.isEqual(dtEnd)) periodsBetween else periodsBetween + 1
        }
    }

    val period: org.joda.time.base.BaseSingleFieldPeriod
        get() = if (periodType == DBHelper.REGULARS_PERIOD_TYPE_DAILY)
            org.joda.time.Days.days(periodMultiplier)
        else
            org.joda.time.Months.months(periodMultiplier)

    /**
     * Returns the cumulative value of this regular for a given time span
     * @param start The beginning of the time span; including
     * *
     * @param end The end of the time span; excluding
     */
    fun getCumulativeValue(start: DateTime, end: DateTime): Value {
        if (!start.isBefore(end)) {
            throw IllegalArgumentException("start must be before end")
        }

        val pnStart: Int
        var pnEnd: Int
        if (!start.isAfter(timeFirst)) {
            pnStart = 0
        } else {
            pnStart = getPeriodNumberForStart(start)
        }

        if (timeLast >= 0 && !end.isBefore(timeLast)) {
            pnEnd = getPeriodNumberForEnd(DateTime(timeLast))
        } else {
            pnEnd = getPeriodNumberForEnd(end)
        }
        pnEnd = Math.max(pnStart, pnEnd)

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

    val value: Value
        get() = Value(currency, amount)

    /**
     * Return a textual representation of this period
     */
    fun getPeriodString(context: Context): String {
        val i = periodIndex
        if (i == -1) {
            //TODO return something like "every n days/months"
            return periodType.toString() + ":" + periodMultiplier.toString()
        } else {
            return context.resources.getStringArray(R.array.interval_names)[i]
        }
    }

    /**
     * Return the index corresponding to [R.array.interval_names] and representing this period
     * @return An index; if the stored period number does not have a corresponding entry in
     * * [R.array.interval_names], -1 will be returned.
     */
    // daily
    // weekly
    // monthly
    // yearly
    val periodIndex: Int
        get() {
            var i = -1
            if (periodType == DBHelper.REGULARS_PERIOD_TYPE_DAILY) {
                if (periodMultiplier == 1) {
                    i = 0
                } else if (periodMultiplier == 7) {
                    i = 1
                }
            } else if (periodType == DBHelper.REGULARS_PERIOD_TYPE_MONTHLY) {
                if (periodMultiplier == 1) {
                    i = 2
                } else if (periodMultiplier == 12) {
                    i = 3
                }
            }
            return i
        }

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     * *           insert into [DBHelper.REGULARS_TABLE_NAME]
     * *
     * @return the same instance from the arguments
     */
    fun toContentValues(cv: ContentValues): ContentValues {
        cv.put(DBHelper.REGULARS_KEY_ID, id)
        cv.put(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst)
        cv.put(DBHelper.REGULARS_KEY_TIME_LAST, timeLast)
        cv.put(DBHelper.REGULARS_KEY_PERIOD_TYPE, periodType)
        cv.put(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER, periodMultiplier)
        cv.put(DBHelper.REGULARS_KEY_IS_SPREAD, isSpread)
        cv.put(DBHelper.REGULARS_KEY_IS_DISABLED, isDisabled)
        cv.put(DBHelper.REGULARS_KEY_DESCRIPTION, description)
        cv.put(DBHelper.REGULARS_KEY_CURRENCY, currency)
        cv.put(DBHelper.REGULARS_KEY_AMOUNT, amount)
        return cv
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeValue(id)
        dest.writeLong(timeFirst)
        dest.writeLong(timeLast)
        dest.writeInt(periodType)
        dest.writeInt(periodMultiplier)
        dest.writeBooleanArray(booleanArrayOf(isSpread, isDisabled))
        dest.writeLong(amount)
        dest.writeString(currency)
        dest.writeString(description)
    }

    private constructor(src: Parcel) {
        id = src.readValue(Long::class.java.classLoader) as Long
        timeFirst = src.readLong()
        timeLast = src.readLong()

        periodType = src.readInt()
        periodMultiplier = src.readInt()
        val bools = BooleanArray(2)
        src.readBooleanArray(bools)
        isSpread = bools[0]
        isDisabled = bools[1]
        amount = src.readLong()
        currency = src.readString()
        description = src.readString()
    }

    override fun equals(o: Any?): Boolean {
        return id != null && o is RegularModel && o.id != null && id == o.id
    }

    companion object {

        @JvmField val CREATOR: Parcelable.Creator<RegularModel> = object : Parcelable.Creator<RegularModel> {
            override fun createFromParcel(`in`: Parcel): RegularModel {
                return RegularModel(`in`)
            }

            override fun newArray(size: Int): Array<RegularModel?> {
                return arrayOfNulls(size)
            }
        }
    }
}
