package de.bitmacht.workingtitle36;

import android.view.ViewGroup;

import java.util.List;

import de.bitmacht.workingtitle36.view.RegularView;

public class RegularsAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private List<RegularModel> regulars;

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        RegularView regularView = new RegularView(parent.getContext());
        return new BaseTransactionsAdapter.BaseTransactionVH(regularView);
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
    }

    /**
     * Remove the item
     * @param holder
     */
    public void removeItem(BaseTransactionVH holder) {
        int position = holder.getAdapterPosition();
        regulars.remove(position);
        notifyItemRemoved(position);
    }
}
