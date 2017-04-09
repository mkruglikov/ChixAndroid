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
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.io.IOException;

import mkryglikov.data.models.Order;
import mkryglikov.data.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class OneOrderActivity extends AppCompatActivity {

    public static final String ORDER = "order";
    private boolean isOrderCompleted;
    private ConnectivityManager cm;
    private String token;
    private Api api = RetrofitClient.getClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_order);
        Order order = ((Order) getIntent().getSerializableExtra(Order.ORDER));
        isOrderCompleted = order.getCompleted() == 1;

        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        AccountManager am = AccountManager.get(this);

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            Account account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, this, new OnTokenAcquired(), null);
        } else {
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, this, null, null);
            finish();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        Bundle orderBundle = new Bundle();
        orderBundle.putSerializable(ORDER, order);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Заказ #" + String.valueOf(order.getId()));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (isOrderCompleted) {
            NotEditableOrderFragment notEditableOrderFragment = new NotEditableOrderFragment();
            notEditableOrderFragment.setArguments(orderBundle);
            fragmentManager.beginTransaction()
                    .add(R.id.orderContainer, notEditableOrderFragment)
                    .commit();
        } else {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
            EditableOrderFragment editableOrderFragment = new EditableOrderFragment();
            editableOrderFragment.setArguments(orderBundle);

            fragmentManager.beginTransaction()
                    .add(R.id.orderContainer, editableOrderFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isOrderCompleted) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.confirm, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_add_order:
                if (checkNetwork())
                    sendOrder();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendOrder() {
        Order updatedOrder = EditableOrderFragment.getUpdatedOrder();
        if (updatedOrder.getTobacco().isEmpty()) {
            Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), "Добавьте кальян", Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
            snackbar.show();
            return;
        }

        int id = updatedOrder.getId();
        String tobacco = updatedOrder.getTobacco();
        String extra = updatedOrder.getExtra();
        String date = updatedOrder.getDate();
        String time = updatedOrder.getInTime();

        api.updateOrder(LoginActivity.AUTH_TOKEN_TYPE + " " + token, id, 0, tobacco, extra, date, time).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Log.d("FUCK", "Не отправился обновленный заказ" + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("FUCK", "Не отправился обновленный заказ совсем");
            }
        });
    }

    private boolean checkNetwork() {
        return cm.getActiveNetworkInfo() != null;
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
        }
    }
}
