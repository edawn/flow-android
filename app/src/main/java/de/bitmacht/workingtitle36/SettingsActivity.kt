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

package de.bitmacht.workingtitle36

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.ListPreference
import android.support.v7.preference.PreferenceFragmentCompat

import org.slf4j.LoggerFactory
import java.util.Currency
import java.util.Locale
import java.util.TreeMap

class SettingsActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        @SuppressLint("StringFormatMatches")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            addPreferencesFromResource(R.xml.preferences)

            val currencyListPref = findPreference(getString(R.string.pref_currency_key)) as ListPreference

            val currencies: Collection<Currency>

            // default system currency
            var defaultCurrencyCode: String? = null
            try {
                defaultCurrencyCode = Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (e: IllegalArgumentException) {
                if (BuildConfig.DEBUG) {
                    logger.warn("no currency for default locale")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                currencies = Currency.getAvailableCurrencies()
            } else {
                val locales = Locale.getAvailableLocales()
                val codeToCurrency = TreeMap<String, Currency>()
                for (locale in locales) {
                    var currency: Currency? = null
                    try {
                        currency = Currency.getInstance(locale)
                    } catch (e: IllegalArgumentException) {
                        if (BuildConfig.DEBUG) {
                            logger.warn("locale without currency: {}", locale)
                        }
                        continue
                    }

                    if (codeToCurrency.containsKey(currency!!.currencyCode)) {
                        continue
                    }
                    codeToCurrency.put(currency.currencyCode, currency)
                }
                currencies = codeToCurrency.values
            }

            val currencyCount = currencies.size
            val currencyNames = arrayOfNulls<CharSequence>(currencyCount)
            val currencyCodes = arrayOfNulls<CharSequence>(currencyCount)
            // reserve room for the default currency at the top of the list
            // this will fail if the default currency is not in the list of available currencies
            var i = if (defaultCurrencyCode == null) 0 else 1
            for (currency in currencies) {
                val currencyCode = currency.currencyCode
                var ri = i++
                if (defaultCurrencyCode != null && defaultCurrencyCode == currencyCode) {
                    ri = 0
                    i--
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    currencyNames[ri] = getString(R.string.currency_list_item, currency.displayName, currencyCode)
                } else {
                    currencyNames[ri] = getString(R.string.currency_list_item, currencyCode)
                }
                currencyCodes[ri] = currencyCode
            }

            currencyListPref.entries = currencyNames
            currencyListPref.entryValues = currencyCodes
        }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {}
    }

    companion object {

        private val logger = LoggerFactory.getLogger(SettingsActivity::class.java)
    }
}
