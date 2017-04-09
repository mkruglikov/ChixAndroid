package mkryglikov.shisha;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.adapters.OrdersAdapter;
import mkryglikov.data.models.ClientInfo;
import mkryglikov.data.models.Order;
import retrofit2.Response;

public class ProfileFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private Account account;
    private AccountManager am;
    private ConnectivityManager cm;
    private TextView tvPhone, tvBirthday, tvNetworkError, tvOrdersHistory, tvOrders;
    private ImageView ivProfileAvatar;
    private String token, phone, birthday, id;
    private Integer ordersCount;
    private RecyclerView rvOrders;
    private OrdersAdapter ordersAdapter;
    private SwipeRefreshLayout srlProfile;
    private ProgressBar pbProfile;
    private NestedScrollView nswProfile;
    private List<Order> orders;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    private boolean isJustCreated = true;
    private Api api = RetrofitClient.getClient();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy"), dateParse = new SimpleDateFormat("yyyy-MM-dd");
    private getUserInfoTask task;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        am = AccountManager.get(getActivity());

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, getActivity(), new OnTokenAcquired(), null);
        } else {
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, getActivity(), null, null);
            getActivity().finish();
        }

        isJustCreated = true;
        getActivity().registerReceiver(networkStateReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        final AlertDialog.Builder adExit = new AlertDialog.Builder(getActivity())
                .setTitle("Выход")
                .setMessage("Вы действительно хотите выйти из аккаунта?")
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        am.removeAccount(account, null, null);
                        am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, getActivity(), null, null);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Отмена", null)
                .setCancelable(true);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rvOrders = (RecyclerView) rootView.findViewById(R.id.rvOrders);
        if (rvOrders.getLayoutManager() == null)
            rvOrders.setLayoutManager(llm);
        rvOrders.addItemDecoration(new DividerItemDecoration(rvOrders.getContext(), llm.getOrientation()));
        rvOrders.setNestedScrollingEnabled(false);

        srlProfile = (SwipeRefreshLayout) rootView.findViewById(R.id.srlProfile);
        srlProfile.setOnRefreshListener(this);
        srlProfile.setColorSchemeResources(R.color.colorAccent);

        ivProfileAvatar = (ImageView) rootView.findViewById(R.id.ivProfileAvatar);

        nswProfile = (NestedScrollView) rootView.findViewById(R.id.nswProfile);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(account.name);

        tvPhone = (TextView) rootView.findViewById(R.id.tvProfilePhone);
        tvBirthday = (TextView) rootView.findViewById(R.id.tvProfileBirthday);
        tvOrdersHistory = (TextView) rootView.findViewById(R.id.tvOrdersHistory);
        tvOrders = (TextView) rootView.findViewById(R.id.tvProfileOrders);
        rootView.findViewById(R.id.tvProfileExit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adExit.show();
            }
        });
        rootView.findViewById(R.id.tvProfileEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().startActivityForResult(new Intent(getActivity(), EditProfileActivity.class), MainActivity.REQUEST_UPDATE_USER_INFO);
                Picasso.with(getActivity()).invalidate("https://api.mkryglikov.ru/avatars/" + id + ".jpg");
            }
        });

        ItemClickSupport.addTo(rvOrders).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, View v) {
                int clickedOrderId = (int) v.findViewById(R.id.rlOrder).getTag();
                Intent oneOrderIntent;
                for (Order order : orders) {
                    if (order.getId() == clickedOrderId) {
                        oneOrderIntent = new Intent(getActivity(), OneOrderActivity.class);
                        oneOrderIntent.putExtra(Order.ORDER, order);
                        if (order.getCompleted() == 1)
                            startActivity(oneOrderIntent);
                        else
                            getActivity().startActivityForResult(oneOrderIntent, MainActivity.REQUEST_UPDATE_ORDER);
                        break;
                    }
                }
            }
        });

        pbProfile = (ProgressBar) rootView.findViewById(R.id.pbProfile);
        tvNetworkError = (TextView) rootView.findViewById(R.id.tvProfileNetworkError);
        return rootView;
    }

    @Override
    public void onRefresh() {
        getUserInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (task != null && task.getStatus() == AsyncTask.Status.RUNNING)
            task.cancel(true);
    }

    @Override
    public void onDestroyView() {
        getActivity().unregisterReceiver(networkStateReceiver);
        super.onDestroyView();
    }

    private void getUserInfo() {
        if (cm.getActiveNetworkInfo() == null) {
            srlProfile.setRefreshing(false);
            tvNetworkError.setVisibility(View.VISIBLE);
            nswProfile.setVisibility(View.GONE);
            pbProfile.setVisibility(View.GONE);
            return;
        }
        task = new getUserInfoTask();
        task.execute();
    }

    private class getUserInfoTask extends AsyncTask<Void, Void, OrdersAdapter> {
        @Override
        protected void onPreExecute() {
            if (!srlProfile.isRefreshing())
                pbProfile.setVisibility(View.VISIBLE);

            if (tvNetworkError.isShown())
                tvNetworkError.setVisibility(View.GONE);

            if (isJustCreated)
                nswProfile.setVisibility(View.GONE);
        }

        @Override
        protected OrdersAdapter doInBackground(Void... params) {
            try {
                Response<ClientInfo> clientInfoResponse = api.getClientInfo(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                Response<List<Order>> ordersResponse = api.getOrders(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();

                if (clientInfoResponse.body() != null && clientInfoResponse.code() == 200) {
                    id = String.valueOf(clientInfoResponse.body().getId());
                    phone = String.valueOf(clientInfoResponse.body().getPhone());
                    ordersCount = clientInfoResponse.body().getTotalOrders();
                    try {
                        birthday = dateFormat.format(dateParse.parse(clientInfoResponse.body().getBirthday()));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                } else
                    Log.w("FUCK", "Не получили информацию о пользователе в профиль" + clientInfoResponse.code());

                if (ordersResponse.body() != null && ordersResponse.code() == 200) {
                    ordersAdapter = new OrdersAdapter(ordersResponse.body());
                    orders = ordersResponse.body();
                } else if (ordersResponse.code() == 404) {
                    return null;
                } else {
                    Log.w("FUCK", "Не получили список заказов в профиль " + ordersResponse.code());
                    am.invalidateAuthToken(LoginActivity.ACCOUNT_TYPE, token);
                    am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, getActivity(), new OnTokenAcquired(), null);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return ordersAdapter;
        }

        @Override
        protected void onPostExecute(@Nullable OrdersAdapter ordersAdapter) {
            if (ordersAdapter != null) {
                rvOrders.setVisibility(View.VISIBLE);
                rvOrders.setAdapter(ordersAdapter);
                tvOrdersHistory.setText("История заказов");
            } else {
                tvOrdersHistory.setText("Вы до сих не совершили ни одного заказа\nНажмите + чтобы разместить новый заказ\n↓");
                tvOrdersHistory.setGravity(Gravity.CENTER);
                tvOrdersHistory.setTextSize(16);
                rvOrders.setVisibility(View.GONE);
            }
            tvPhone.setText("+7" + phone);
            tvBirthday.setText(birthday);

            Picasso.with(getActivity())
                    .load("https://api.mkryglikov.ru/avatars/" + id + ".jpg")
                    .into(ivProfileAvatar);

            //ToDo статусы
            String count = "";
            if (ordersAdapter != null && ordersCount > 0) {
                switch (ordersCount % 10) {
                    case 1:
                        count = " заказ";
                        break;
                    case 2:
                    case 3:
                    case 4:
                        count = " заказа";
                        break;
                    case 0:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                        count = " заказов";
                        break;
                }
                tvOrders.setText(String.valueOf(ordersCount) + count);
            }

            srlProfile.setRefreshing(false);
            pbProfile.setVisibility(View.GONE);
            if (!nswProfile.isShown()) {
                nswProfile.setVisibility(View.VISIBLE);
                nswProfile.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
            }
        }
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
            getUserInfo();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getExtras() != null) {
                if (isJustCreated) {
                    isJustCreated = false;
                    return;
                }
                getUserInfo();
            }
        }
    }
}
