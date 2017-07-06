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

class Periods(val longStart: DateTime, val longPeriod: Period, val shortStart: DateTime, val shortPeriod: Period) : Parcelable {

    /** by default the view is set to the start of the current month and day */
    constructor(viewTime: DateTime = DateTime.now()) : this(viewTime.withDayOfMonth(1).withTimeAtStartOfDay(),
            DEFAULT_LONG_PERIOD, viewTime.withTimeAtStartOfDay(), DEFAULT_SHORT_PERIOD)

    val longEnd: DateTime by lazy { longStart.plus(longPeriod) }

    val shortEnd: DateTime by lazy { shortStart.plus(shortPeriod) }

    val previousLong: Periods by lazy {
        val start = longStart.minus(longPeriod)
        Periods(start, longPeriod, start, shortPeriod)
    }

    val nextLong: Periods by lazy {
        val start = longStart.plus(longPeriod)
        Periods(start, longPeriod, start, shortPeriod)
    }

    val previousShort: Periods? by lazy {
        val start = shortStart.minus(shortPeriod)
        if (start.isBefore(longStart)) null else Periods(longStart, longPeriod, start, shortPeriod)
    }

    val nextShort: Periods? by lazy {
        val start = shortStart.plus(shortPeriod)
        if (!longEnd.isAfter(start)) null else Periods(longStart, longPeriod, start, shortPeriod)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        with(dest) {
            writeSerializable(longStart)
            writeSerializable(longPeriod)
            writeSerializable(shortStart)
            writeSerializable(shortPeriod)
        }
    }

    private constructor(src: Parcel) : this(src.readSerializable() as DateTime, src.readSerializable() as Period,
            src.readSerializable() as DateTime, src.readSerializable() as Period)

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