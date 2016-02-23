package de.bitmacht.workingtitle36;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RegularModel implements Parcelable {

    @IntDef({PERIOD_TYPE_DAILY, PERIOD_TYPE_MONTHLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PeriodType {}

    public static final int PERIOD_TYPE_DAILY = 0;
    public static final int PERIOD_TYPE_MONTHLY = 1;

    public long creationTime;
    public long timeFirst;
    @PeriodType
    public int periodType;
    public int periodMultiplier;
    public boolean isSpread = false;
    public boolean isDisabled = false;
    public boolean isDeleted = false;
    public long value;
    public String currency;
    public String description;

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
        dest.writeLong(value);
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

    public RegularModel(Parcel in) {
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
        value = in.readLong();
        currency = in.readString();
        description = in.readString();
    }
}
