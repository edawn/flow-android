package de.bitmacht.workingtitle36;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Currency;
import java.util.Locale;
import java.util.TreeMap;

public class SettingsActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(SettingsActivity.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @SuppressLint("StringFormatMatches")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            ListPreference currencyListPref = (ListPreference) findPreference(getString(R.string.pref_currency_key));

            Collection<Currency> currencies;

            // default system currency
            String defaultCurrencyCode = null;
            try {
                defaultCurrencyCode = Currency.getInstance(Locale.getDefault()).getCurrencyCode();
            } catch (IllegalArgumentException e) {
                if (BuildConfig.DEBUG) {
                    logger.warn("no currency for default locale");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                currencies = Currency.getAvailableCurrencies();
            } else {
                Locale[] locales = Locale.getAvailableLocales();
                TreeMap<String, Currency> codeToCurrency = new TreeMap<>();
                for (Locale locale : locales) {
                    Currency currency = null;
                    try {
                        currency = Currency.getInstance(locale);
                    } catch (IllegalArgumentException e) {
                        if (BuildConfig.DEBUG) {
                            logger.warn("locale without currency: {}", locale);
                        }
                        continue;
                    }
                    if (codeToCurrency.containsKey(currency.getCurrencyCode())) {
                        continue;
                    }
                    codeToCurrency.put(currency.getCurrencyCode(), currency);
                }
                currencies = codeToCurrency.values();
            }

            int currencyCount = currencies.size();
            CharSequence[] currencyNames = new CharSequence[currencyCount];
            CharSequence[] currencyCodes = new CharSequence[currencyCount];
            // reserve room for the default currency at the top of the list
            // this will fail if the default currency is not in the list of available currencies
            int i = defaultCurrencyCode == null ?  0 : 1;
            for (Currency currency : currencies) {
                String currencyCode = currency.getCurrencyCode();
                int ri = i++;
                if (defaultCurrencyCode != null && defaultCurrencyCode.equals(currencyCode)) {
                    ri = 0;
                    i--;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    currencyNames[ri] = getString(R.string.currency_list_item, currency.getDisplayName(), currencyCode);
                } else {
                    currencyNames[ri] = getString(R.string.currency_list_item, currencyCode);
                }
                currencyCodes[ri] = currencyCode;
            }

            currencyListPref.setEntries(currencyNames);
            currencyListPref.setEntryValues(currencyCodes);
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
        }
    }
}
