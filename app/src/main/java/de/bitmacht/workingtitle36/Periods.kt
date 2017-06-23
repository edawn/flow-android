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

import android.os.Parcel
import android.os.Parcelable

import org.joda.time.DateTime
import org.joda.time.Period

class Periods : Parcelable {

    var longStart: DateTime? = null
        private set
    var longPeriod: Period? = null
        private set
    var shortStart: DateTime? = null
        private set
    var shortPeriod: Period? = null
        private set

    /**
     * Create a new instance; by default the view is set to the start of the current month and day
     */
    @JvmOverloads constructor(viewTime: DateTime = DateTime.now()) {
        longStart = viewTime.withDayOfMonth(1).withTimeAtStartOfDay()
        longPeriod = DEFAULT_LONG_PERIOD

        shortStart = viewTime.withTimeAtStartOfDay()
        shortPeriod = DEFAULT_SHORT_PERIOD
    }

    constructor(longStart: DateTime, longPeriod: Period, shortStart: DateTime, shortPeriod: Period) {
        this.longStart = longStart
        this.longPeriod = longPeriod
        this.shortStart = shortStart
        this.shortPeriod = shortPeriod
    }

    val longEnd: DateTime
        get() = longStart!!.plus(longPeriod)

    val shortEnd: DateTime
        get() = shortStart!!.plus(shortPeriod)

    fun previousLong(): Periods {
        val start = longStart!!.minus(longPeriod)
        return Periods(start, longPeriod!!, start, shortPeriod!!)
    }

    fun nextLong(): Periods {
        val start = longStart!!.plus(longPeriod)
        return Periods(start, longPeriod!!, start, shortPeriod!!)
    }

    fun previousShort(): Periods? {
        val start = shortStart!!.minus(shortPeriod)
        if (start.isBefore(longStart)) {
            return null
        }
        return Periods(longStart!!, longPeriod!!, start, shortPeriod!!)
    }

    fun nextShort(): Periods? {
        val start = shortStart!!.plus(shortPeriod)
        if (!longEnd.isAfter(start)) {
            return null
        }
        return Periods(longStart!!, longPeriod!!, start, shortPeriod!!)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(longStart)
        dest.writeSerializable(longPeriod)
        dest.writeSerializable(shortStart)
        dest.writeSerializable(shortPeriod)
    }

    private constructor(src: Parcel) {
        longStart = src.readSerializable() as DateTime
        longPeriod = src.readSerializable() as Period
        shortStart = src.readSerializable() as DateTime
        shortPeriod = src.readSerializable() as Period
    }

    companion object {

        /**
         * This is the default long period (one month)
         */
        val DEFAULT_LONG_PERIOD = Period.months(1)

        /**
         * This is the default short period (one day)
         */
        val DEFAULT_SHORT_PERIOD = Period.days(1)

        @JvmField val CREATOR: Parcelable.Creator<Periods> = object : Parcelable.Creator<Periods> {
            override fun createFromParcel(src: Parcel): Periods {
                return Periods(src)
            }

            override fun newArray(size: Int): Array<Periods?> {
                return arrayOfNulls(size)
            }
        }
    }

}