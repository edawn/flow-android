package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.base.BaseSingleFieldPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class RegularModel implements Parcelable, Comparable<RegularModel> {

    private static final Logger logger = LoggerFactory.getLogger(RegularModel.class);

    @IntDef({DBHelper.REGULARS_PERIOD_TYPE_DAILY, DBHelper.REGULARS_PERIOD_TYPE_MONTHLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodType {}

    public long creationTime;
    public long timeFirst;
    @PeriodType
    public int periodType;
    public int periodMultiplier;
    public boolean isSpread = false;
    public boolean isDisabled = false;
    public boolean isDeleted = false;
    public long amount;
    public String currency;
    public String description;

    public List<TransactionsRegularModel> executed = null;

    /**
     * Creates a new instance
     */
    public RegularModel(long creationTime, long timeFirst, @PeriodType int periodType, int periodMultiplier,
                        boolean isSpread, boolean isDisabled, boolean isDeleted, long amount, String currency,
                        String description) {
        this.creationTime = creationTime;
        this.timeFirst = timeFirst;
        this.periodType = periodType;
        this.periodMultiplier = periodMultiplier;
        this.isSpread = isSpread;
        this.isDisabled = isDisabled;
        this.isDeleted = isDeleted;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public RegularModel(Cursor cursor) {
        creationTime = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_CREATION_TIME));
        timeFirst = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_FIRST));
        //noinspection ResourceType
        periodType = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_TYPE));
        periodMultiplier = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER));
        isSpread = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_SPREAD)) != 0;
        isDisabled = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_DISABLED)) != 0;
        isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_DELETED)) != 0;
        amount = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_AMOUNT));
        currency = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_CURRENCY));
        description = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_DESCRIPTION));
    }

    /**
     * Calculates the period number range. ({@link DBHelper#TRANSACTIONS_REGULAR_KEY_PERIOD_NUMBER} and
     * {@link TransactionsRegularModel#periodNumber})
     * @param start The start of the time span
     * @param end The end of the time span. Must be greater than start
     * @return A Pair that denotes the first (including) and the last (excluding) period number of the
     * periods that start in the given time span
     */
    //TODO check time zone
    public Pair<Integer, Integer> getPeriodNumberRange(long start, long end) {
        if (start >= end) {
            throw new IllegalArgumentException("start must be before end");
        }

        Pair<Integer, Integer> range = new Pair<>(getPeriodNumberForStart(start), getPeriodNumberForEnd(end));

        return range;
    }

    //TODO merge getPeriodNumberForStart and getPeriodNumberForEnd into getPeriodNumberRange
    private int getPeriodNumberForStart(long start) {
        if (start == timeFirst) {
            return 0;
        }

        DateTime dtFirst = new DateTime(timeFirst);
        DateTime dtStart = new DateTime(start);
        int periodsBetween;
        BaseSingleFieldPeriod period = getPeriod();

        if (period instanceof Days) {
            Days periodDays = (Days) period;
            periodsBetween = Days.daysBetween(dtFirst, dtStart).getDays() / periodDays.getDays();
        } else if (period instanceof Months) {
            Months periodMonths = (Months) period;
            periodsBetween = Months.monthsBetween(dtFirst, dtStart).getMonths() / periodMonths.getMonths();
        } else {
            throw new IllegalArgumentException("Unsupported period type");
        }

        Period periodPeriod = new Period(period);
        DateTime discreteToStart = dtFirst.plus(periodPeriod.multipliedBy(periodsBetween));
        if (dtStart.isBefore(dtFirst)) {
            return periodsBetween;
        } else {
            return discreteToStart.isEqual(dtStart) ? periodsBetween : periodsBetween + 1;
        }
    }

    private int getPeriodNumberForEnd(long end) {
        if (end == timeFirst) {
            return 0;
        }

        DateTime dtFirst = new DateTime(timeFirst);
        DateTime dtEnd = new DateTime(end);
        int periodsBetween;
        BaseSingleFieldPeriod period = getPeriod();

        if (period instanceof Days) {
            Days periodDays = (Days) period;
            periodsBetween = Days.daysBetween(dtFirst, dtEnd).getDays() / periodDays.getDays();
        } else if (period instanceof Months) {
            Months periodMonths = (Months) period;
            periodsBetween = Months.monthsBetween(dtFirst, dtEnd).getMonths() / periodMonths.getMonths();
        } else {
            throw new IllegalArgumentException("Unsupported period type");
        }

        Period periodPeriod = new Period(period);
        DateTime discreteToEnd = dtFirst.plus(periodPeriod.multipliedBy(periodsBetween));
        if (dtEnd.isBefore(dtFirst)) {
            return periodsBetween;
        } else {
            return discreteToEnd.isEqual(dtEnd) ? periodsBetween : periodsBetween + 1;
        }
    }

    /**
     * Calculates an unix time for a period number.
     * @param periodNumber The period number
     * @return The unix time (in ms) represented by the period number
     */
    //TODO check time zone
    public long getTimeForPeriodNumber(int periodNumber) {
        DateTime dtFirst = new DateTime(timeFirst);
        Period period = new Period(getPeriod());
        DateTime dt = dtFirst.plus(period.multipliedBy(periodNumber));
        return dt.getMillis();
    }

    public org.joda.time.base.BaseSingleFieldPeriod getPeriod() {
        return periodType == DBHelper.REGULARS_PERIOD_TYPE_DAILY ?
                org.joda.time.Days.days(periodMultiplier) : org.joda.time.Months.months(periodMultiplier);
    }

    /**
     * Returns the cumulative amount of the executed transactions associated with this instance
     */
    public Value getExecutedValue() {
        return new Value(currency, amount * (executed == null ? 0 : executed.size()));
    }

    public Value getValue() {
        return new Value(currency, amount);
    }

    /**
     * Map this instance to the ContentValues
     * @param cv Where to store the fields of this instance; can be later used to
     *           insert into {@link DBHelper#REGULARS_TABLE_NAME}
     * @return the same instance from the arguments
     */
    public ContentValues toContentValues(@NonNull ContentValues cv) {
        cv.put(DBHelper.REGULARS_KEY_CREATION_TIME, creationTime);
        cv.put(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst);
        cv.put(DBHelper.REGULARS_KEY_PERIOD_TYPE, periodType);
        cv.put(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER, periodMultiplier);
        cv.put(DBHelper.REGULARS_KEY_IS_SPREAD, isSpread);
        cv.put(DBHelper.REGULARS_KEY_IS_DISABLED, isDisabled);
        cv.put(DBHelper.REGULARS_KEY_IS_DELETED, isDeleted);
        cv.put(DBHelper.REGULARS_KEY_DESCRIPTION, description);
        cv.put(DBHelper.REGULARS_KEY_CURRENCY, currency);
        cv.put(DBHelper.REGULARS_KEY_AMOUNT, amount);
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(creationTime);
        dest.writeLong(timeFirst);
        dest.writeInt(periodType);
        dest.writeInt(periodMultiplier);
        dest.writeBooleanArray(new boolean[]{isSpread, isDisabled, isDeleted});
        dest.writeLong(amount);
        dest.writeString(currency);
        dest.writeString(description);
    }

    public static final Parcelable.Creator<RegularModel> CREATOR =
            new Parcelable.Creator<RegularModel>() {
                public RegularModel createFromParcel(Parcel in) {
                    return new RegularModel(in);
                }
                @Override
                public RegularModel[] newArray(int size) {
                    return new RegularModel[size];
                }
            };

    private RegularModel(Parcel in) {
        creationTime = in.readLong();
        timeFirst = in.readLong();
        //noinspection ResourceType
        periodType = in.readInt();
        periodMultiplier = in.readInt();
        boolean[] bools = new boolean[3];
        in.readBooleanArray(bools);
        isSpread = bools[0];
        isDisabled = bools[1];
        isDeleted = bools[2];
        amount = in.readLong();
        currency = in.readString();
        description = in.readString();
    }

    @Override
    public int compareTo(@NonNull RegularModel another) {
        return timeFirst < another.timeFirst ? -1 : timeFirst > another.timeFirst ? 1 :
                creationTime < another.creationTime ? -1 : creationTime > another.creationTime ? 1 : 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegularModel && creationTime == ((RegularModel) o).creationTime;
    }
}
