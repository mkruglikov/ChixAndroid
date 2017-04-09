package mkryglikov.data.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import mkryglikov.data.models.Order;
import mkryglikov.shisha.R;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {
    private List<Order> orders;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM");
    private SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");
    private Context context;

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rlOrder;
        TextView tvDate, tvTobaccoCount, tvTotal, tvIsDone;

        OrderViewHolder(View itemView) {
            super(itemView);
            rlOrder = (RelativeLayout) itemView.findViewById(R.id.rlOrder);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
            tvTobaccoCount = (TextView) itemView.findViewById(R.id.tvTobaccoCount);
            tvTotal = (TextView) itemView.findViewById(R.id.tvTotal);
            tvIsDone = (TextView) itemView.findViewById(R.id.tvIsDone);
        }
    }

    public OrdersAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @Override
    public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_view, parent, false);
        context = parent.getContext();
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(OrderViewHolder holder, int i) {
        holder.rlOrder.setTag(orders.get(i).getId());
        try {
            Date date = dateParse.parse(orders.get(i).getDate());
            holder.tvDate.setText(dateFormat.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String count = "";
        int tobaccosCount = orders.get(i).getTobacco().split(",").length;
        if (tobaccosCount > 0) {
            switch (tobaccosCount % 10) {
                case 1:
                    count = " кальян";
                    break;
                case 2:
                case 3:
                case 4:
                    count = " кальяна";
                    break;
                case 0:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    count = " кальянов";
                    break;
            }
        }
        holder.tvTobaccoCount.setText(tobaccosCount + count);
        holder.tvTotal.setText(String.valueOf(orders.get(i).getTotal()) + " \u20BD");

        if (orders.get(i).getCompleted() == 0) {
            holder.tvIsDone.setText("В ожидании");
            holder.tvIsDone.setTextColor(context.getResources().getColor(R.color.colorOrange));
        } else if (orders.get(i).getCompleted() == 1) {
            holder.tvIsDone.setText("Выполнен");
            holder.tvIsDone.setTextColor(context.getResources().getColor(R.color.colorGreen));
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }
}
