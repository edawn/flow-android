package de.bitmacht.workingtitle36;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsArrayAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionsArrayAdapter.class);

    /** will be non-null in the subadapter */
    private TransactionsArrayAdapter parent = null;
    /** the one and only subadapter of this adapter; will be null in the subadapter */
    private TransactionsArrayAdapter sub = null;

    private List<TransactionsModel> transactions;

    public TransactionsArrayAdapter() {}

    private TransactionsArrayAdapter(TransactionsArrayAdapter parent) {
        this.parent = parent;
    }

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TransactionView transactionView = (TransactionView) inflater.inflate(R.layout.transaction_view, parent, false);

        transactionView.setTimeFormat(TimeView.TIME_FORMAT_TIMEDATE_SHORT);
        return new ClickableTransactionVH(transactionView);
    }

    @Override
    public int getItemCount() {
        return transactions == null ? 0 : transactions.size();
    }

    @Override
    public void onBindViewHolder(BaseTransactionsAdapter.BaseTransactionVH holder, int position) {
        TransactionView transactionView = (TransactionView) holder.itemView;
        transactionView.setData(transactions.get(position).mostRecentEdit);
        onPostBind(transactionView);
    }

    public void setData(List<TransactionsModel> transactions) {
        if (parent != null) {
            throw new UnsupportedOperationException("not allowed for a subadapter");
        }
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    /**
     * Return the model associated with a adapter position
     * @return The model or null if there is no item for the given position
     */
    @Nullable
    public TransactionsModel getModel(int position) {
        try {
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
        if (sub == null) {
            sub = new TransactionsArrayAdapter(this);
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
            long trTime = transactions.get(i).mostRecentEdit.transactionTime;
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
                startIndex = endIndex = 0;
            }
        }
        TransactionsArrayAdapter subAdapter = getSubAdapter();
        subAdapter.transactions = transactions.subList(startIndex, endIndex);
        subAdapter.notifyDataSetChanged();
    }

}
