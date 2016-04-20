package de.bitmacht.workingtitle36;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import de.bitmacht.workingtitle36.view.BaseTransactionView;
import de.bitmacht.workingtitle36.view.TransactionView;

abstract class BaseTransactionsAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private int maxValueTextLength = 0;
    private int valueWidth = 0;
    private RecyclerView recyclerView;

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
            TransactionView transactionView = (TransactionView) lm.findViewByPosition(first);
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

    class BaseTransactionVH extends RecyclerView.ViewHolder {
        public BaseTransactionVH(BaseTransactionView transactionView) {
            super(transactionView);
        }
    }
}
