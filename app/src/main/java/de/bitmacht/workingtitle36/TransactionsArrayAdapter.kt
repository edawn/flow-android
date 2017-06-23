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

import android.view.LayoutInflater
import android.view.ViewGroup

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.bitmacht.workingtitle36.view.TransactionView

/**
 * This adapter holds an array of transactions; it also creates a dependent adapter that exposes a
 * subset of the transactions of its parent
 */
class TransactionsArrayAdapter : BaseTransactionsAdapter<BaseTransactionsAdapter<*>.BaseTransactionVH> {

    /** will be non-null in the subadapter  */
    private var parent: TransactionsArrayAdapter? = null
    /** the one and only subadapter of this adapter; will be null in the subadapter  */
    private var sub: TransactionsArrayAdapter? = null

    private var transactions: MutableList<TransactionsModel>? = null
    private var subIndexStart = 0
    private var subIndexEnd = 0

    constructor() {
        sub = TransactionsArrayAdapter(this)
    }

    private constructor(parent: TransactionsArrayAdapter) {
        this.parent = parent
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTransactionsAdapter<*>.BaseTransactionVH {
        val inflater = LayoutInflater.from(parent.context)
        val transactionView = inflater.inflate(R.layout.transaction_view, parent, false) as TransactionView
        return this.ClickableTransactionVH(transactionView)
    }

    override fun getItemCount(): Int {
        if (parent == null) {
            return if (transactions == null) 0 else transactions!!.size
        } else {
            return if (parent?.transactions == null) 0 else parent!!.subIndexEnd - parent!!.subIndexStart
        }
    }

    override fun onBindViewHolder(holder: BaseTransactionsAdapter<*>.BaseTransactionVH, position: Int) {
        val transactionView = holder.itemView as TransactionView
        val transaction = if (parent != null)
            parent?.transactions!![parent!!.subIndexStart + position]
        else
            transactions!![position]
        transactionView.setData(transaction.mostRecentEdit!!)
        onPostBind(transactionView)
    }

    /**
     * Set the transaction that will be represented by this adapter and its subadapter
     * @param transactions The transactions to be represented
     * *
     * @param subStartTime The start of the time range which the subadapter will represent (including; in ms since the epoch)
     * *
     * @param subEndTime The end of the time range which the subadapter will represent (excluding; in ms since the epoch)
     */
    fun setData(transactions: MutableList<TransactionsModel>, subStartTime: Long, subEndTime: Long) {
        if (parent != null) {
            throw UnsupportedOperationException("not allowed for a subadapter")
        }
        this.transactions = transactions
        setSubRange(subStartTime, subEndTime)
        notifyDataSetChanged()
    }

    /**
     * Remove the item
     * @return The model of the item removed
     */
    fun removeItem(holder: BaseTransactionsAdapter<*>.BaseTransactionVH): TransactionsModel {
        val position = holder.getAdapterPosition()
        val transactionRemoved: TransactionsModel
        if (parent != null) {
            val parentPos = parent!!.subIndexStart + position
            transactionRemoved = parent!!.transactions!!.removeAt(parentPos)
            parent!!.subIndexEnd--
            parent!!.notifyItemRemoved(parentPos)
        } else {
            transactionRemoved = transactions!!.removeAt(position)
            var subPos: Int? = null
            if (subIndexStart <= position && position < subIndexEnd) {
                subPos = position - subIndexStart
            }
            if (position < subIndexEnd) {
                subIndexEnd--
            }
            if (position < subIndexStart) {
                subIndexStart--
            }
            if (subPos != null) {
                sub!!.notifyItemRemoved(subPos)
            }
        }
        notifyItemRemoved(position)
        return transactionRemoved
    }

    /**
     * Return the model associated with a adapter position
     * @return The model or null if there is no item for the given position
     */
    fun getModel(position: Int): TransactionsModel? {
        try {
            if (parent != null) {
                return parent!!.transactions!![parent!!.subIndexStart + position]
            }
            return transactions!![position]
        } catch (e: IndexOutOfBoundsException) {
            if (BuildConfig.DEBUG) {
                logger.trace("No item at requested position: {}", e)
            }
        }

        return null
    }

    /**
     * Return an adapter, that depends on this adapters data source
     */
    val subAdapter: TransactionsArrayAdapter
        get() {
            if (parent != null) {
                throw UnsupportedOperationException("not allowed for a subadapter")
            }
            return sub!!
        }

    /**
     * Define the time range for the subadapter
     * @param startTime The start of the time range (including; in ms since the epoch)
     * *
     * @param endTime The end of the time range (excluding; in ms since the epoch)
     */
    fun setSubRange(startTime: Long, endTime: Long) {
        if (parent != null) {
            throw UnsupportedOperationException("not allowed for a subadapter")
        }
        if (transactions == null) {
            return
        }
        var startIndex: Int? = null
        var endIndex: Int? = null
        val size = transactions!!.size
        var i = 0

        while (i < size) {
            val trTime = transactions!![i].mostRecentEdit!!.transactionTime
            if (i == 0 && startTime <= trTime) {
                startIndex = 0
            }
            if (startIndex == null && startTime < trTime) {
                startIndex = i
            }
            if (endIndex == null && endTime <= trTime) {
                endIndex = i
            }
            if (i == size - 1 && endIndex == null) {
                endIndex = size
            }
            i++
        }
        if (startIndex == null || endIndex == null) {
            // this will happen when transactions is empty
            endIndex = 0
            startIndex = endIndex
        } else if (endIndex < startIndex) {
            if (BuildConfig.DEBUG) {
                logger.warn("start/end index {}/{} for start/end time {}/{}", startIndex, endIndex, startTime, endTime)
            }
            endIndex = 0
            startIndex = endIndex
        }
        subIndexStart = startIndex
        subIndexEnd = endIndex
        sub!!.notifyDataSetChanged()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TransactionsArrayAdapter::class.java)
    }

}
