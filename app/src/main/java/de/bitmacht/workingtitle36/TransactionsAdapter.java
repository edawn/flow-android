package de.bitmacht.workingtitle36;

import android.database.Cursor;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionVH> {

    private Cursor cursor = null;

    private int maxValueTextLength = 0;
    private int valueWidth = 0;
    private RecyclerView recyclerView;

    @Override
    public TransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        TransactionView transactionView = new TransactionView(parent.getContext());
        return new TransactionVH(transactionView);
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    @Override
    public void onBindViewHolder(TransactionVH holder, int position) {
        cursor.moveToPosition(position);
        Edit edit = new Edit(cursor);
        TransactionView transactionView = ((TransactionView) holder.itemView);
        transactionView.setData(edit);
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

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
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
