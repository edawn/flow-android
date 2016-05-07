package de.bitmacht.workingtitle36;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsArrayAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private List<TransactionsModel> transactions;

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TransactionView transactionView = (TransactionView) inflater.inflate(R.layout.transaction_view, parent, false);

        transactionView.setTimeFormat(TimeView.TIME_FORMAT_TIMEDATE_SHORT);
        return new BaseTransactionsAdapter.BaseTransactionVH(transactionView);
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
}
