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

    private List<TransactionsModel> transactions;

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

}
