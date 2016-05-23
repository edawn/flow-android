package de.bitmacht.workingtitle36;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.bitmacht.workingtitle36.view.RegularView;

public class RegularsAdapter extends BaseTransactionsAdapter<BaseTransactionsAdapter.BaseTransactionVH> {

    private static final Logger logger = LoggerFactory.getLogger(RegularsAdapter.class);

    private List<RegularModel> regulars;

    @Override
    public BaseTransactionsAdapter.BaseTransactionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RegularView regularView = (RegularView) inflater.inflate(R.layout.regular_view, parent, false);
        return new ClickableTransactionVH(regularView);
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
     * @return The model of the item removed
     */
    public RegularModel removeItem(BaseTransactionVH holder) {
        int position = holder.getAdapterPosition();
        RegularModel regular = regulars.remove(position);
        notifyItemRemoved(position);
        return regular;
    }

    /**
     * Return the model associated with a adapter position
     * @return The model or null if there is no item for the given position
     */
    @Nullable
    public RegularModel getModel(int position) {
        try {
            return regulars.get(position);
        } catch (IndexOutOfBoundsException e) {
            if (BuildConfig.DEBUG) {
                logger.trace("No item at requested position: {}", e);
            }
        }
        return null;
    }
}
