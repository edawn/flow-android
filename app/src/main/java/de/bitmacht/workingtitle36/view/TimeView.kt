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

package de.bitmacht.workingtitle36.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.support.annotation.IntDef
import android.text.format.DateFormat
import android.util.AttributeSet
import android.widget.TextView
import de.bitmacht.workingtitle36.BuildConfig
import de.bitmacht.workingtitle36.R
import de.bitmacht.workingtitle36.logd
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock

/**
 * Displays a time.
 */
class TimeView : TextView {

    @IntDef(TIME_FORMAT_TIME.toLong(), TIME_FORMAT_DATE.toLong(), TIME_FORMAT_TIMEDATE_SHORT.toLong())
    @Retention(AnnotationRetention.SOURCE)
    annotation class TimeFormat

    private var timeFormatStyle = TIME_FORMAT_TIME
    private var timeFormat: SimpleDateFormat? = null

    /**
     * Set the time to be displayed.
     * @param time The time in ms since the epoch.
     */
    var time: Long = 0
        set(time) {
            if (this.time == time) {
                return
            }
            field = time
            update()
        }

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr, 0)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TimeView, defStyleAttr, defStyleRes)
        timeFormatStyle = a.getInteger(R.styleable.TimeView_timeFormat, timeFormatStyle)
        a.recycle()

        updateTimeFormat()
    }

    fun setTimeFormat(@TimeFormat timeFormatStyle: Int) {
        if (this.timeFormatStyle != timeFormatStyle) {
            this.timeFormatStyle = timeFormatStyle
            updateTimeFormat()
        }
    }

    @TimeFormat
    fun getTimeFormat(): Int {
        return timeFormatStyle
    }

    private fun updateTimeFormat() {
        lock.lock()
        try {
            var update = false
            val threadId = Thread.currentThread().id
            if (lastThreadId == null || threadId != lastThreadId) {
                update = true
                lastThreadId = threadId
                logd("thread updated; new tid: $threadId")
            }
            val contextHash = context.hashCode()
            if (lastContextHash == null || contextHash != lastContextHash) {
                update = true
                lastContextHash = contextHash
                logd("context updated; new context hash: $contextHash")
            }

            if (update) {
                timeFormats = arrayOfNulls<SimpleDateFormat>(TIME_FORMATS.size + 1)
            }

            timeFormat = timeFormats[timeFormatStyle]

            if (timeFormat == null) {
                var pattern: String
                val locale = Locale.getDefault()

                if (timeFormatStyle == TIME_FORMAT_TIMEDATE_SHORT_FIXED) {
                    pattern = TIME_FORMATS_OLD_API[2]
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        pattern = if (DateFormat.is24HourFormat(context))
                            TIME_FORMATS[timeFormatStyle]
                        else
                            TIME_FORMATS_12[timeFormatStyle]
                        logd("locale: $locale raw pattern: $pattern")
                        pattern = DateFormat.getBestDateTimePattern(locale, pattern)
                    } else {
                        pattern = TIME_FORMATS_OLD_API[timeFormatStyle]
                    }
                }

                logd("pattern: $pattern")
                timeFormat = SimpleDateFormat(pattern, locale)
                timeFormats[timeFormatStyle] = timeFormat
            }
        } finally {
            lock.unlock()
        }

        update()
    }

    private fun update() {
        if (this.time != 0L) {
            text = timeFormat!!.format(Date(this.time))
        }
    }

    companion object {

        const val TIME_FORMAT_TIME = 0
        const val TIME_FORMAT_DATE = 1
        const val TIME_FORMAT_TIMEDATE_SHORT = 2
        const val TIME_FORMAT_TIMEDATE_SHORT_FIXED = 3

        private val TIME_FORMATS = arrayOf("Hm", "yMd", "MMddHm")
        /**
         * 12-hour-cycle formats
         */
        private val TIME_FORMATS_12 = arrayOf("hma", "yMd", "MMddhma")
        /**
         * Fallback for api levels before 18. See [android.text.format.DateFormat.getBestDateTimePattern]
         */
        private val TIME_FORMATS_OLD_API = arrayOf("HH:mm", "yyyy-MM-dd", "MM-dd HH:mm")

        private val lock = ReentrantLock()
        private var lastThreadId: Long? = null
        private var lastContextHash: Int? = null
        private lateinit var timeFormats: Array<SimpleDateFormat?>
    }
}
