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

package de.bitmacht.workingtitle36.db

import android.database.sqlite.SQLiteDatabase
import de.bitmacht.workingtitle36.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DBManager private constructor() {

//    val fusedSubject = BehaviorSubject.create<FusedResult>()
//    val transactionsMap: HashMap<TransactionsParams, TransactionsResult> = HashMap()

    // use a single dedicated thread for DB (write) access
    private val dbScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
            .also { it.createWorker().schedule { Thread.currentThread().name = "DB-worker" } }

    private val lock = ReentrantLock()

    private val regularsSubject = BehaviorSubject.create<RegularsResult>()
    private val regularsObs = regularsSubject.doOnSubscribe {
        logd("subscribing")
        lock.withLock {
            if (!regularsSubject.hasValue()) {
                logd("calculating initial value")
                requeryRegular()
            }
        }
    }

    fun updateRegular(regular: RegularModel) {
        Completable.fromCallable {
            logd("updating: regular: $regular")
            getWritableDB().insertWithOnConflict(DBHelper.REGULARS_TABLE_NAME, null,
                    regular.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.subscribeOn(dbScheduler)
                .doOnComplete {
                    logd("update-complete")
                    requeryRegular()
                }.subscribe()
    }

    fun deleteRegular(regularId: Long) {
        Completable.fromCallable {
            logd("deleting: id: $regularId")
            val count = getWritableDB().delete(DBHelper.REGULARS_TABLE_NAME, "${DBHelper.REGULARS_KEY_ID} = ?", arrayOf(regularId.toString()))
            if (count != 1) logw("$count rows deleted; expected one; regular id: $regularId")
        }.subscribeOn(dbScheduler)
                .doOnComplete {
                    logd("delete-complete")
                    requeryRegular()
                }
    }

    private fun requeryRegular() {
        regularsSubject.onNext(queryRegulars())
    }

    fun getRegularsObservable(): Observable<RegularsResult> {
        logd("-")
        return regularsObs
    }

    fun getTransactions(periods: Periods): Single<TransactionsResult> {
        return Single.create({ e: SingleEmitter<TransactionsResult> ->
            e.onSuccess(TransactionsResult(getDB().rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY,
                    arrayOf(periods.longStart.millis.toString(), periods.longEnd.millis.toString())).use { cursor ->
                ArrayList<TransactionsModel>(cursor.count).apply {
                    while (cursor.moveToNext())
                        add(TransactionsModel.getInstanceWithEdit(cursor))
                }
            }, periods))
        })
    }

    /** Synchronously query regulars */
    private fun queryRegulars(includeDisabled: Boolean = false): RegularsResult {
        logd("-")
        return RegularsResult(getDB().rawQuery(if (includeDisabled) DBHelper.REGULARS_QUERY else DBHelper.ACTIVE_REGULARS_QUERY, null)
                .use { cursor ->
                    with(ArrayList<RegularModel>(cursor.count)) {
                        while (cursor.moveToNext()) add(RegularModel(cursor))
                        this
                    }
                }
                , includeDisabled)
    }

    fun getSuggestions(column: String, query: String): Single<SuggestionsResult> {
        return Single.create { e: SingleEmitter<SuggestionsResult> ->
            e.onSuccess(SuggestionsResult(getDB()
                    .rawQuery(String.format(DBHelper.SUGGESTIONS_QUERY, column), arrayOf("$query%"))
                    .use { cursor ->
                        ArrayList<Suggestion>(cursor.count).apply {
                            while (cursor.moveToNext()) {
                                add(Suggestion(cursor.getString(0), cursor.getInt(1)))
                            }
                        }
                    }, column, query))
        }
    }

    private fun getDB(): SQLiteDatabase {
        return MyApplication.dbHelper.readableDatabase
    }

    private fun getWritableDB(): SQLiteDatabase {
        return MyApplication.dbHelper.writableDatabase
    }

    data class TransactionsParams(val startMillis: Long, val endMillis: Long)
    data class FusedParams(val transaction: TransactionsParams, val includeDisabled: Boolean)

    data class RegularsResult(val regulars: ArrayList<RegularModel>, val includeDisabled: Boolean)
    data class TransactionsResult(val transactions: ArrayList<TransactionsModel>, val periods: Periods)
    data class FusedResult(val regulars: RegularsResult, val transactions: TransactionsResult)

    data class SuggestionsResult(val suggestions: ArrayList<Suggestion>, val column: String, val query: String)
    data class Suggestion(val text: String, val frequency: Int)

    companion object {
        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        const val COLUMN_DESCRIPTION = DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION
        /**
         * A valid value for [TransactionsSuggestionsLoader.ARG_COLUMN]
         */
        const val COLUMN_LOCATION = DBHelper.EDITS_KEY_TRANSACTION_LOCATION

        val instance = DBManager()

    }
}