package de.bitmacht.workingtitle36;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.base.BaseSingleFieldPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RegularModel implements Parcelable {

    private static final Logger logger = LoggerFactory.getLogger(RegularModel.class);

    @IntDef({DBHelper.REGULARS_PERIOD_TYPE_DAILY, DBHelper.REGULARS_PERIOD_TYPE_MONTHLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodType {}

    public Long id = null;
    public long timeFirst;
    public long timeLast;
    @PeriodType
    public int periodType;
    public int periodMultiplier;
    public boolean isSpread = false;
    public boolean isDisabled = false;
    public long amount;
    public String currency;
    public String description;

    /**
     * Creates a new instance
     */
    public RegularModel(long timeFirst, long timeLast, @PeriodType int periodType, int periodMultiplier,
                        boolean isSpread, boolean isDisabled, long amount, String currency, String description) {
        this.timeFirst = timeFirst;
        this.timeLast = timeLast;
        this.periodType = periodType;
        this.periodMultiplier = periodMultiplier;
        this.isSpread = isSpread;
        this.isDisabled = isDisabled;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public RegularModel(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_ID));
        timeFirst = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_FIRST));
        timeLast = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_TIME_LAST));
        //noinspection ResourceType
        periodType = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_TYPE));
        periodMultiplier = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER));
        isSpread = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_SPREAD)) != 0;
        isDisabled = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_IS_DISABLED)) != 0;
        amount = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_AMOUNT));
        currency = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_CURRENCY));
        description = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.REGULARS_KEY_DESCRIPTION));
    }

    //TODO merge getPeriodNumberForStart and getPeriodNumberForEnd into getPeriodNumberRange
    private int getPeriodNumberForStart(DateTime dtStart) {
        if (dtStart.isEqual(timeFirst)) {
            return 0;
        }

        DateTime dtFirst = new DateTime(timeFirst);
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

    private int getPeriodNumberForEnd(DateTime dtEnd) {
        if (dtEnd.isEqual(timeFirst)) {
            return 0;
        }

        DateTime dtFirst = new DateTime(timeFirst);
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

    public org.joda.time.base.BaseSingleFieldPeriod getPeriod() {
        return periodType == DBHelper.REGULARS_PERIOD_TYPE_DAILY ?
                org.joda.time.Days.days(periodMultiplier) : org.joda.time.Months.months(periodMultiplier);
    }

    /**
     * Returns the cumulative value of this regular for a given time span
     * @param start The beginning of the time span; including
     * @param end The end of the time span; excluding
     */
    public Value getCumulativeValue(DateTime start, DateTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }

        int pnStart, pnEnd;
        if (!start.isAfter(timeFirst)) {
            pnStart = 0;
        } else {
            pnStart = getPeriodNumberForStart(start);
        }

        if (timeLast >= 0 && !end.isBefore(timeLast)) {
            pnEnd = getPeriodNumberForEnd(new DateTime(timeLast));
        } else {
            pnEnd = getPeriodNumberForEnd(end);
        }
        pnEnd = Math.max(pnStart, pnEnd);

        if (BuildConfig.DEBUG) {
            if (pnStart < 0 || pnEnd < 0) {
                logger.error("pnStart: {} pnEnd: {}; neither should be below zero", pnStart, pnEnd);
            }
        }

        Value value = new Value(currency, amount * (pnEnd - pnStart));

        if (BuildConfig.DEBUG) {
            StringBuilder sb = new StringBuilder();
            int pn = pnStart;
            DateTime dtFirst = new DateTime(timeFirst);
            Period period = new Period(getPeriod());
            while (pn < pnEnd) {
                DateTime dt = dtFirst.plus(period.multipliedBy(pn));
                sb.append(dt).append(" / ");
                pn++;
            }
            logger.trace("regular: {} in: {} - {}:\nvalue: {}\n{}", description, start, end, value, sb);
        }
        return value;
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
        cv.put(DBHelper.REGULARS_KEY_ID, id);
        cv.put(DBHelper.REGULARS_KEY_TIME_FIRST, timeFirst);
        cv.put(DBHelper.REGULARS_KEY_TIME_LAST, timeLast);
        cv.put(DBHelper.REGULARS_KEY_PERIOD_TYPE, periodType);
        cv.put(DBHelper.REGULARS_KEY_PERIOD_MULTIPLIER, periodMultiplier);
        cv.put(DBHelper.REGULARS_KEY_IS_SPREAD, isSpread);
        cv.put(DBHelper.REGULARS_KEY_IS_DISABLED, isDisabled);
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
        dest.writeValue(id);
        dest.writeLong(timeFirst);
        dest.writeLong(timeLast);
        dest.writeInt(periodType);
        dest.writeInt(periodMultiplier);
        dest.writeBooleanArray(new boolean[]{isSpread, isDisabled});
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
        id = (Long) in.readValue(Long.class.getClassLoader());
        timeFirst = in.readLong();
        timeLast = in.readLong();
        //noinspection ResourceType
        periodType = in.readInt();
        periodMultiplier = in.readInt();
        boolean[] bools = new boolean[2];
        in.readBooleanArray(bools);
        isSpread = bools[0];
        isDisabled = bools[1];
        amount = in.readLong();
        currency = in.readString();
        description = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        return id != null && o instanceof RegularModel && ((RegularModel) o).id != null && id.equals(((RegularModel)o).id);
    }
}
