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

import android.content.Context
import android.preference.PreferenceManager



object Utils {

    /**
     * Return a String value from the default shared preferences

     * @param context  The Context to use
     * *
     * @param resId    A String resource that contains the key of the value to be retrieved
     * *
     * @param defValue the default value
     * *
     * @return the value of the preference, or defValue if the preference does not exist
     */
    fun getsPref(context: Context, resId: Int, defValue: String): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(resId), defValue)
    }

    /**
     * Return a boolean value from the default shared preferences

     * @param context  The Context to use
     * *
     * @param resId    A String resource that contains the key of the value to be retrieved
     * *
     * @param defValue the default value
     * *
     * @return the value of the preference, or defValue if the preference does not exist
     */
    fun getbPref(context: Context, resId: Int, defValue: Boolean): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(resId), defValue)
    }

    /**
     * Return an int value from the default shared preferences

     * @param context  The Context to use
     * *
     * @param resId    A String resource that contains the key of the value to be retrieved
     * *
     * @param defValue the default value
     * *
     * @return the value of the preference, or defValue if the preference does not exist
     */
    fun getiPref(context: Context, resId: Int, defValue: Int): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(resId), defValue)
    }

    /**
     * Return a float value from the default shared preferences

     * @param context  The Context to use
     * *
     * @param resId    A String resource that contains the key of the value to be retrieved
     * *
     * @param defValue the default value
     * *
     * @return the value of the preference, or defValue if the preference does not exist
     */
    fun getfPref(context: Context, resId: Int, defValue: Float): Float {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(context.getString(resId), defValue)
    }

    /**
     * Set a String value in the default shared preferences

     * @param context The Context to use
     * *
     * @param resId   A String resource that contains the key of the value to be set
     * *
     * @param value   The value to be set
     */
    fun setsPref(context: Context, resId: Int, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(resId), value).apply()
    }

    /**
     * Set a boolean value in the default shared preferences

     * @param context The Context to use
     * *
     * @param resId   A String resource that contains the key of the value to be set
     * *
     * @param value   The value to be set
     */
    fun setbPref(context: Context, resId: Int, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(resId), value).apply()
    }

    /**
     * Set an int value in the default shared preferences

     * @param context The Context to use
     * *
     * @param resId   A String resource that contains the key of the value to be set
     * *
     * @param value   The value to be set
     */
    fun setiPref(context: Context, resId: Int, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(context.getString(resId), value).apply()
    }

    /**
     * Set float value in the default shared preferences

     * @param context The Context to use
     * *
     * @param resId   A String resource that contains the key of the value to be set
     * *
     * @param value   The value to be set
     */
    fun setfPref(context: Context, resId: Int, value: Float) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(context.getString(resId), value).apply()
    }

}