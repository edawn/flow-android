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
import java.util.*

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

            val currencies: Collection<Currency>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                currencies = Currency.getAvailableCurrencies()
            } else {
                val locales = Locale.getAvailableLocales()
                val codeToCurrency = TreeMap<String, Currency>()
                for (locale in locales) {
                    try {
                        with (Currency.getInstance(locale) ?: continue) {
                            if (!codeToCurrency.containsKey(currencyCode))
                                codeToCurrency.put(currencyCode, this)
                        }
                    } catch (e: IllegalArgumentException) {
                        logw("locale without currency: $locale")
                    }
                }
                currencies = codeToCurrency.values
            }

            val currenciesSorted = currencies.sortedBy { it.currencyCode }

            val currencyCount = currenciesSorted.size
            val currencyNames = arrayOfNulls<CharSequence>(currencyCount)
            val currencyCodes = arrayOfNulls<CharSequence>(currencyCount)

            currenciesSorted.forEachIndexed { index, currency ->
                val currencyCode = currency.currencyCode
                currencyNames[index] =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                            getString(R.string.currency_list_item, currency.displayName, currencyCode)
                        else getString(R.string.currency_list_item, currencyCode)
                currencyCodes[index] = currencyCode
            }

            with(findPreference(getString(R.string.pref_currency_key)) as ListPreference) {
                entries = currencyNames
                entryValues = currencyCodes
            }
        }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {}
    }
}
