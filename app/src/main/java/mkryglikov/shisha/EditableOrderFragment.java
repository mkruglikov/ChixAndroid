package mkryglikov.shisha;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.philliphsu.bottomsheetpickers.date.DatePickerDialog;
import com.philliphsu.bottomsheetpickers.time.BottomSheetTimePickerDialog;
import com.philliphsu.bottomsheetpickers.time.numberpad.NumberPadTimePickerDialog;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.adapters.AddItemAdapter;
import mkryglikov.data.adapters.EditableOrderItemsAdapter;
import mkryglikov.data.models.ActiveOrderItem;
import mkryglikov.data.models.Order;
import mkryglikov.data.models.TobaccosExtrasResponse;
import retrofit2.Response;

public class EditableOrderFragment extends Fragment implements com.philliphsu.bottomsheetpickers.date.DatePickerDialog.OnDateSetListener, BottomSheetTimePickerDialog.OnTimeSetListener {

    private RecyclerView rvEditOrderTobaccos, rvEditOrderExtras;
    private LinearLayout llEditOrderExtras, llEditOrderTobaccos, llContent;
    private ProgressBar pb;
    private TextView tvTime, tvDate, tvTotal;
    private int tobaccosTotal = 0, extrasTotal = 0;
    private Calendar orderCalendar = Calendar.getInstance();
    private ConnectivityManager cm;
    private Order order;
    private String token;
    private List<TobaccosExtrasResponse> allTobaccos, allExtras;
    private Api api = RetrofitClient.getClient();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM");
    private SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat timeParse = new SimpleDateFormat("HH:mm:ss");
    private StringBuilder timeSb = new StringBuilder(), totalSb = new StringBuilder();

    private static int orderId;
    private static String orderDate, orderTime;
    private static List<ActiveOrderItem> orderTobaccos = new LinkedList<ActiveOrderItem>(), orderExtras = new LinkedList<ActiveOrderItem>();

    @Override
    public void onDetach() {
        orderTobaccos.clear();
        orderExtras.clear();
        super.onDetach();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        order = (Order) getArguments().getSerializable(OneOrderActivity.ORDER);
        orderId = order.getId();
        orderDate = order.getDate();
        orderTime = order.getInTime();

        try {
            order.setInTime(timeFormat.format(timeParse.parse(order.getInTime())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        AccountManager am = AccountManager.get(getActivity());

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            Account account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, getActivity(), new OnTokenAcquired(), null);
        } else {
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, getActivity(), null, null);
            getActivity().finish();
        }

        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_editable_order, container, false);
        llContent = (LinearLayout) rootView.findViewById(R.id.llContent);
        pb = (ProgressBar) rootView.findViewById(R.id.pb);
        llContent.setVisibility(View.GONE);
        pb.setVisibility(View.VISIBLE);

        llEditOrderExtras = (LinearLayout) rootView.findViewById(R.id.llEditOrderExtras);
        llEditOrderTobaccos = (LinearLayout) rootView.findViewById(R.id.llEditOrderTobaccos);
        Button btnAddExtra = (Button) rootView.findViewById(R.id.btnAddExtra);
        Button btnAddTobacco = (Button) rootView.findViewById(R.id.btnAddTobacco);

        tvTime = (TextView) rootView.findViewById(R.id.tvTime);
        tvTime.setText(order.getInTime());
        tvDate = (TextView) rootView.findViewById(R.id.tvDate);
        try {
            tvDate.setText(dateFormat.format(dateParse.parse(order.getDate())));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        tvTotal = (TextView) rootView.findViewById(R.id.tvTotal);
        tvTotal.setText(String.valueOf(order.getTotal()) + " \u20BD");

        rvEditOrderTobaccos = (RecyclerView) rootView.findViewById(R.id.rvEditOrderTobaccos);
        rvEditOrderExtras = (RecyclerView) rootView.findViewById(R.id.rvEditOrderExtras);

        rvEditOrderTobaccos.setNestedScrollingEnabled(false);
        rvEditOrderExtras.setNestedScrollingEnabled(false);

        rvEditOrderTobaccos.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvEditOrderExtras.setLayoutManager(new LinearLayoutManager(getActivity()));

        try {
            orderCalendar.setTime(dateParse.parse(order.getDate()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        final DatePickerDialog dp = new DatePickerDialog.Builder(
                EditableOrderFragment.this,
                orderCalendar.get(Calendar.YEAR),
                orderCalendar.get(Calendar.MONTH),
                orderCalendar.get(Calendar.DAY_OF_MONTH))
                .build();

        rootView.findViewById(R.id.rlOrderEditDate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dp.show(getFragmentManager(), "date");
            }
        });

        final NumberPadTimePickerDialog tp = new NumberPadTimePickerDialog.Builder(EditableOrderFragment.this, true)
                .build();
        rootView.findViewById(R.id.rlOrderEditTime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tp.setHint(order.getInTime());
                tp.show(getFragmentManager(), "time");
            }
        });


        btnAddTobacco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddTobaccoDialog(getActivity()).show();
            }
        });

        btnAddExtra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddExtraDialog(getActivity()).show();
            }
        });

        return rootView;
    }

    private class AddTobaccoDialog extends AlertDialog {

        AddTobaccoDialog(Context context) {
            super(context);

            View adTobaccoView = getActivity().getLayoutInflater().inflate(R.layout.edit_order_add_item, null);
            RecyclerView rvItems = (RecyclerView) adTobaccoView.findViewById(R.id.rvItems);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            rvItems.setLayoutManager(llm);
            rvItems.addItemDecoration(new DividerItemDecoration(rvItems.getContext(), llm.getOrientation()));

            final List<TobaccosExtrasResponse> dialogTobaccos = new LinkedList<>();

            for (TobaccosExtrasResponse allTobacco : allTobaccos) {
                dialogTobaccos.add(allTobacco);
            }

            for (ActiveOrderItem orderTobacco : orderTobaccos) {
                ListIterator<TobaccosExtrasResponse> it = dialogTobaccos.listIterator();
                while (it.hasNext()) {
                    TobaccosExtrasResponse dialogTobacco = it.next();
                    if (dialogTobacco.getName().equals(orderTobacco.getName())) {
                        it.remove();
                    }
                }
            }

            rvItems.setAdapter(new AddItemAdapter(dialogTobaccos));
            setTitle("Добавить кальян");
            setView(adTobaccoView);
            setCancelable(true);

            ItemClickSupport.addTo(rvItems).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                @Override
                public void onItemClicked(RecyclerView recyclerView, View v) {
                    int tobaccoId = (int) v.findViewById(R.id.rlAddItem).getTag();
                    for (TobaccosExtrasResponse allTobacco : dialogTobaccos) {
                        if (allTobacco.getId() == tobaccoId) {
                            orderTobaccos.add(new ActiveOrderItem(allTobacco.getName(), 1, allTobacco.getPrice()));
                            tobaccosTotal += allTobacco.getPrice();
                            setTobaccoAdapter();
                            updateTotalView();
                            dismiss();
                            break;
                        }
                    }
                }
            });
        }
    }

    private class AddExtraDialog extends AlertDialog {

        AddExtraDialog(Context context) {
            super(context);

            View adExtraView = getActivity().getLayoutInflater().inflate(R.layout.edit_order_add_item, null);
            RecyclerView rvItems = (RecyclerView) adExtraView.findViewById(R.id.rvItems);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            rvItems.setLayoutManager(llm);
            rvItems.addItemDecoration(new DividerItemDecoration(rvItems.getContext(), llm.getOrientation()));


            List<TobaccosExtrasResponse> dialogExtras = new LinkedList<>();

            for (TobaccosExtrasResponse allExtra : allExtras) {
                dialogExtras.add(allExtra);
            }


            for (ActiveOrderItem orderExtra : orderExtras) {
                ListIterator<TobaccosExtrasResponse> it = dialogExtras.listIterator();
                while (it.hasNext()) {
                    TobaccosExtrasResponse dialogExtra = it.next();
                    if (dialogExtra.getName().equals(orderExtra.getName())) {
                        it.remove();
                    }
                }
            }

            rvItems.setAdapter(new AddItemAdapter(dialogExtras));
            setTitle("Добавить экстра");
            setView(adExtraView);
            setCancelable(true);

            ItemClickSupport.addTo(rvItems).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                @Override
                public void onItemClicked(RecyclerView recyclerView, View v) {
                    int extraId = (int) v.findViewById(R.id.rlAddItem).getTag();
                    for (TobaccosExtrasResponse allExtra : allExtras) {
                        if (allExtra.getId() == extraId) {
                            orderExtras.add(new ActiveOrderItem(allExtra.getName(), 1, allExtra.getPrice()));
                            extrasTotal += allExtra.getPrice();
                            setExtraAdapter();
                            updateTotalView();
                            dismiss();
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        orderDate = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
        try {
            tvDate.setText(dateFormat.format(dateParse.parse(year + "-" + (monthOfYear + 1) + "-" + dayOfMonth)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onTimeSet(ViewGroup viewGroup, int hourOfDay, int minute) {
        if (hourOfDay < 13 && hourOfDay != 0) {
            Snackbar snackbar = Snackbar.make(getView(), "Мы работаем с 13:00", Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
            snackbar.show();
            return;
        }
        timeSb.setLength(0);
        timeSb.append(hourOfDay)
                .append(":")
                .append(minute == 0 ? "00" : minute);
        tvTime.setText(timeSb.toString());
        order.setInTime(hourOfDay + ":" + (minute == 0 ? "00" : minute));
        orderTime = hourOfDay + ":" + (minute == 0 ? "00" : minute);
    }


    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> results) {
            try {
                token = results.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
            if (checkNetwork())
                getPrices();
        }
    }

    private boolean checkNetwork() {
        return cm.getActiveNetworkInfo() != null;
    }

    private void getPrices() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Response<List<TobaccosExtrasResponse>> tobaccosResponse = api.getTobaccos(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                    if (tobaccosResponse.body() != null && tobaccosResponse.code() == 200)
                        allTobaccos = tobaccosResponse.body();
                    else
                        Log.d("FUCK", "Не получили список табаков в заказ " + tobaccosResponse.code());

                    if (!order.getExtra().isEmpty()) {
                        Response<List<TobaccosExtrasResponse>> extrasResponse = api.getExtras(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                        if (extrasResponse.body() != null && extrasResponse.code() == 200)
                            allExtras = extrasResponse.body();
                        else
                            Log.d("FUCK", "Не получили список экстра в заказ " + extrasResponse.code());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                parseItems();
                pb.setVisibility(View.GONE);
                llContent.setVisibility(View.VISIBLE);
                llContent.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    private void parseItems() {
        for (String orderTobaccoName : order.getTobacco().split(",")) {//FixMe hell1
            for (TobaccosExtrasResponse tobacco : allTobaccos) {
                if (tobacco.getName().equals(orderTobaccoName)) {
                    tobaccosTotal += tobacco.getPrice();
                    boolean isNew = true;
                    for (ActiveOrderItem tobaccoItem : orderTobaccos) {
                        if (tobaccoItem.getName().equals(orderTobaccoName)) {
                            tobaccoItem.setQuantity(tobaccoItem.getQuantity() + 1);
                            tobaccoItem.setPrice(tobaccoItem.getPrice() + tobacco.getPrice());
                            isNew = false;
                            break;
                        }
                    }
                    if (isNew)
                        orderTobaccos.add(new ActiveOrderItem(orderTobaccoName, 1, tobacco.getPrice()));
                }
            }
        }
        setTobaccoAdapter();


        if (!order.getExtra().isEmpty()) {
            for (String orderExtraName : order.getExtra().split(",")) {//FixMe hell2
                for (TobaccosExtrasResponse extra : allExtras) {
                    if (extra.getName().equals(orderExtraName)) {
                        extrasTotal += extra.getPrice();
                        boolean isNew = true;
                        for (ActiveOrderItem extraItem : orderExtras) {
                            if (extraItem.getName().equals(orderExtraName)) {
                                extraItem.setQuantity(extraItem.getQuantity() + 1);
                                extraItem.setPrice(extraItem.getPrice() + extra.getPrice());
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew)
                            orderExtras.add(new ActiveOrderItem(orderExtraName, 1, extra.getPrice()));
                    }
                }
            }

            setExtraAdapter();
        } else {
            rvEditOrderExtras.setVisibility(View.GONE);
            llEditOrderExtras.setVisibility(View.GONE);
        }
    }

    private void setTobaccoAdapter() {
        if (!orderTobaccos.isEmpty()) {
            rvEditOrderTobaccos.setVisibility(View.VISIBLE);
            llEditOrderTobaccos.setVisibility(View.VISIBLE);
            EditableOrderItemsAdapter tobaccosAdapter = new EditableOrderItemsAdapter(orderTobaccos);
            tobaccosAdapter.setOnUpdateTotalListener(new EditableOrderItemsAdapter.OnUpdateTotalListener() {
                @Override
                public void onUpdateTotal(int total, boolean recreateAdapter) {
                    tobaccosTotal = total;
                    updateTotalView();
                    if (recreateAdapter)
                        setTobaccoAdapter();
                }
            });
            rvEditOrderTobaccos.setAdapter(tobaccosAdapter);
        } else {
            rvEditOrderTobaccos.setVisibility(View.GONE);
            llEditOrderTobaccos.setVisibility(View.GONE);
        }
    }

    private void setExtraAdapter() {
        if (!orderExtras.isEmpty()) {
            rvEditOrderExtras.setVisibility(View.VISIBLE);
            llEditOrderExtras.setVisibility(View.VISIBLE);
            EditableOrderItemsAdapter extrasAdapter = new EditableOrderItemsAdapter(orderExtras);
            extrasAdapter.setOnUpdateTotalListener(new EditableOrderItemsAdapter.OnUpdateTotalListener() {
                @Override
                public void onUpdateTotal(int total, boolean recreateAdapter) {
                    extrasTotal = total;
                    updateTotalView();
                    if (recreateAdapter)
                        setExtraAdapter();
                }
            });
            rvEditOrderExtras.setAdapter(extrasAdapter);
        } else {
            rvEditOrderExtras.setVisibility(View.GONE);
            llEditOrderExtras.setVisibility(View.GONE);
        }
    }

    private void updateTotalView() {
        totalSb.setLength(0);
        totalSb.append(tobaccosTotal + extrasTotal);
        totalSb.append(" \u20BD");
        tvTotal.setText(totalSb.toString());
    }

    public static Order getUpdatedOrder() {
        StringBuilder sbTobaccos = new StringBuilder();
        String prefix = "";
        for (ActiveOrderItem tobacco : orderTobaccos) {
            for (int i = 0; i < tobacco.getQuantity(); i++) {
                sbTobaccos.append(prefix);
                prefix = ",";
                sbTobaccos.append(tobacco.getName());
            }
        }

        StringBuilder sbExtras = new StringBuilder();
        if (!orderExtras.isEmpty()) {
            prefix = "";
            for (ActiveOrderItem extra : orderExtras) {
                for (int i = 0; i < extra.getQuantity(); i++) {
                    sbExtras.append(prefix);
                    prefix = ",";
                    sbExtras.append(extra.getName());
                }
            }
        } else {
            sbExtras.append(" ");
        }

        return new Order(orderId, sbTobaccos.toString(), sbExtras.toString(), orderDate, orderTime);
    }

}
