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

package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.bitmacht.workingtitle36.R;

/**
 * Displays a time.
 */
public class TimeView extends TextView {


    @IntDef({TIME_FORMAT_TIME, TIME_FORMAT_DATE, TIME_FORMAT_TIMEDATE, TIME_FORMAT_TIMEDATE_SHORT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeFormat {}

    public static final int TIME_FORMAT_TIME = 0;
    public static final int TIME_FORMAT_DATE = 1;
    public static final int TIME_FORMAT_TIMEDATE = 2;
    public static final int TIME_FORMAT_TIMEDATE_SHORT = 3;

    private static final String[] TIME_FORMATS = {"HH:mm", "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "MM-dd HH:mm"};

    private int timeFormatStyle = TIME_FORMAT_TIME;
    private SimpleDateFormat timeFormat;

    private long time = 0;

    public TimeView(Context context) {
        super(context);
        initAttrs(context, null, 0, 0);
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs, 0, 0);
    }

    public TimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeView, defStyleAttr, defStyleRes);

        try {
            timeFormatStyle = a.getInteger(R.styleable.TimeView_timeFormat, timeFormatStyle);
        } finally {
            a.recycle();
        }

        updateTimeFormat();
    }

    public void setTimeFormat(@TimeFormat int timeFormatStyle) {
        this.timeFormatStyle = timeFormatStyle;
        updateTimeFormat();
    }

    @TimeFormat
    public int getTimeFormat() {
        return timeFormatStyle;
    }

    private void updateTimeFormat() {
        timeFormat = new SimpleDateFormat(TIME_FORMATS[timeFormatStyle]);
        update();
    }

    /**
     * Set the time to be displayed.
     * @param time The time in ms since the epoch.
     */
    public void setTime(long time) {
        if (this.time == time) {
            return;
        }
        this.time = time;
        update();
    }

    public long getTime() {
        return time;
    }

    private void update() {
        if (time != 0) {
            setText(timeFormat.format(new Date(time)));
        }
    }
}
