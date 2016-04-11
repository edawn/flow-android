package de.bitmacht.workingtitle36;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.List;

import de.bitmacht.workingtitle36.view.TimeView;
import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsArrayAdapter extends RecyclerView.Adapter<TransactionsArrayAdapter.TransactionVH> {

    private int maxValueTextLength = 0;
    private int valueWidth = 0;
    private RecyclerView recyclerView;
    private List<TransactionsModel> transactions;

    @Override
    public TransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        TransactionView transactionView = new TransactionView(parent.getContext());
        transactionView.setTimeFormat(TimeView.TIME_FORMAT_TIMEDATE_SHORT);
        return new TransactionVH(transactionView);
    }

    @Override
    public int getItemCount() {
        return transactions == null ? 0 : transactions.size();
    }

    @Override
    public void onBindViewHolder(TransactionVH holder, int position) {
        TransactionView transactionView = ((TransactionView) holder.itemView);
        transactionView.setData(transactions.get(position).mostRecentEdit);
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
            TransactionView transactionView = (TransactionView) lm.findViewByPosition(first);
            transactionView.setValueViewWidth(valueWidth);
            first++;
        }
    }

    public void setData(List<TransactionsModel> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
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

    class TransactionVH extends RecyclerView.ViewHolder {
        public TransactionVH(TransactionView transactionView) {
            super(transactionView);
        }
    }
}
