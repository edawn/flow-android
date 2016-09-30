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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Period;

public class Periods implements Parcelable {

    /**
     * This is the default long period (one month)
     */
    public static final Period DEFAULT_LONG_PERIOD = Period.months(1);

    /**
     * This is the default short period (one day)
     */
    public static final Period DEFAULT_SHORT_PERIOD = Period.days(1);

    private DateTime longStart;
    private Period longPeriod;
    private DateTime shortStart;
    private Period shortPeriod;

    /**
     * Create a new instance with the view set to the start of the current month and day
     */
    public Periods() {
        this(DateTime.now());
    }

    /**
     * Create a new instance with the view set to the start of the month and day represented by viewTime
     */
    public Periods(DateTime viewTime) {
        longStart = viewTime.withDayOfMonth(1).withTimeAtStartOfDay();
        longPeriod = DEFAULT_LONG_PERIOD;

        shortStart = viewTime.withTimeAtStartOfDay();
        shortPeriod = DEFAULT_SHORT_PERIOD;
    }

    public Periods(DateTime longStart, Period longPeriod, DateTime shortStart, Period shortPeriod) {
        this.longStart = longStart;
        this.longPeriod = longPeriod;
        this.shortStart = shortStart;
        this.shortPeriod = shortPeriod;
    }

    @NonNull
    public DateTime getLongStart() {
        return longStart;
    }

    @NonNull
    public Period getLongPeriod() {
        return longPeriod;
    }

    @NonNull
    public DateTime getLongEnd() {
        return longStart.plus(longPeriod);
    }

    @NonNull
    public DateTime getShortStart() {
        return shortStart;
    }

    @NonNull
    public Period getShortPeriod() {
        return shortPeriod;
    }

    @NonNull
    public DateTime getShortEnd() {
        return shortStart.plus(shortPeriod);
    }

    @NonNull
    public Periods previousLong() {
        DateTime start = longStart.minus(longPeriod);
        return new Periods(start, longPeriod, start, shortPeriod);
    }

    @NonNull
    public Periods nextLong() {
        DateTime start = longStart.plus(longPeriod);
        return new Periods(start, longPeriod, start, shortPeriod);
    }

    @Nullable
    public Periods previousShort() {
        DateTime start = shortStart.minus(shortPeriod);
        if (start.isBefore(longStart)) {
            return null;
        }
        return new Periods(longStart, longPeriod, start, shortPeriod);
    }

    @Nullable
    public Periods nextShort() {
        DateTime start = shortStart.plus(shortPeriod);
        if (!getLongEnd().isAfter(start)) {
            return null;
        }
        return new Periods(longStart, longPeriod, start, shortPeriod);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(longStart);
        dest.writeSerializable(longPeriod);
        dest.writeSerializable(shortStart);
        dest.writeSerializable(shortPeriod);
    }

    public static final Parcelable.Creator<Periods> CREATOR =
            new Creator<Periods>() {
                @Override
                public Periods createFromParcel(Parcel source) {
                    return new Periods(source);
                }

                @Override
                public Periods[] newArray(int size) {
                    return new Periods[size];
                }
            };

    private Periods(Parcel source) {
        longStart = (DateTime) source.readSerializable();
        longPeriod = (Period) source.readSerializable();
        shortStart = (DateTime) source.readSerializable();
        shortPeriod = (Period) source.readSerializable();
    }

}
