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

import android.content.ContentValues
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
import javax.inject.Inject
import kotlin.collections.HashMap
import kotlin.concurrent.withLock

class DBManager @Inject constructor(dbHelper: DBHelper) {

//    val fusedSubject = BehaviorSubject.create<FusedResult>()
//    val transactionsMap: HashMap<TransactionsParams, TransactionsResult> = HashMap()

    private val readableDB = dbHelper.readableDatabase
    private val writableDB = dbHelper.writableDatabase

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
            writableDB.insertWithOnConflict(DBHelper.REGULARS_TABLE_NAME, null,
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
            val count = writableDB.delete(DBHelper.REGULARS_TABLE_NAME, "${DBHelper.REGULARS_KEY_ID} = ?", arrayOf(regularId.toString()))
            if (count != 1) logw("$count rows deleted; expected one; regular id: $regularId")
        }.subscribeOn(dbScheduler)
                .doOnComplete {
                    logd("delete-complete")
                    requeryRegular()
                }.subscribe()
    }

    private fun requeryRegular() {
        regularsSubject.onNext(queryRegulars())
    }

    fun getRegularsObservable(): Observable<RegularsResult> {
        logd("-")
        return regularsObs
    }

    /** Synchronously query regulars */
    private fun queryRegulars(includeDisabled: Boolean = false): RegularsResult {
        logd("-")
        return RegularsResult(readableDB.rawQuery(if (includeDisabled) DBHelper.REGULARS_QUERY else DBHelper.ACTIVE_REGULARS_QUERY, null)
                .use { cursor ->
                    with(ArrayList<RegularModel>(cursor.count)) {
                        while (cursor.moveToNext()) add(RegularModel(cursor))
                        this
                    }
                }
                , includeDisabled)
    }

    private inner class TransactionsSubjectWrapper(val params: TransactionsParams) {
        private val lock = ReentrantLock()
        val subject = BehaviorSubject.create<TransactionsResult>()
        fun getObservable() = subject.doOnSubscribe {
            logd("-")
            lock.withLock {
                if (!subject.hasValue()) {
                    subject.onNext(queryTransactions(params))
                }
            }
        }
    }

    private val transactionsSubjects = HashMap<TransactionsParams, TransactionsSubjectWrapper>()

    fun getTransactionsObservable(periods: Periods): Observable<TransactionsResult> {
        var subjectWrapper = transactionsSubjects[TransactionsParams.fromPeriods(periods)]
        if (subjectWrapper == null) {
            subjectWrapper = TransactionsSubjectWrapper(TransactionsParams.fromPeriods(periods))
            transactionsSubjects.put(subjectWrapper.params, subjectWrapper)
        }
        return subjectWrapper.getObservable()
    }

    /** Synchronously query transactions */
    fun queryTransactions(transactionsParams: TransactionsParams): TransactionsResult {
        return TransactionsResult(readableDB.rawQuery(DBHelper.TRANSACTIONS_EDITS_TIME_SPAN_QUERY,
                    arrayOf(transactionsParams.startMillis.toString(), transactionsParams.endMillis.toString())).use { cursor ->
                ArrayList<TransactionsModel>(cursor.count).apply {
                    while (cursor.moveToNext())
                        add(TransactionsModel.getInstanceWithEdit(cursor))
                }
            }, transactionsParams)
    }

    fun updateTransaction(transaction: TransactionsModel) {
        logd("-")
        Completable.fromCallable {
            writableDB.insertWithOnConflict(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                    transaction.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        }.subscribeOn(dbScheduler).doOnComplete {
            requeryTransactions()
        }.subscribe()
    }

    fun deleteTransaction(transaction: TransactionsModel, undelete: Boolean = false) {
        updateTransaction(transaction.copy().apply { isRemoved = !undelete })
    }

    fun requeryTransactions() {
        logd("-")
        transactionsSubjects.values.forEach {
            logd("${it.params}")
            it.subject.onNext(queryTransactions(it.params))
        }
    }

    fun createEdit(edit: Edit) {
        Completable.fromCallable {
            try {
                val db = writableDB
                db.beginTransaction()
                try {

                    var transactionId = edit.transaction
                    var parentId: Long? = null
                    var sequence = 0

                    var createNewTransaction = true
                    if (transactionId != null) {
                        val transactionsCount =
                                db.rawQuery(DBHelper.TRANSACTION_QUERY, arrayOf(java.lang.Long.toString(transactionId))).use { it.count }
                        if (transactionsCount == 0) {
                            transactionId = null
                        } else {
                            parentId = edit.parent
                            createNewTransaction = false
                            if (transactionsCount > 1) logw("this should not happen: more than one resulting row")
                        }
                    }

                    if (createNewTransaction) {
                        // transaction does not exist; insert new
                        transactionId = db.insertOrThrow(DBHelper.TRANSACTIONS_TABLE_NAME, null,
                                ContentValues().apply { put(DBHelper.TRANSACTIONS_KEY_IS_REMOVED, false) })
                    } else {
                        db.rawQuery(DBHelper.EDITS_FOR_TRANSACTION_QUERY, arrayOf(java.lang.Long.toString(transactionId!!))).use { cursor ->
                            logd("${cursor.count} edits for transaction $transactionId")
                            while (cursor.moveToNext()) {
                                val tmpEdit = Edit(cursor)
                                sequence = Math.max(sequence, tmpEdit.sequence!! + 1)
                                if (edit.parent == tmpEdit.id) parentId = edit.parent
                            }
                        }
                        if (parentId == null)
                            throw Exception("No parent found for edit ${edit.id} in transaction $transactionId")
                    }

                    db.insertOrThrow(DBHelper.EDITS_TABLE_NAME, null, ContentValues(10).apply {
                        put(DBHelper.EDITS_KEY_PARENT, parentId)
                        put(DBHelper.EDITS_KEY_TRANSACTION, transactionId)
                        put(DBHelper.EDITS_KEY_SEQUENCE, sequence)
                        put(DBHelper.EDITS_KEY_IS_PENDING, false)
                        put(DBHelper.EDITS_KEY_TRANSACTION_TIME, edit.transactionTime)
                        put(DBHelper.EDITS_KEY_TRANSACTION_DESCRIPTION, edit.transactionDescription)
                        put(DBHelper.EDITS_KEY_TRANSACTION_LOCATION, edit.transactionLocation)
                        put(DBHelper.EDITS_KEY_TRANSACTION_CURRENCY, edit.transactionCurrency)
                        put(DBHelper.EDITS_KEY_TRANSACTION_AMOUNT, edit.transactionAmount)
                    })
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                loge("creating edit failed: edit: $edit", e)
            }
        }.subscribeOn(dbScheduler).doOnComplete {
            requeryTransactions()
        }.subscribe()
    }

    fun getSuggestions(column: String, query: String): Single<SuggestionsResult> {
        return Single.create { e: SingleEmitter<SuggestionsResult> ->
            e.onSuccess(SuggestionsResult(readableDB
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

    data class TransactionsParams(val startMillis: Long, val endMillis: Long) {
        companion object {
            fun fromPeriods(periods: Periods): TransactionsParams = TransactionsParams(periods.longStart.millis, periods.longEnd.millis)
        }
    }
    data class FusedParams(val transaction: TransactionsParams, val includeDisabled: Boolean)

    data class RegularsResult(val regulars: ArrayList<RegularModel>, val includeDisabled: Boolean)
    data class TransactionsResult(val transactions: ArrayList<TransactionsModel>, val params: TransactionsParams)
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
    }
}