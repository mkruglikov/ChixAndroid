package mkryglikov.shisha;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.adapters.OrderItemAdapter;
import mkryglikov.data.models.Order;
import mkryglikov.data.models.OrderItem;
import mkryglikov.data.models.TobaccosExtrasResponse;
import retrofit2.Response;

public class NotEditableOrderFragment extends Fragment {

    private RecyclerView rvOneOrderItems;
    private ProgressBar pb;
    private Account account;
    private AccountManager am;
    private ConnectivityManager cm;
    private Order order;
    private String token;
    private List<TobaccosExtrasResponse> tobaccos, extras;
    private Api api = RetrofitClient.getClient();
    private SimpleDateFormat dateFormatWithYear = new SimpleDateFormat("dd MMMM yyyy");
    private SimpleDateFormat dateFormatWithoutYear = new SimpleDateFormat("dd MMMM");
    private SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        order = (Order) getArguments().getSerializable(OneOrderActivity.ORDER);

        cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        am = AccountManager.get(getActivity());

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
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
        View rootView = inflater.inflate(R.layout.fragment_not_editable_order, container, false);
        rvOneOrderItems = (RecyclerView) rootView.findViewById(R.id.rvOneOrderItems);
        pb = (ProgressBar) rootView.findViewById(R.id.pb);

        try {
            Date date = dateParse.parse(order.getDate());

            if (date.getYear() == new Date().getYear())
                ((TextView) rootView.findViewById(R.id.tvDate)).setText(dateFormatWithoutYear.format(date));
            else
                ((TextView) rootView.findViewById(R.id.tvDate)).setText(dateFormatWithYear.format(date));

        } catch (ParseException e) {
            e.printStackTrace();
        }
        String inTime = order.getInTime(), outTime = order.getOutTime();
        ((TextView) rootView.findViewById(R.id.tvInTime)).setText(inTime.substring(0, inTime.length()-3));
        ((TextView) rootView.findViewById(R.id.tvOutTime)).setText(outTime.substring(0, outTime.length()-3));
        ((TextView) rootView.findViewById(R.id.tvTotal)).setText(String.valueOf(order.getTotal()) + " \u20BD");

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());

        rvOneOrderItems.setLayoutManager(llm);
        rvOneOrderItems.addItemDecoration(new DividerItemDecoration(getActivity(), llm.getOrientation()));
        return rootView;
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
                rvOneOrderItems.setVisibility(View.GONE);
                pb.setVisibility(View.VISIBLE);
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Response<List<TobaccosExtrasResponse>> tobaccosResponse = api.getTobaccos(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                    if (tobaccosResponse.body() != null && tobaccosResponse.code() == 200)
                        tobaccos = tobaccosResponse.body();
                    else
                        Log.d("FUCK", "Не получили список табаков в заказ " + tobaccosResponse.code());

                    if (!order.getExtra().isEmpty()) {
                        Response<List<TobaccosExtrasResponse>> extrasResponse = api.getExtras(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                        if (extrasResponse.body() != null && extrasResponse.code() == 200)
                            extras = extrasResponse.body();
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
                setItems();
                pb.setVisibility(View.GONE);
                rvOneOrderItems.setVisibility(View.VISIBLE);
                rvOneOrderItems.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    private void setItems() {
        List<OrderItem> items = new ArrayList<OrderItem>();

        for (String tobaccoName : order.getTobacco().split(",")) {//FIXME костыль с циклами
            int price = 0;
            for (TobaccosExtrasResponse tobacco : tobaccos) {
                if (tobacco.getName().equals(tobaccoName))
                    price = tobacco.getPrice();
            }
            items.add(new OrderItem(tobaccoName, price, OrderItem.TOBACCO));
        }

        if (!order.getExtra().isEmpty()) {
            int price = 0;

            for (String extraName : order.getExtra().split(",")) {
                for (TobaccosExtrasResponse extra : extras) {
                    if (extra.getName().equals(extraName))
                        price = extra.getPrice();
                }
                items.add(new OrderItem(extraName, price, OrderItem.EXTRA));
            }
        }
        rvOneOrderItems.setAdapter(new OrderItemAdapter(items));
    }
}
