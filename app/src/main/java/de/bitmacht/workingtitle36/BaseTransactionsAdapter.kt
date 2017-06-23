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

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

import de.bitmacht.workingtitle36.view.BaseTransactionView

typealias ClickListener = (BaseTransactionsAdapter<*>, Int) -> Unit

abstract class BaseTransactionsAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    private var maxValueTextLength = 0
    private var valueWidth = 0
    private var recyclerView: RecyclerView? = null
    private var itemClickListener: ClickListener? = null

    /**
     * This should be called by the implementing class at the end of the onBindViewHolder method.
     * It unifies the widths of the ValueView currently visible.
     * @param transactionView The BaseTransactionView that has been bound
     */
    protected fun onPostBind(transactionView: BaseTransactionView) {
        val valueTextLength = transactionView.valueTextLength
        if (valueTextLength > maxValueTextLength) {
            // the new value text is longer than the previous length of the value texts
            maxValueTextLength = valueTextLength
            valueWidth = transactionView.getValueTextWidth()
            transactionView.setValueViewWidth(valueWidth)
            updateValueWidths()
        } else if (maxValueTextLength > valueTextLength) {
            // the reused value view width does not match the value views
            transactionView.setValueViewWidth(valueWidth)
        }
    }

    private fun updateValueWidths() {
        // it seems that there is no way to find the views that are not visible and thus scrapped,
        // but not recycled (yet); they may come back without rebinding, which is bad
        val lm = recyclerView!!.layoutManager as LinearLayoutManager
        var first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == -1) {
            return
        }
        while (first <= last) {
            val transactionView = lm.findViewByPosition(first) as BaseTransactionView
            transactionView.setValueViewWidth(valueWidth)
            first++
        }

        recyclerView!!.post { recyclerView!!.requestLayout() }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        if (this.recyclerView != null) {
            throw UnsupportedOperationException("Attaching this adapter to more than one RecyclerView is not allowed")
        }
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = null
    }

    /**
     * Set a listener for click events on the items in this adapter
     */
    fun setOnItemClickListener(listener: ClickListener?) {
        itemClickListener = listener
    }

    private fun performItemClick(viewHolder: ClickableTransactionVH) {
        itemClickListener?.invoke(this, viewHolder.adapterPosition)
    }

    open inner class BaseTransactionVH(transactionView: BaseTransactionView) : RecyclerView.ViewHolder(transactionView)

    inner class ClickableTransactionVH(transactionView: BaseTransactionView) : BaseTransactionVH(transactionView), View.OnClickListener {
        init {
            transactionView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            performItemClick(this)
        }
    }
}
