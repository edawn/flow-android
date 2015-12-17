package de.bitmacht.workingtitle36;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionVH> {

    private Cursor cursor = null;

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
        ((TransactionView) holder.itemView).setData(edit);

    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    class TransactionVH extends RecyclerView.ViewHolder {
        public TransactionVH(TransactionView transactionView) {
            super(transactionView);
        }
    }
}
