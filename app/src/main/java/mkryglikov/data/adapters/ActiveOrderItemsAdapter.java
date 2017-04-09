package mkryglikov.data.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import mkryglikov.data.models.ActiveOrderItem;
import mkryglikov.shisha.R;

public class ActiveOrderItemsAdapter extends RecyclerView.Adapter<ActiveOrderItemsAdapter.ActiveOrderItemsViewHolder> {
    private List<ActiveOrderItem> items;

    public static class ActiveOrderItemsViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity, tvPrice;

        ActiveOrderItemsViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvQuantity = (TextView) itemView.findViewById(R.id.tvQuantity);
            tvPrice = (TextView) itemView.findViewById(R.id.tvPrice);
        }
    }


    public ActiveOrderItemsAdapter(List<ActiveOrderItem> items) {
        this.items = items;
    }

    @Override
    public ActiveOrderItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.active_order_item_view, parent, false);
        return new ActiveOrderItemsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ActiveOrderItemsViewHolder holder, int i) {
        holder.tvName.setText(items.get(i).getName());
        holder.tvQuantity.setText(String.valueOf("x" + items.get(i).getQuantity()));
        holder.tvPrice.setText(String.valueOf(items.get(i).getPrice()) + " \u20BD");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
