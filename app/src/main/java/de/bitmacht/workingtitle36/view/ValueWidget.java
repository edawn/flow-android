package de.bitmacht.workingtitle36.view;

import de.bitmacht.workingtitle36.Value;

public interface ValueWidget {
    /**
     * Sets the amount that will be displayed
     * @param value The amount
     * @return The text that will be displayed
     */
    String setValue(Value value);

    /**
     * Return the Value represented by this widget
     */
    Value getValue();
}
