package de.bitmacht.workingtitle36;

import android.database.Cursor;
import android.view.ViewGroup;

import de.bitmacht.workingtitle36.view.TransactionView;

public class TransactionsAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private Cursor cursor = null;

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        TransactionView transactionView = new TransactionView(parent.getContext());
        return new BaseTransactionsAdapter.BaseTransactionVH(transactionView);
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    @Override
    public void onBindViewHolder(BaseTransactionsAdapter.BaseTransactionVH holder, int position) {
        cursor.moveToPosition(position);
        Edit edit = new Edit(cursor);
        TransactionView transactionView = (TransactionView) holder.itemView;
        transactionView.setData(edit);
        onPostBind(transactionView);
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }
}
