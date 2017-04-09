package mkryglikov.shisha;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.models.AuthResponse;
import mkryglikov.data.models.ClientInfo;
import retrofit2.Response;

public class LoginActivity extends AccountAuthenticatorActivity implements View.OnClickListener, TextView.OnEditorActionListener {

    final static String ARG_IS_ADDING_NEW_ACCOUNT = "isAddingNewAccount";
    public static final String AUTH_TOKEN_TYPE = "Bearer";
    public static final String ACCOUNT_TYPE = "ru.mkryglikov.shisha";
    public static final String ARG_AUTH_TYPE = "authToken";
    public static final String KEY_REFRESH_TOKEN = "refreshToken";
    public static final String KEY_CLIENT_NAME = "clientName";
    private static final int REQUEST_REGISTER = 0;
    private Api api = RetrofitClient.getClient();
    private ConnectivityManager cm;
    private String authToken, refreshToken, clientName;
    private ProgressBar pbLogin;
    private Button btnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        EditText etPassword = (EditText) findViewById(R.id.etPassword);
        etPassword.setOnEditorActionListener(this);

        btnEnter = (Button) findViewById(R.id.btnEnter);
        btnEnter.setOnClickListener(this);

        Button btnRegister = (Button) findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(this);

        Typeface bebas = Typeface.createFromAsset(getAssets(), getString(R.string.bebas_regular_font));
        TextView tvLogo = (TextView) findViewById(R.id.tvLogo);
        tvLogo.setTypeface(bebas);

        pbLogin = (ProgressBar) findViewById(R.id.pbLogin);
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnEnter:
                login();
                break;
            case R.id.btnRegister:
                startActivityForResult(new Intent(LoginActivity.this, RegisterActivity.class), REQUEST_REGISTER);
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        login();
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_REGISTER && resultCode == RESULT_OK)
            this.finish();
    }

    private void showSnackbar(String text) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_login), text, Snackbar.LENGTH_LONG);
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text)).setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void login() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        final String username = ((TextView) findViewById(R.id.etUsername)).getText().toString();
        final String password = ((TextView) findViewById(R.id.etPassword)).getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
            showSnackbar("Введите логин/пароль");
        else if (cm.getActiveNetworkInfo() == null)
            showSnackbar("Нет соединения с сетью");
        else {
            new AsyncTask<Void, Void, Intent>() {

                @Override
                protected void onPreExecute() {
                    btnEnter.setVisibility(View.GONE);
                    pbLogin.setVisibility(View.VISIBLE);
                }

                @Override
                protected Intent doInBackground(Void... params) {
                    try {
                        Response<AuthResponse> authResponse = api.auth(Authenticator.AUTH_GRANT_TYPE, Authenticator.APP_ID, Authenticator.APP_SECRET, username, password).execute();
                        switch (authResponse.code()) {
                            case 200:
                                authToken = authResponse.body().getAccessToken();
                                refreshToken = authResponse.body().getRefreshToken();
                                Response<ClientInfo> userInfoResponse = api.getClientInfo(AUTH_TOKEN_TYPE + " " + authToken).execute();
                                switch (userInfoResponse.code()) {
                                    case 200:
                                        clientName = userInfoResponse.body().getName();
                                        break;
                                    case 500:
                                        showSnackbar("Ошибка сервера, повторите попытку позже");
                                        break;
                                    default:
                                        showSnackbar("Ошибка");
                                        break;
                                }
                                break;
                            case 500:
                                showSnackbar("Ошибка сервера, повторите попытку позже");
                                break;
                            case 401:
                            case 403:
                                showSnackbar("Неверный логин/пароль");
                                break;
                            default:
                                showSnackbar("Ошибка");
                                break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (authToken == null || refreshToken == null || clientName == null)
                        return null;

                    final Intent res = new Intent();
                    res.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
                    res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                    res.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);
                    res.putExtra(LoginActivity.KEY_REFRESH_TOKEN, refreshToken);
                    res.putExtra(LoginActivity.KEY_CLIENT_NAME, clientName);
                    return res;
                }

                @Override
                protected void onPostExecute(Intent intent) {
                    pbLogin.setVisibility(View.GONE);
                    btnEnter.setVisibility(View.VISIBLE);

                    if (intent != null)
                        finishLogin(intent);
                }
            }.execute();
        }
    }

    private void finishLogin(Intent intent) {
        String accountName = intent.getStringExtra(LoginActivity.KEY_CLIENT_NAME);

        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            String authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String refreshToken = intent.getStringExtra(LoginActivity.KEY_REFRESH_TOKEN);
            AccountManager am = AccountManager.get(getApplicationContext());
            if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
                for (Account accountToDelete : am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)) {
                    am.removeAccount(accountToDelete, null, null);
                }
            }
            am.addAccountExplicitly(account, null, null);
            am.setUserData(account, LoginActivity.KEY_REFRESH_TOKEN, refreshToken);
            am.setAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, authToken);
        }
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // Отключаем возврат в MainActivity
        moveTaskToBack(true);
    }
}
