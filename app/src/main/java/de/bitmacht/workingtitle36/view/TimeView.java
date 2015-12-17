package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Displays a time.
 */
public class TimeView extends TextView {

    private SimpleDateFormat timeFormat;

    public TimeView(Context context) {
        super(context);
        init();
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        this.timeFormat = new SimpleDateFormat("HH:mm");
    }

    /**
     * Set the time to be displayed.
     * @param time The time in ms since the epoch.
     */
    public void setTime(long time) {
        setText(timeFormat.format(new Date(time)));
    }
}
