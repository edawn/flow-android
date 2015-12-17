package de.bitmacht.workingtitle36.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

import de.bitmacht.workingtitle36.R;

/**
 * Shows the description.
 */
public class DescriptionView extends TextView {

    private String description;
    private boolean isConcise = false;

    public DescriptionView(Context context) {
        super(context);
    }

    public DescriptionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs, 0, 0);
    }

    public DescriptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DescriptionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DescriptionView, defStyleAttr, defStyleRes);

        try {
            isConcise = a.getBoolean(R.styleable.DescriptionView_isConcise, isConcise);
        } finally {
            a.recycle();
        }
    }

    /**
     * Set the description.
     * @param description The description to be shown; null to clear
     */
    public void setDescription(String description) {
        this.description = description;
        updateDescription();
    }

    /**
     * Returns the brevity of this View. Correlates with the isConcise attributes.
     * @return true If this View shows a short description; false otherwise
     */
    public boolean isConcise() {
        return isConcise;
    }

    /**
     * Set the brevity of the description.
     * @param concise If true, only the first line of the description will be shown
     */
    public void setConcise(boolean concise) {
        if (isConcise != concise) {
            isConcise = !isConcise;
            updateDescription();
        }
    }

    private void updateDescription() {
        String text = null;
        if (description != null) {
            text = description.trim();
            if (isConcise) {
                text = text.split("\\n", 2)[0];
            }
        }
        setText(text);
    }
}
