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

package de.bitmacht.workingtitle36;

import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import de.bitmacht.workingtitle36.view.BaseTransactionView;

abstract class BaseTransactionsAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private int maxValueTextLength = 0;
    private int valueWidth = 0;
    private RecyclerView recyclerView;
    private OnItemClickListener itemClickListener;

    /**
     * This should be called by the implementing class at the end of the onBindViewHolder method.
     * It unifies the widths of the ValueView currently visible.
     * @param transactionView The BaseTransactionView that has been bound
     */
    protected void onPostBind(BaseTransactionView transactionView) {
        int valueTextLength = transactionView.getValueTextLength();
        if (valueTextLength > maxValueTextLength) {
            // the new value text is longer than the previous length of the value texts
            maxValueTextLength = valueTextLength;
            valueWidth = transactionView.getValueTextWidth();
            transactionView.setValueViewWidth(valueWidth);
            updateValueWidths();
        } else if (maxValueTextLength > valueTextLength) {
            // the reused value view width does not match the value views
            transactionView.setValueViewWidth(valueWidth);
        }
    }

    private void updateValueWidths() {
        LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
        int first = lm.findFirstVisibleItemPosition();
        int last = lm.findLastVisibleItemPosition();
        if (first == -1) {
            return;
        }
        while (first <= last) {
            BaseTransactionView transactionView = (BaseTransactionView) lm.findViewByPosition(first);
            transactionView.setValueViewWidth(valueWidth);
            first++;
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        if (this.recyclerView != null) {
            throw new UnsupportedOperationException("Attaching this adapter to more than one RecyclerView is not allowed");
        }
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = null;
    }

    /**
     * Set a listener for click events on the items in this adapter
     */
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        itemClickListener = listener;
    }

    private void performItemClick(ClickableTransactionVH viewHolder) {
        if (itemClickListener != null) {
            itemClickListener.onItemClick(this, viewHolder.getAdapterPosition());
        }
    }

    class BaseTransactionVH extends RecyclerView.ViewHolder {
        public BaseTransactionVH(BaseTransactionView transactionView) {
            super(transactionView);
        }
    }

    class ClickableTransactionVH extends BaseTransactionVH implements View.OnClickListener {
        public ClickableTransactionVH(BaseTransactionView transactionView) {
            super(transactionView);
            transactionView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            performItemClick(this);
        }
    }

    public interface OnItemClickListener {
        /**
         * This will be called when an item has been clicked
         * @param adapter The adapter holding the item clicked
         * @param adapterPosition The position of the item in the adapter
         * @see android.support.v7.widget.RecyclerView.ViewHolder#getAdapterPosition()
         */
        void onItemClick(BaseTransactionsAdapter adapter, int adapterPosition);
    }
}
