package mkryglikov.shisha;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import static mkryglikov.shisha.R.id.mainFragmentContainer;
import static mkryglikov.shisha.R.id.menuMain;
import static mkryglikov.shisha.R.id.menuProfile;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_FRAGMENT_ACTIVE_ORDERS = "ActiveOrdersFragment";
    private static final String TAG_FRAGMENT_PROFILE = "ProfileFragment";
    public static final int REQUEST_UPDATE_USER_INFO = 0;
    private static final int REQUEST_NEW_ORDER = 1;
    public static final int REQUEST_UPDATE_ORDER = 2;
    private FragmentManager fm;
    private ActiveOrdersFragment activeOrdersFragment;
    private ProfileFragment profileFragment;
    private ConnectivityManager cm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AccountManager am = AccountManager.get(this);
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
            return;

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length < 1) {
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, this, null, null);
            finish();
        } else {
            fm = getSupportFragmentManager();
            BottomNavigationView bottomNavigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);
            bottomNavigation.setSelectedItemId(R.id.menuMain);
            bottomNavigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    if (item.getItemId() == R.id.menuAdd) {
                        if (checkNetwork())
                            startActivityForResult(new Intent(MainActivity.this, NewOrderActivity.class), REQUEST_NEW_ORDER);
                        else {
                            Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), "Нет соединения с сетью", Snackbar.LENGTH_LONG);
                            View snackbarView = snackbar.getView();
                            snackbarView.setBackgroundColor(Color.WHITE);
                            snackbarView.setMinimumHeight(findViewById(R.id.bottomNavigation).getHeight());
                            snackbarView.findViewById(android.support.design.R.id.snackbar_text).setMinimumHeight(findViewById(R.id.bottomNavigation).getHeight());
                            snackbar.show();
                        }
                    } else {
                        FragmentTransaction ft = fm.beginTransaction();
                        switch (item.getItemId()) {
                            case menuMain:
                                if (fm.findFragmentByTag(TAG_FRAGMENT_ACTIVE_ORDERS) == null) {
                                    if (activeOrdersFragment == null)
                                        activeOrdersFragment = new ActiveOrdersFragment();
                                    ft.replace(mainFragmentContainer, activeOrdersFragment, TAG_FRAGMENT_ACTIVE_ORDERS);
                                    item.setChecked(true);
                                }
                                break;
                            case menuProfile:
                                if (fm.findFragmentByTag(TAG_FRAGMENT_PROFILE) == null) {
                                    if (profileFragment == null)
                                        profileFragment = new ProfileFragment();
                                    ft.replace(mainFragmentContainer, profileFragment, TAG_FRAGMENT_PROFILE);
                                    item.setChecked(true);
                                }
                                break;
                        }
                        ft.commit();
                    }
                    return false;
                }
            });

            if (savedInstanceState == null) {
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(mainFragmentContainer, new ActiveOrdersFragment(), TAG_FRAGMENT_ACTIVE_ORDERS);
                ft.commit();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_NEW_ORDER && resultCode == RESULT_OK) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_ACTIVE_ORDERS) != null)
                ((ActiveOrdersFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_ACTIVE_ORDERS)).onRefresh();
            else if (getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PROFILE) != null)
                ((ProfileFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PROFILE)).onRefresh();

            showSnackbar("Заказ добавлен");
        } else if (requestCode == REQUEST_UPDATE_ORDER && resultCode == RESULT_OK) {
            if (getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_ACTIVE_ORDERS) != null)
                ((ActiveOrdersFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_ACTIVE_ORDERS)).onRefresh();
            else if (getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PROFILE) != null)
                ((ProfileFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PROFILE)).onRefresh();

            showSnackbar("Заказ обновлен");
        } else if (requestCode == REQUEST_UPDATE_USER_INFO && resultCode == RESULT_OK) {
            ((ProfileFragment) getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_PROFILE)).onRefresh();

            showSnackbar("Данные успешно обновлены");
        }
    }

    private boolean checkNetwork() {
        return cm.getActiveNetworkInfo() != null;
    }

    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), text, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.WHITE);
        snackbarView.setMinimumHeight(findViewById(R.id.bottomNavigation).getHeight());
        snackbarView.findViewById(android.support.design.R.id.snackbar_text).setMinimumHeight(findViewById(R.id.bottomNavigation).getHeight());
        snackbar.show();
    }
}
