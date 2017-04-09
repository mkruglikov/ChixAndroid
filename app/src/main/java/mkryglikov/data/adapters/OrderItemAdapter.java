package mkryglikov.data.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import mkryglikov.data.models.OrderItem;
import mkryglikov.shisha.R;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;

    public static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvPrice;
        private ImageView ivOrderItemIcon;

        OrderItemViewHolder(View itemView) {
            super(itemView);
            ivOrderItemIcon = (ImageView) itemView.findViewById(R.id.ivOrderItemIcon);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvPrice = (TextView) itemView.findViewById(R.id.tvPrice);
        }
    }

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public OrderItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item_view, parent, false);
        return new OrderItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(OrderItemViewHolder holder, int i) {
        if (orderItems.get(i).getCategory().equals(OrderItem.TOBACCO))
            holder.ivOrderItemIcon.setImageResource(R.drawable.ic_tobacco);
        else if (orderItems.get(i).getCategory().equals(OrderItem.EXTRA))
            holder.ivOrderItemIcon.setImageResource(R.drawable.ic_extra);
        holder.tvName.setText(orderItems.get(i).getName());
        holder.tvPrice.setText(String.valueOf(orderItems.get(i).getPrice()) + " \u20BD");
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }
}
