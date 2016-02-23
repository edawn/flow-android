package de.bitmacht.workingtitle36.view;

import java.util.Currency;

public interface ValueWidget {
    /**
     * Sets the value that will be displayed
     * @param currency The currency
     * @param cents The value in minor currency units
     * @return The text that will be displayed
     */
    String setValue(Currency currency, long cents);
}
