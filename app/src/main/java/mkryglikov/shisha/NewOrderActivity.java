package mkryglikov.shisha;

import android.Manifest;
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
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.philliphsu.bottomsheetpickers.time.BottomSheetTimePickerDialog;
import com.philliphsu.bottomsheetpickers.time.numberpad.NumberPadTimePickerDialog;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.HorizontalCalendarListener;
import mkryglikov.data.adapters.NewOrderViewPagerAdapter;
import mkryglikov.data.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Response;


public class NewOrderActivity extends AppCompatActivity implements BottomSheetTimePickerDialog.OnTimeSetListener {
    private ViewPager viewpagerNewOrder;
    private TabLayout tabsNewOrder;
    private NumberPadTimePickerDialog dpTime;
    private Calendar calendar = Calendar.getInstance();
    private Account account;
    private AccountManager am;
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    private String today = df.format(Calendar.getInstance().getTime());
    private String selectedDate = df.format(Calendar.getInstance().getTime());
    private String selectedTime = "13:00";
    private TextView tvTime;
    private int selectedHour = 13, selectedMinutes = 0;
    private ConnectivityManager cm;
    private String token;
    private NewOrderViewPagerAdapter tabsAdapter;
    private Api api = RetrofitClient.getClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);

        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        am = AccountManager.get(this);

        viewpagerNewOrder = (ViewPager) findViewById(R.id.viewpagerNewOrder);
        tabsNewOrder = (TabLayout) findViewById(R.id.tabsNewOrder);

        tabsAdapter = new NewOrderViewPagerAdapter(getSupportFragmentManager());

        Fragment tobaccoFragment = new AddTobaccoFragment();
        tabsAdapter.addFragment(tobaccoFragment, getString(R.string.tobacco));

        Fragment extraFragment = new AddExtraFragment();
        tabsAdapter.addFragment(extraFragment, getString(R.string.extra));

        viewpagerNewOrder.setAdapter(tabsAdapter);
        tabsNewOrder.setupWithViewPager(viewpagerNewOrder);

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, this, new OnTokenAcquired(), null);

        } else
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, NewOrderActivity.this, new OnAccountCreated(), null);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_new_order);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvTime = (TextView) findViewById(R.id.tvTime);

        if (calendar.get(Calendar.HOUR_OF_DAY) > 13) {
            selectedTime = calendar.get(Calendar.HOUR_OF_DAY) + ":" + (calendar.get(Calendar.MINUTE) < 10 ? "0" + calendar.get(Calendar.MINUTE) : calendar.get(Calendar.MINUTE));
            selectedHour = calendar.get(Calendar.HOUR_OF_DAY);
            selectedMinutes = calendar.get(Calendar.MINUTE);
        }

        tvTime.setText(selectedTime);

        dpTime = NumberPadTimePickerDialog.newInstance(this);
        dpTime.setThemeDark(true);
        dpTime.setHint(selectedTime);

        tvTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dpTime.show(getSupportFragmentManager(), "timePicker");
            }
        });

        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 1);

        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, 0);

        HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(this, R.id.horizontalCalendar)
                .startDate(startDate.getTime())
                .endDate(endDate.getTime())
                .build();

        horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Date selectedDate, int position) {
                NewOrderActivity.this.selectedDate = df.format(selectedDate);
            }
        });
    }

    @Override
    public void onTimeSet(ViewGroup viewGroup, int hourOfDay, int minute) {
        if (hourOfDay < 13 && hourOfDay != 0) {
            Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), "Мы работаем с 13:00", Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            ((TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
            snackbar.show();
            return;
        }
        selectedHour = hourOfDay;
        selectedMinutes = minute;
        selectedTime = hourOfDay + ":" + (minute < 10 ? "0" + minute : minute);
        tvTime.setText(selectedTime);
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

    private class OnAccountCreated implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            if (ActivityCompat.checkSelfPermission(NewOrderActivity.this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            Log.d("FUCK", "Хотим получить токен после добавления аккаунта");
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, NewOrderActivity.this, new OnTokenAcquired(), null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.confirm, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_order:
                String tobaccos = AddTobaccoFragment.getTobaccosToSend();
                if (cm.getActiveNetworkInfo() == null)
                    showErrorSnackbar("Нет соединения с сетью");
                else if (tobaccos.isEmpty())
                    showErrorSnackbar("Выберите табак");
                else if (isPastTimeSelected())
                    showErrorSnackbar("Выбрано прошедшее время");
                else
                    sendOrder();
                return true;
            case android.R.id.home:
                setResult(RESULT_CANCELED, null);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showErrorSnackbar(String errorText) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_new_order), errorText, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        ((TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void sendOrder() {

        final String tobaccos = AddTobaccoFragment.getTobaccosToSend();
        final String extras = AddExtraFragment.getExtrasToSend().isEmpty() ? " " : AddExtraFragment.getExtrasToSend();

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Response<ResponseBody> response = api.newOrder(LoginActivity.AUTH_TOKEN_TYPE + " " + token, 0, tobaccos, extras, selectedDate, selectedTime).execute();
                    if (response.code() == 200) {
                        return true;
                    } else {
                        Log.d("FUCK", "Заказ не отправился " + response.code());
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean isSuccess) {
                if (isSuccess) {
                    setResult(RESULT_OK, null);
                    finish();
                }
            }
        }.execute();
    }

    private boolean isPastTimeSelected() {
        int hour = calendar.get(Calendar.HOUR_OF_DAY); //Intellij bug
        if (!selectedDate.equals(today))
            return false;
        if (selectedHour > hour)
            return false;
        if (selectedHour == hour && selectedMinutes >= calendar.get(Calendar.MINUTE))
            return false;
        return true;
    }
}
