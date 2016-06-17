package de.bitmacht.workingtitle36.view;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.bitmacht.workingtitle36.Value;

public interface ValueWidget {
    /**
     * Sets the amount that will be displayed
     * @param value The amount
     * @return The text that will be displayed
     */
    @NonNull
    String setValue(@NonNull Value value);

    /**
     * Return the Value represented by this widget
     */
    @Nullable
    Value getValue();
}
