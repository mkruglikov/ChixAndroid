package mkryglikov.shisha;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import mkryglikov.data.models.AuthResponse;
import mkryglikov.data.models.ClientInfo;
import mkryglikov.data.RetrofitClient;
import retrofit2.Response;

import static android.R.attr.accountType;
import static mkryglikov.shisha.LoginActivity.AUTH_TOKEN_TYPE;

class Authenticator extends AbstractAccountAuthenticator {

    static final String AUTH_GRANT_TYPE = "password";
    static final String REFRESH_GRANT_TYPE = "refresh_token";
    static final String APP_ID = "APP_ID";
    static final String APP_SECRET = "APP_SECRET";

    private String clientName;
    private Context context;
    private Api api = RetrofitClient.getClient();

    Authenticator(Context c) {
        super(c);
        context = c;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(context, LoginActivity.class);

        intent.putExtra(LoginActivity.ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        //Пытаемся получить токен у Account Manager'а
        final AccountManager am = AccountManager.get(context);
        String authToken = am.peekAuthToken(account, authTokenType);
        String refreshToken = am.getUserData(account, LoginActivity.KEY_REFRESH_TOKEN);

        //Проверяем валидность токена
        try {
            Response<List> verifyResponse = api.verifyToken(AUTH_TOKEN_TYPE + " " + authToken).execute();
            if (verifyResponse.code() == 401) {
                Response<AuthResponse> refreshResponse =
                        api.refreshToken(REFRESH_GRANT_TYPE, APP_ID, APP_SECRET, refreshToken).execute();
                if (refreshResponse.code() == 200) {
                    authToken = refreshResponse.body().getAccessToken();
                    refreshToken = refreshResponse.body().getRefreshToken();
                } else {
                    Log.d("FUCK", "Ошибка при обновлении токена");
                }
            } else if (verifyResponse.code() != 200) {
                Log.d("FUCK", "Ошибка при проверки валидности");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            Response<ClientInfo> userInfoResponse = api.getClientInfo(AUTH_TOKEN_TYPE + " " + authToken).execute();
            if (userInfoResponse.code() == 200)
                clientName = userInfoResponse.body().getName();
            else if (userInfoResponse.code() != 200)
                Log.d("FUCK", "Ошибка при получении данных пользователя после получения токена");
        } catch (IOException e) {
            e.printStackTrace();
        }

//      Возвращаем токен, если получили его
        if (!TextUtils.isEmpty(authToken) && !TextUtils.isEmpty(refreshToken) && !TextUtils.isEmpty(clientName)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

            result.putString(LoginActivity.KEY_REFRESH_TOKEN, refreshToken);
            am.setUserData(account, LoginActivity.KEY_REFRESH_TOKEN, refreshToken);

            result.putString(LoginActivity.KEY_CLIENT_NAME, clientName);
            am.setUserData(account, LoginActivity.KEY_CLIENT_NAME, clientName);

            return result;
        }


//      Если не получили токен, вызываем LoginActivity как в addAccount()
        final Intent intent = new Intent(context, LoginActivity.class);

        intent.putExtra(LoginActivity.ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }
}
