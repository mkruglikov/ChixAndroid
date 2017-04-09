package mkryglikov.data.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import mkryglikov.data.models.ActiveOrderItem;
import mkryglikov.shisha.R;
import pl.polak.clicknumberpicker.ClickNumberPickerListener;
import pl.polak.clicknumberpicker.ClickNumberPickerView;
import pl.polak.clicknumberpicker.PickerClickType;

public class EditableOrderItemsAdapter extends RecyclerView.Adapter<EditableOrderItemsAdapter.EditableOrderItemsViewHolder> {
    private List<ActiveOrderItem> items;
    private Context context;
    private int total;

    public static class EditableOrderItemsViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice;
        ClickNumberPickerView quantityPicker;
        ImageView ivDelete;

        EditableOrderItemsViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvPrice = (TextView) itemView.findViewById(R.id.tvPrice);
            quantityPicker = (ClickNumberPickerView) itemView.findViewById(R.id.quantityPicker);
            ivDelete = (ImageView) itemView.findViewById(R.id.ivDelete);
        }
    }

    public EditableOrderItemsAdapter(List<ActiveOrderItem> items) {
        this.items = items;
    }

    @Override
    public EditableOrderItemsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.edit_order_item_view, parent, false);
        return new EditableOrderItemsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final EditableOrderItemsViewHolder holder, int i) {
        final int onePrice = items.get(i).getPrice() / items.get(i).getQuantity();
        holder.tvName.setText(items.get(i).getName());
        holder.tvPrice.setText(onePrice + " \u20BD");
        holder.quantityPicker.setPickerValue(items.get(i).getQuantity());
        holder.quantityPicker.setClickNumberPickerListener(new ClickNumberPickerListener() {
            @Override
            public void onValueChange(float previousValue, float currentValue, PickerClickType pickerClickType) {
                items.get(holder.getAdapterPosition()).setQuantity((int) currentValue);
                items.get(holder.getAdapterPosition()).setPrice(onePrice * (int) currentValue);
                total = 0;
                for (ActiveOrderItem item : items) {
                    total += item.getPrice();
                }
                listener.onUpdateTotal(total, false);
            }
        });

        holder.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                items.remove(holder.getAdapterPosition());
                total = 0;
                for (ActiveOrderItem item : items) {
                    total += item.getPrice();
                }
                listener.onUpdateTotal(total, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private OnUpdateTotalListener listener;

    public interface OnUpdateTotalListener {
        void onUpdateTotal(int total, boolean recreateAdapter);
    }

    public void setOnUpdateTotalListener(OnUpdateTotalListener listener) {
        this.listener = listener;
    }
}
