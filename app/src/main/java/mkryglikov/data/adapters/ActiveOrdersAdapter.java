package mkryglikov.data.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import mkryglikov.data.models.ActiveOrderItem;
import mkryglikov.data.models.Order;
import mkryglikov.data.models.TobaccosExtrasResponse;
import mkryglikov.data.RetrofitClient;
import mkryglikov.shisha.LoginActivity;
import mkryglikov.shisha.MainActivity;
import mkryglikov.shisha.OneOrderActivity;
import mkryglikov.shisha.R;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActiveOrdersAdapter extends RecyclerView.Adapter<ActiveOrdersAdapter.ActiveOrderViewHolder> {
    private List<Order> orders;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM");
    private SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat timeParse = new SimpleDateFormat("HH:mm:ss");
    private List<TobaccosExtrasResponse> tobaccos, extras;
    private String token;

    static class ActiveOrderViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView rvActiveOrderTobaccos, rvActiveOrderExtras;
        private TextView tvDate, tvTotal, tvTime;
        private LinearLayout llActiveOrderExtras;
        private Button btnDelete, btnEdit;
        private ProgressBar pbProgress;

        ActiveOrderViewHolder(View itemView) {
            super(itemView);
            rvActiveOrderTobaccos = (RecyclerView) itemView.findViewById(R.id.rvEditOrderTobaccos);
            rvActiveOrderExtras = (RecyclerView) itemView.findViewById(R.id.rvEditOrderExtras);
            llActiveOrderExtras = (LinearLayout) itemView.findViewById(R.id.llEditOrderExtras);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
            tvTime = (TextView) itemView.findViewById(R.id.tvTime);
            tvTotal = (TextView) itemView.findViewById(R.id.tvTotal);
            btnDelete = (Button) itemView.findViewById(R.id.btnDelete);
            btnEdit = (Button) itemView.findViewById(R.id.btnEdit);
            pbProgress = (ProgressBar) itemView.findViewById(R.id.pbProgress);
        }
    }


    public ActiveOrdersAdapter(List<Order> orders, List<TobaccosExtrasResponse> tobaccos, List<TobaccosExtrasResponse> extras, String token) {
        this.orders = orders;
        Collections.reverse(this.orders);
        this.tobaccos = tobaccos;
        this.extras = extras;
        this.token = token;
    }

    @Override
    public ActiveOrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.active_order_view, parent, false);
        context = parent.getContext();
        return new ActiveOrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ActiveOrderViewHolder holder, final int i) {
        List<ActiveOrderItem> tobaccoItems = new LinkedList<ActiveOrderItem>();
        for (String orderTobaccoName : orders.get(i).getTobacco().split(",")) {//FixMe hell1
            for (TobaccosExtrasResponse tobacco : tobaccos) {
                if (tobacco.getName().equals(orderTobaccoName)) {
                    boolean isNew = true;
                    for (ActiveOrderItem tobaccoItem : tobaccoItems) {
                        if (tobaccoItem.getName().equals(orderTobaccoName)) {
                            tobaccoItem.setQuantity(tobaccoItem.getQuantity() + 1);
                            tobaccoItem.setPrice(tobaccoItem.getPrice() + tobacco.getPrice());
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew)
                        tobaccoItems.add(new ActiveOrderItem(orderTobaccoName, 1, tobacco.getPrice()));
                }
            }
        }
        holder.rvActiveOrderTobaccos.setLayoutManager(new LinearLayoutManager(context));
        holder.rvActiveOrderTobaccos.setAdapter(new ActiveOrderItemsAdapter(tobaccoItems));

        if (!orders.get(i).getExtra().isEmpty() && !orders.get(i).getExtra().equals(" ")) {
            List<ActiveOrderItem> extraItems = new LinkedList<ActiveOrderItem>();
            for (String orderExtraName : orders.get(i).getExtra().split(",")) {//FixMe hell2
                for (TobaccosExtrasResponse extra : extras) {
                    if (extra.getName().equals(orderExtraName)) {
                        boolean isNew = true;
                        for (ActiveOrderItem extraItem : extraItems) {
                            if (extraItem.getName().equals(orderExtraName)) {
                                extraItem.setQuantity(extraItem.getQuantity() + 1);
                                extraItem.setPrice(extraItem.getPrice() + extra.getPrice());
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew)
                            extraItems.add(new ActiveOrderItem(orderExtraName, 1, extra.getPrice()));
                    }
                }
            }
            holder.rvActiveOrderExtras.setLayoutManager(new LinearLayoutManager(context));
            holder.rvActiveOrderExtras.setAdapter(new ActiveOrderItemsAdapter(extraItems));
        } else {
            holder.llActiveOrderExtras.setVisibility(View.GONE);
            holder.rvActiveOrderExtras.setVisibility(View.GONE);
        }

        try {
            holder.tvDate.setText(dateFormat.format(dateParse.parse(orders.get(i).getDate())));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            holder.tvTime.setText(timeFormat.format(timeParse.parse(orders.get(i).getInTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        holder.tvTotal.setText(String.valueOf(orders.get(i).getTotal()) + " \u20BD");

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent oneOrderIntent = new Intent(context, OneOrderActivity.class);
                oneOrderIntent.putExtra(Order.ORDER, orders.get(i));
                ((MainActivity) context).startActivityForResult(oneOrderIntent, MainActivity.REQUEST_UPDATE_ORDER);
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle("Подтверждение")
                        .setMessage("Вы действительно хотите отменить заказ?")
                        .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                holder.btnEdit.setVisibility(View.GONE);
                                holder.btnDelete.setVisibility(View.GONE);
                                holder.pbProgress.setVisibility(View.VISIBLE);
                                RetrofitClient.getClient().deleteOrder(LoginActivity.AUTH_TOKEN_TYPE + " " + token, orders.get(i).getId()).enqueue(new Callback<ResponseBody>() {
                                    @Override
                                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                        if (response.code() == 200) {
                                            listener.onUpdateActiveOrders();
                                        } else {
                                            Log.d("FUCK", "Не получили список активных заказов на главную" + response.code());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                                        t.printStackTrace();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Нет", null)
                        .setCancelable(true)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }


    private OnUpdateActiveOrdersListener listener;

    public interface OnUpdateActiveOrdersListener {
        void onUpdateActiveOrders();
    }

    public void setOnUpdateActiveOrdersListener(OnUpdateActiveOrdersListener listener) {
        this.listener = listener;
    }

}


