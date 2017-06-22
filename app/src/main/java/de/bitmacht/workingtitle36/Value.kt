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

import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Combines an amount and a ISO 4217 currency code
 */
class Value : Parcelable {
    /**
     * The ISO 4217 currency code associated with this object
     */
    val currencyCode: String
    /**
     * The number of minor currency units represented by this object
     */
    val amount: Long

    /**
     * Create a new instance of Value
     * @param amount The number of minor currency units
     * *
     * @param currencyCode The ISO 4217 currency code
     */
    constructor(currencyCode: String, amount: Long) {
        this.currencyCode = currencyCode
        this.amount = amount
    }

    /**
     * Returns a new Value having the currency of this amount, but with a different amount
     */
    fun withAmount(amount: Long): Value {
        return Value(this.currencyCode, amount)
    }

    /**
     * Returns the Currency associated with this object's currency code
     */
    val currency: Currency
        get() = Currency.getInstance(currencyCode)

    /**
     * Returns a new Value with the sum of this and the other Value
     * @throws CurrencyMismatchException If the currencies differ
     */
    @Throws(CurrencyMismatchException::class)
    fun add(other: Value): Value {
        if (currencyCode != other.currencyCode) {
            throw CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode)
        }
        return Value(currencyCode, amount + other.amount)
    }

    /**
     * Return a new Value with the sum of this and all Values from others
     * @throws CurrencyMismatchException If any currency code differs from this
     */
    @Throws(CurrencyMismatchException::class)
    fun addAll(others: Array<Value>): Value {
        var sumount = amount
        for (other in others) {
            if (currencyCode != other.currencyCode) {
                throw CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode)
            }
            sumount += other.amount
        }
        return Value(currencyCode, sumount)
    }

    /**
     * Return a new Value with the sum of this and all Values from others
     * @throws CurrencyMismatchException If any currency code differs from this
     */
    @Throws(CurrencyMismatchException::class)
    fun addAll(others: Collection<Value>): Value {
        var sumount = amount
        for (other in others) {
            if (currencyCode != other.currencyCode) {
                throw CurrencyMismatchException("Unable to add " + other.currencyCode + " to " + currencyCode)
            }
            sumount += other.amount
        }
        return Value(currencyCode, sumount)
    }

    /**
     * Builds an appropriate String for a given currency and amount.
     * Uses the default locale.
     * @return The generated String
     */
    val string: String
        get() = getString(Locale.getDefault())

    /**
     * Builds an appropriate String for a given currency and amount.
     * @param locale The Locale to use for the decimal separator
     * *
     * @return The generated String
     */
    fun getString(locale: Locale): String {
        val vs = getValueAndSymbolStrings(locale)
        return vs.first + vs.second
    }

    fun getValueAndSymbolStrings(locale: Locale): Pair<String, String> {
        val currency = currency
        val fractionDigits = currency.defaultFractionDigits
        val symbol = currency.symbol
        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).monetaryDecimalSeparator

        val sign = if (amount < 0) "-" else ""
        val umount = Math.abs(amount)
        var major = umount
        var minor: Long = 0
        val showFraction = fractionDigits > 0
        if (showFraction) {
            val div = Math.pow(10.0, fractionDigits.toDouble()).toLong()
            major = umount / div
            minor = umount % div
        }

        val valueString = if (showFraction)
            String.format("%s%d%s%02d", sign, major, decimalSeparator, minor)
        else
            String.format("%s%d", sign, major)

        return Pair(valueString, symbol)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(currencyCode)
        dest.writeLong(amount)
    }

    private constructor(src: Parcel) {
        currencyCode = src.readString()
        amount = src.readLong()
    }

    class CurrencyMismatchException(s: String) : Exception(s)

    override fun toString(): String {
        return currencyCode + ":" + amount
    }

    companion object {

        @JvmField final val CREATOR: Parcelable.Creator<Value> = object : Parcelable.Creator<Value> {
            override fun createFromParcel(source: Parcel): Value {
                return Value(source)
            }

            override fun newArray(size: Int): Array<Value?> {
                return arrayOfNulls(size)
            }
        }
    }
}
