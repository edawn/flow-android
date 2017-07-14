/*
 * Copyright 2017 Kamil Sartys
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

object ValueUtils {

    data class SpentResult(
            val currentDay: Value,
            val beforeCurrentDay: Value,
            val total: Value,
            val hasTransactions: Boolean,
            val hasTransactionsCurrentDay: Boolean)

    fun calculateSpent(transactions: List<TransactionsModel>, currencyCode: String, periods: Periods? = null): SpentResult {
        val startOfDayMillis = periods?.shortStart?.millis ?: 0
        val endOfDayMillis = periods?.shortEnd?.millis ?: 0
        var valueBeforeDay = Value(currencyCode, 0)
        var valueDay = Value(currencyCode, 0)
        var valueTotal = Value(currencyCode, 0)
        var hasTransactionsDay = false
        for (transact in transactions) {
            val edit = transact.mostRecentEdit
            if (edit == null) {
                logw("transaction lacks most recent edit: $transact")
                continue
            }
            try {
                val transactionTime = edit.transactionTime
                if (transactionTime < startOfDayMillis) {
                    valueBeforeDay = valueBeforeDay.add(edit.value)
                } else if (transactionTime < endOfDayMillis) {
                    hasTransactionsDay = true
                    valueDay = valueDay.add(edit.value)
                } else {
                    valueTotal = valueTotal.add(edit.value)
                }
            } catch (e: Value.CurrencyMismatchException) {
                logw("unable to add: $edit: ${e.message}")
            }
        }

        return SpentResult(valueDay, valueBeforeDay, valueTotal.add(valueDay).add(valueBeforeDay),
                !transactions.isEmpty(), hasTransactionsDay)
    }

    /**
     * Calculates the income for the long period.
     * Only regular transactions having currencyCode will be considered
     */
    fun calculateIncome(regulars: List<RegularModel>, currencyCode: String, period: Periods): Value {
        return Value(currencyCode, 0).addAll(regulars.filter { it.currency == currencyCode }
                .map { it.getCumulativeValue(period.longStart, period.longEnd) })
    }
}