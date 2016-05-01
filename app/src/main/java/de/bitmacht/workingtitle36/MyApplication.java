package de.bitmacht.workingtitle36;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.Locale;

public class MyApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MyApplication.class);

    private static Currency currency = null;

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener = null;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        initPrefs(prefs);

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getString(R.string.pref_currency_key))) {
                    updateCurrency(sharedPreferences);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        updateCurrency(prefs);
    }

    private void initPrefs(SharedPreferences prefs) {
        String prefKey = getString(R.string.pref_currency_key);
        if (!prefs.contains(prefKey)) {
            Currency defaultCurrency = Currency.getInstance(Locale.getDefault());
            prefs.edit().putString(prefKey, defaultCurrency.getCurrencyCode()).apply();
        }
    }

    public void updateCurrency(SharedPreferences prefs) {
        currency = null;
        String currencyCode = null;
        String prefKey = getString(R.string.pref_currency_key);
        if (prefs.contains(prefKey)) {
            currencyCode = prefs.getString(prefKey, null);
        }
        if (currencyCode != null) {
            try {
                currency = Currency.getInstance(currencyCode);
            } catch (IllegalArgumentException e) {
                if (BuildConfig.DEBUG) {
                    logger.warn("unknown currency code: {}", currencyCode);
                }
            }
        }
        if (currency == null) {
            if (BuildConfig.DEBUG) {
                logger.info("using default currency");
            }
            currency = Currency.getInstance(Locale.getDefault());
        }
        if (BuildConfig.DEBUG) {
            logger.info("new currency: {}", currency);
        }
    }

    public static Currency getCurrency() {
        return currency;
    }
}
