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

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Currency
import java.util.Locale

class MyApplication : Application() {

    // necessary to hold a strong reference
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener({sharedPreferences, key ->
        logger.info("pref changed")
        if (key == getString(R.string.pref_currency_key)) {
            updateCurrency(sharedPreferences)
        }
    })

    override fun onCreate() {
        super.onCreate()

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        initPrefs(prefs)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        updateCurrency(prefs)
    }

    private fun initPrefs(prefs: SharedPreferences) {
        val prefKey = getString(R.string.pref_currency_key)
        if (!prefs.contains(prefKey)) {
            val defaultCurrency = Currency.getInstance(Locale.getDefault())
            prefs.edit().putString(prefKey, defaultCurrency.currencyCode).apply()
        }
    }

    fun updateCurrency(prefs: SharedPreferences) {
        val currencyCode: String? = prefs.getString(getString(R.string.pref_currency_key), null)
        var newCurrency: Currency? = null
        if (currencyCode != null) {
            try {
                newCurrency = Currency.getInstance(currencyCode)
            } catch (e: IllegalArgumentException) {
                if (BuildConfig.DEBUG) {
                    logger.warn("unknown currency code: {}", currencyCode)
                }
            }
        }
        if (newCurrency == null) {
            if(BuildConfig.DEBUG) {
                logger.warn("loading currency from preferences failed")
            }
        } else {
            currency = newCurrency
        }
        if (BuildConfig.DEBUG) {
            logger.info("new currency: {}", currency)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MyApplication::class.java)

        var currency: Currency = Currency.getInstance(Locale.getDefault())
    }
}
