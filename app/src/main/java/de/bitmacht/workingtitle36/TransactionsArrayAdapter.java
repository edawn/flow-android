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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.bitmacht.workingtitle36.view.TransactionView;

/**
 * This adapter holds an array of transactions; it also creates a dependent adapter that exposes a
 * subset of the transactions of its parent
 */
public class TransactionsArrayAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsArrayAdapter.class);

    /** will be non-null in the subadapter */
    private TransactionsArrayAdapter parent = null;
    /** the one and only subadapter of this adapter; will be null in the subadapter */
    private TransactionsArrayAdapter sub = null;

    private List<TransactionsModel> transactions;
    private int subIndexStart = 0;
    private int subIndexEnd = 0;

    public TransactionsArrayAdapter() {
        sub = new TransactionsArrayAdapter(this);
    }

    private TransactionsArrayAdapter(TransactionsArrayAdapter parent) {
        this.parent = parent;
    }

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TransactionView transactionView = (TransactionView) inflater.inflate(R.layout.transaction_view, parent, false);
        return new ClickableTransactionVH(transactionView);
    }

    @Override
    public int getItemCount() {
        if (parent == null) {
            return transactions == null ? 0 : transactions.size();
        } else {
            return parent.transactions == null ? 0 : parent.subIndexEnd - parent.subIndexStart;
        }
    }

    @Override
    public void onBindViewHolder(BaseTransactionsAdapter.BaseTransactionVH holder, int position) {
        TransactionView transactionView = (TransactionView) holder.itemView;
        TransactionsModel transaction = parent != null ?
                parent.transactions.get(parent.subIndexStart + position) : transactions.get(position);
        transactionView.setData(transaction.getMostRecentEdit());
        onPostBind(transactionView);
    }

    /**
     * Set the transaction that will be represented by this adapter and its subadapter
     * @param transactions The transactions to be represented
     * @param subStartTime The start of the time range which the subadapter will represent (including; in ms since the epoch)
     * @param subEndTime The end of the time range which the subadapter will represent (excluding; in ms since the epoch)
     */
    public void setData(List<TransactionsModel> transactions, long subStartTime, long subEndTime) {
        if (parent != null) {
            throw new UnsupportedOperationException("not allowed for a subadapter");
        }
        this.transactions = transactions;
        setSubRange(subStartTime, subEndTime);
        notifyDataSetChanged();
    }

    /**
     * Remove the item
     * @return The model of the item removed
     */
    public TransactionsModel removeItem(BaseTransactionVH holder) {
        int position = holder.getAdapterPosition();
        TransactionsModel transactionRemoved;
        if (parent != null) {
            int parentPos = parent.subIndexStart + position;
            transactionRemoved = parent.transactions.remove(parentPos);
            parent.subIndexEnd--;
            parent.notifyItemRemoved(parentPos);
        } else {
            transactionRemoved = transactions.remove(position);
            Integer subPos = null;
            if (subIndexStart <= position && position < subIndexEnd) {
                subPos = position - subIndexStart;
            }
            if (position < subIndexEnd) {
                subIndexEnd--;
            }
            if (position < subIndexStart) {
                subIndexStart--;
            }
            if (subPos != null) {
                sub.notifyItemRemoved(subPos);
            }
        }
        notifyItemRemoved(position);
        return transactionRemoved;
    }

    /**
     * Return the model associated with a adapter position
     * @return The model or null if there is no item for the given position
     */
    @Nullable
    public TransactionsModel getModel(int position) {
        try {
            if (parent != null) {
                return parent.transactions.get(parent.subIndexStart + position);
            }
            return transactions.get(position);
        } catch (IndexOutOfBoundsException e) {
            if (BuildConfig.DEBUG) {
                logger.trace("No item at requested position: {}", e);
            }
        }
        return null;
    }

    /**
     * Return an adapter, that depends on this adapters data source
     */
    public TransactionsArrayAdapter getSubAdapter() {
        if (parent != null) {
            throw new UnsupportedOperationException("not allowed for a subadapter");
        }
        return sub;
    }

    /**
     * Define the time range for the subadapter
     * @param startTime The start of the time range (including; in ms since the epoch)
     * @param endTime The end of the time range (excluding; in ms since the epoch)
     */
    public void setSubRange(long startTime, long endTime) {
        if (parent != null) {
            throw new UnsupportedOperationException("not allowed for a subadapter");
        }
        if (transactions == null) {
            return;
        }
        Integer startIndex = null;
        Integer endIndex = null;
        final int size = transactions.size();
        int i = 0;

        while (i < size) {
            long trTime = transactions.get(i).getMostRecentEdit().getTransactionTime();
            if (i == 0 && startTime <= trTime) {
                startIndex = 0;
            }
            if (startIndex == null && startTime < trTime) {
                startIndex = i;
            }
            if (endIndex == null && endTime <= trTime) {
                endIndex = i;
            }
            if (i == size - 1 && endIndex == null) {
                endIndex = size;
            }
            i++;
        }
        if (startIndex == null || endIndex == null) {
            // this will happen when transactions is empty
            startIndex = endIndex = 0;
        } else if (endIndex < startIndex) {
            if (BuildConfig.DEBUG) {
                logger.warn("start/end index {}/{} for start/end time {}/{}", startIndex, endIndex, startTime, endTime);
            }
            startIndex = endIndex = 0;
        }
        subIndexStart = startIndex;
        subIndexEnd = endIndex;
        sub.notifyDataSetChanged();
    }

}
