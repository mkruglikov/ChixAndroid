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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.adapters.ActiveOrdersAdapter;
import mkryglikov.data.models.Order;
import mkryglikov.data.models.TobaccosExtrasResponse;
import retrofit2.Response;

public class ActiveOrdersFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, ActiveOrdersAdapter.OnUpdateActiveOrdersListener {

    private Account account;
    private AccountManager am;
    private ConnectivityManager cm;
    private SwipeRefreshLayout srlActiveOrders;
    private ProgressBar pbActiveOrders;
    private String token;
    private RecyclerView rvActiveOrders;
    private RelativeLayout rlNetworkError, rlNoActiveOrders;
    private boolean isJustCreated = true;
    private NestedScrollView nswActiveOrders;
    private ActiveOrdersAdapter activeOrdersAdapter;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    private Api api = RetrofitClient.getClient();
    private getActiveOrdersTask task;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        am = AccountManager.get(getActivity());
        cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

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
        View rootView = inflater.inflate(R.layout.fragment_active_orders, container, false);
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_main);

        pbActiveOrders = (ProgressBar) rootView.findViewById(R.id.pbActiveOrders);
        nswActiveOrders = (NestedScrollView) rootView.findViewById(R.id.nswActiveOrders);
        rlNetworkError = (RelativeLayout) rootView.findViewById(R.id.rlNetworkError);
        rlNoActiveOrders = (RelativeLayout) rootView.findViewById(R.id.rlNoActiveOrders);
        srlActiveOrders = (SwipeRefreshLayout) rootView.findViewById(R.id.srlActiveOrders);
        srlActiveOrders.setOnRefreshListener(this);
        srlActiveOrders.setColorSchemeResources(R.color.colorAccent);

        rvActiveOrders = (RecyclerView) rootView.findViewById(R.id.rvActiveOrders);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        rvActiveOrders.setLayoutManager(llm);
        rvActiveOrders.setNestedScrollingEnabled(false);

        if (rvActiveOrders.getAdapter() == null && activeOrdersAdapter != null)
            rvActiveOrders.setAdapter(activeOrdersAdapter);

        return rootView;
    }

    @Override
    public void onRefresh() {
        getOrders();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (task != null && task.getStatus() == AsyncTask.Status.RUNNING)
            task.cancel(true);
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(networkStateReceiver);
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onUpdateActiveOrders() {
        getOrders();
    }

    private void getOrders() {
        if (cm.getActiveNetworkInfo() == null) {
            if (srlActiveOrders.isRefreshing())
                srlActiveOrders.setRefreshing(false);
            rlNetworkError.setVisibility(View.VISIBLE);
            rlNoActiveOrders.setVisibility(View.GONE);
            nswActiveOrders.setVisibility(View.GONE);
            pbActiveOrders.setVisibility(View.GONE);
            return;
        }
        task = new getActiveOrdersTask();
        task.execute();
    }

    private class getActiveOrdersTask extends AsyncTask<Void, Void, ActiveOrdersAdapter> {
        @Override
        protected void onPreExecute() {
            rlNetworkError.setVisibility(View.GONE);
            if (!srlActiveOrders.isRefreshing()) {
                pbActiveOrders.setVisibility(View.VISIBLE);
                nswActiveOrders.setVisibility(View.GONE);
            }
        }

        @Override
        protected ActiveOrdersAdapter doInBackground(Void... params) {
            List<TobaccosExtrasResponse> tobaccos = new ArrayList<>(), extras = new ArrayList<>();
            try {
                Response<List<TobaccosExtrasResponse>> tobaccosResponse = api.getTobaccos(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                Response<List<TobaccosExtrasResponse>> extrasResponse = api.getExtras(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                Response<List<Order>> activeOrdersResponse = api.getActiveOrders(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();

                if (tobaccosResponse.body() != null && tobaccosResponse.code() == 200)
                    tobaccos = tobaccosResponse.body();
                else
                    Log.w("FUCK", "Не получили список табаков на главную " + tobaccosResponse.code());

                if (extrasResponse.body() != null && extrasResponse.code() == 200)
                    extras = extrasResponse.body();
                else
                    Log.w("FUCK", "Не получили список экстра на главную " + extrasResponse.code());

                if (activeOrdersResponse.body() != null && activeOrdersResponse.code() == 200)
                    activeOrdersAdapter = new ActiveOrdersAdapter(activeOrdersResponse.body(), tobaccos, extras, token);
                else if (activeOrdersResponse.code() == 404)
                    return null;
                else {
                    Log.w("FUCK", "Не получили список активных заказов на главную" + activeOrdersResponse.code());
                    am.invalidateAuthToken(LoginActivity.ACCOUNT_TYPE, token);
                    am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, getActivity(), new OnTokenAcquired(), null);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return activeOrdersAdapter;
        }

        @Override
        protected void onPostExecute(@Nullable ActiveOrdersAdapter activeOrdersAdapter) {
            if (activeOrdersAdapter != null) {
                activeOrdersAdapter.setOnUpdateActiveOrdersListener(ActiveOrdersFragment.this);
                rvActiveOrders.setAdapter(activeOrdersAdapter);
                rvActiveOrders.setVisibility(View.VISIBLE);
                nswActiveOrders.setVisibility(View.VISIBLE);
                nswActiveOrders.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
            } else {
                if (!rlNoActiveOrders.isShown()) {
                    rlNoActiveOrders.setVisibility(View.VISIBLE);
                    rlNoActiveOrders.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in));
                }
                nswActiveOrders.setVisibility(View.GONE);
            }
            pbActiveOrders.setVisibility(View.GONE);
            srlActiveOrders.setRefreshing(false);
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
            getOrders();
        }
    }

    public class NetworkStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getExtras() != null) {
                if (isJustCreated) {
                    isJustCreated = false;
                    return;
                }
                getOrders();
            }
        }
    }
}
