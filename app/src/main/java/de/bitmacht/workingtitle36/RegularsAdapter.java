package de.bitmacht.workingtitle36;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import de.bitmacht.workingtitle36.view.BaseTransactionView;
import de.bitmacht.workingtitle36.view.RegularView;

public class RegularsAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private List<RegularModel> regulars;
    private OnItemClickListener itemClickListener = null;

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RegularView regularView = (RegularView) inflater.inflate(R.layout.regular_view, parent, false);
        return new ClickableVH(regularView);
    }

    @Override
    public void onBindViewHolder(BaseTransactionsAdapter.BaseTransactionVH holder, int position) {
        RegularView regularView = (RegularView) holder.itemView;
        regularView.setData(regulars.get(position));
        onPostBind(regularView);
    }

    @Override
    public int getItemCount() {
        return regulars == null ? 0 : regulars.size();
    }

    public void setData(List<RegularModel> regulars) {
        this.regulars = regulars;
        notifyDataSetChanged();
    }

    /**
     * Remove the item
     * @param holder
     * @return The model of the item removed
     */
    public RegularModel removeItem(BaseTransactionVH holder) {
        int position = holder.getAdapterPosition();
        RegularModel regular = regulars.remove(position);
        notifyItemRemoved(position);
        return regular;
    }

    /**
     * Return the model associated with a particular ViewHolder
     * @return The model; may return null if the data set has been updated recently
     */
    @Nullable
    public RegularModel getModel(@NonNull BaseTransactionVH holder) {
        int pos = holder.getAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) {
            return regulars.get(pos);
        }
        return null;
    }

    /**
     * Set a listener for click events on the items in this adapter
     */
    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        itemClickListener = listener;
    }

    private void performItemClick(ClickableVH viewHolder) {
        if (itemClickListener != null) {
            itemClickListener.onItemClick(this, viewHolder);
        }
    }

    public class ClickableVH extends BaseTransactionVH implements View.OnClickListener {

        public ClickableVH(BaseTransactionView transactionView) {
            super(transactionView);
            transactionView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            performItemClick(this);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(RegularsAdapter adapter, ClickableVH viewHolder);
    }
}
