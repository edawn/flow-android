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
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import de.bitmacht.workingtitle36.BuildConfig;
import de.bitmacht.workingtitle36.R;

/**
 * Displays a time.
 */
public class TimeView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger(TimeView.class);


    @IntDef({TIME_FORMAT_TIME, TIME_FORMAT_DATE, TIME_FORMAT_TIMEDATE_SHORT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeFormat {}

    public static final int TIME_FORMAT_TIME = 0;
    public static final int TIME_FORMAT_DATE = 1;
    public static final int TIME_FORMAT_TIMEDATE_SHORT = 2;

    private static final String[] TIME_FORMATS = {"Hm", "yMd", "MMddHm"};
    /**
     * 12-hour-cycle formats
     */
    private static final String[] TIME_FORMATS_12 = {"hma", "yMd", "MMddhma"};
    /**
     * Fallback for api levels before 18. See {@link android.text.format.DateFormat#getBestDateTimePattern}
     */
    private static final String[] TIME_FORMATS_OLD_API = {"HH:mm", "yyyy-MM-dd", "MM-dd HH:mm"};

    private static final ReentrantLock lock = new ReentrantLock();
    private static Long lastThreadId = null;
    private static Integer lastContextHash = null;
    private static SimpleDateFormat[] timeFormats = null;

    private int timeFormatStyle = TIME_FORMAT_TIME;
    private SimpleDateFormat timeFormat;

    private long time = 0;

    public TimeView(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public TimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeView, defStyleAttr, defStyleRes);

        try {
            timeFormatStyle = a.getInteger(R.styleable.TimeView_timeFormat, timeFormatStyle);
        } finally {
            a.recycle();
        }

        updateTimeFormat();
    }

    public void setTimeFormat(@TimeFormat int timeFormatStyle) {
        if (this.timeFormatStyle != timeFormatStyle) {
            this.timeFormatStyle = timeFormatStyle;
            updateTimeFormat();
        }
    }

    @TimeFormat
    public int getTimeFormat() {
        return timeFormatStyle;
    }

    private void updateTimeFormat() {
        lock.lock();
        try {
            boolean update = false;
            long threadId = Thread.currentThread().getId();
            if (lastThreadId == null || threadId != lastThreadId) {
                update = true;
                lastThreadId = threadId;
                if (BuildConfig.DEBUG) {
                    logger.debug("thread updated; new tid: {}", threadId);
                }
            }
            int contextHash = getContext().hashCode();
            if (lastContextHash == null || contextHash != lastContextHash) {
                update = true;
                lastContextHash = contextHash;
                if (BuildConfig.DEBUG) {
                    logger.debug("context updated; new context hash: {}", contextHash);
                }
            }

            if (update) {
                timeFormats = new SimpleDateFormat[TIME_FORMATS.length];
            }

            timeFormat = timeFormats[timeFormatStyle];

            if (timeFormat == null) {
                String pattern;
                Locale locale = Locale.getDefault();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    pattern = DateFormat.is24HourFormat(getContext()) ?
                            TIME_FORMATS[timeFormatStyle] : TIME_FORMATS_12[timeFormatStyle];
                    if (BuildConfig.DEBUG) {
                        logger.debug("locale: {} raw pattern: {}", locale, pattern);
                    }
                    pattern = DateFormat.getBestDateTimePattern(locale, pattern);
                } else {
                    pattern = TIME_FORMATS_OLD_API[timeFormatStyle];
                }

                if (BuildConfig.DEBUG) {
                    logger.debug("pattern: {}", pattern);
                }
                timeFormat = new SimpleDateFormat(pattern, locale);
                timeFormats[timeFormatStyle] = timeFormat;
            }
        } finally {
            lock.unlock();
        }

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
