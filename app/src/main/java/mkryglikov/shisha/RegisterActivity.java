package mkryglikov.shisha;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.philliphsu.bottomsheetpickers.date.DatePickerDialog;

import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Pattern;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.models.AuthResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener, com.philliphsu.bottomsheetpickers.date.DatePickerDialog.OnDateSetListener {
    private Button btnRegisterConfirm;
    private ViewSwitcher vsRegister;
    private EditText etRegisterName, etRegisterSurname, etRegisterPhone, etRegisterBirthday;
    private EditText etRegisterUsername, etRegisterPassword, etRegisterPasswordRepeat;
    private TextInputLayout tilRegisterName, tilRegisterSurname, tilRegisterPhone, tilRegisterBirthday;
    private TextInputLayout tilRegisterUsername, tilRegisterPassword, tilRegisterPasswordRepeat;
    private String username, password, name, birthday;
    private long phone;
    private Calendar calendar = Calendar.getInstance();
    private int bdYear, bdMonth, bdDay;
    private Pattern ruPattern = Pattern.compile("[а-яА-ЯёЁ]*"), engPattern = Pattern.compile("[_.A-Za-z\\d]*");
    private DatePickerDialog dpBirthday;
    private Api api = RetrofitClient.getClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ((TextView) findViewById(R.id.tvLogo)).setTypeface(Typeface.createFromAsset(getAssets(), getString(R.string.bebas_regular_font)));

        btnRegisterConfirm = (Button) findViewById(R.id.btnRegisterConfirm);
        btnRegisterConfirm.setOnClickListener(this);

        etRegisterName = (EditText) findViewById(R.id.etRegisterName);
        etRegisterSurname = (EditText) findViewById(R.id.etRegisterSurname);
        etRegisterPhone = (EditText) findViewById(R.id.etRegisterPhone);
        etRegisterBirthday = (EditText) findViewById(R.id.etRegisterBirthday);
        etRegisterBirthday.setOnClickListener(this);

        etRegisterUsername = (EditText) findViewById(R.id.etRegisterUsername);
        etRegisterPassword = (EditText) findViewById(R.id.etRegisterPassword);
        etRegisterPasswordRepeat = (EditText) findViewById(R.id.etRegisterPasswordRepeat);

        etRegisterName.addTextChangedListener(new InputTextWatcher(etRegisterName));
        etRegisterSurname.addTextChangedListener(new InputTextWatcher(etRegisterSurname));
        etRegisterPhone.addTextChangedListener(new InputTextWatcher(etRegisterPhone));
        etRegisterBirthday.addTextChangedListener(new InputTextWatcher(etRegisterBirthday));

        etRegisterUsername.addTextChangedListener(new InputTextWatcher(etRegisterUsername));
        etRegisterPassword.addTextChangedListener(new InputTextWatcher(etRegisterPassword));
        etRegisterPasswordRepeat.addTextChangedListener(new InputTextWatcher(etRegisterPasswordRepeat));

        tilRegisterName = (TextInputLayout) findViewById(R.id.tilRegisterName);
        tilRegisterSurname = (TextInputLayout) findViewById(R.id.tilRegisterSurname);
        tilRegisterPhone = (TextInputLayout) findViewById(R.id.tilRegisterPhone);
        tilRegisterBirthday = (TextInputLayout) findViewById(R.id.tilRegisterBirthday);

        tilRegisterUsername = (TextInputLayout) findViewById(R.id.tilRegisterUsername);
        tilRegisterPassword = (TextInputLayout) findViewById(R.id.tilRegisterPassword);
        tilRegisterPasswordRepeat = (TextInputLayout) findViewById(R.id.tilRegisterPasswordRepeat);

        vsRegister = (ViewSwitcher) findViewById(R.id.vsRegister);
        Animation inAnim = new AlphaAnimation(0, 1);
        inAnim.setDuration(250);
        Animation outAnim = new AlphaAnimation(1, 0);
        outAnim.setDuration(250);

        vsRegister.setInAnimation(inAnim);
        vsRegister.setOutAnimation(outAnim);

        dpBirthday = DatePickerDialog.newInstance(
                RegisterActivity.this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRegisterConfirm:
                if (btnRegisterConfirm.getTag().equals("0")) {
                    if (validateName() && validateSurname() && validatePhone() && validateBirthday()) {
                        vsRegister.showNext();
                        btnRegisterConfirm.setTag("1");
                        btnRegisterConfirm.setText("Зарегистрироваться");
                    }
                } else if (btnRegisterConfirm.getTag().equals("1")) {
                    if (validateUsername() && validatePassword() && validatePasswordRepeat())
                        register();
                }
                break;
            case R.id.etRegisterBirthday:
                dpBirthday.show(getSupportFragmentManager(), "Date");
                break;
        }
    }

    public void register() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                username = etRegisterUsername.getText().toString();
                password = etRegisterPassword.getText().toString();
                name = etRegisterName.getText().toString() + " " + etRegisterSurname.getText().toString();
                phone = Long.valueOf(etRegisterPhone.getText().toString());
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Response<ResponseBody> registerResponse = api.register(username, password, name, phone, birthday).execute();
                    if (registerResponse.code() == 200) {
                        Response<AuthResponse> authResponse = api.auth(Authenticator.AUTH_GRANT_TYPE, Authenticator.APP_ID, Authenticator.APP_SECRET, username, password).execute();
                        if (authResponse.body() != null && authResponse.code() == 200) {
                            final Account account = new Account(name, LoginActivity.ACCOUNT_TYPE);
                            AccountManager am = AccountManager.get(getApplicationContext());
                            am.addAccountExplicitly(account, null, null);
                            am.setUserData(account, LoginActivity.KEY_REFRESH_TOKEN, authResponse.body().getRefreshToken());
                            am.setAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, authResponse.body().getAccessToken());
                            setResult(RESULT_OK, null);
                            finish();
                        } else
                            Snackbar.make(findViewById(R.id.activity_login), "Ошибка при авторизации " + authResponse.code(), Snackbar.LENGTH_LONG).show();
                    } else if (registerResponse.code() == 404) {
                        //ToDo???
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    @Override
    public void onBackPressed() {
        if (btnRegisterConfirm.getTag().equals("1")) {
            vsRegister.showPrevious();
            btnRegisterConfirm.setTag("0");
            btnRegisterConfirm.setText("Далее");
        } else
            super.onBackPressed();
    }

    @Override
    public void onDateSet(com.philliphsu.bottomsheetpickers.date.DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        bdYear = year;
        bdMonth = monthOfYear;
        bdDay = dayOfMonth;
        birthday = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
        etRegisterBirthday.setText(dayOfMonth + "." + (monthOfYear + 1) + "." + year);
    }

    private class InputTextWatcher implements TextWatcher {
        private View view;

        private InputTextWatcher(View view) {
            this.view = view;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            switch (view.getId()) {
                case R.id.etRegisterName:
                    validateName();
                    break;
                case R.id.etRegisterSurname:
                    validateSurname();
                    break;
                case R.id.etRegisterPhone:
                    validatePhone();
                    break;
                case R.id.etRegisterBirthday:
                    validateBirthday();
                    break;
                case R.id.etRegisterUsername:
                    validateUsername();
                    break;
                case R.id.etRegisterPassword:
                    validatePassword();
                    break;
                case R.id.etRegisterPasswordRepeat:
                    validatePasswordRepeat();
                    break;
            }
        }
    }

    private boolean validateName() {
        if (etRegisterName.getText().length() < 2) {
            tilRegisterName.setError("Введите имя");
            return false;
        } else if (!ruPattern.matcher(etRegisterName.getText().toString()).matches()) {
            tilRegisterName.setError("Допускаются только буквы русского алфавита");
            return false;
        } else if (etRegisterName.getText().length() > 20) {
            tilRegisterName.setError("Длина не может быть более 20 символов");
            return false;
        } else {
            tilRegisterName.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateSurname() {
        if (etRegisterSurname.getText().length() < 2) {
            tilRegisterSurname.setError("Введите фамилию");
            return false;
        } else if (!ruPattern.matcher(etRegisterSurname.getText().toString()).matches()) {
            tilRegisterSurname.setError("Допускаются только буквы русского алфавита");
            return false;
        } else if (etRegisterSurname.getText().length() > 20) {
            tilRegisterSurname.setError("Длина не может быть более 20 символов");
            return false;
        } else {
            tilRegisterSurname.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePhone() {
        if (etRegisterPhone.getText().length() != 10) {
            tilRegisterPhone.setError("Введите 10-значный номер телефона");
            return false;
        } else {
            tilRegisterPhone.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateBirthday() {
        if (TextUtils.isEmpty(etRegisterBirthday.getText().toString())) {
            tilRegisterBirthday.setError("Выберите дату рождения");
            return false;
        }
        Boolean isEighteen = false;
                
        if ((calendar.get(Calendar.YEAR) - bdYear) == 18) {
            if (calendar.get(Calendar.MONTH) == bdMonth) {
                if (calendar.get(Calendar.DAY_OF_MONTH) > bdDay)
                    isEighteen = true;
            } else if (calendar.get(Calendar.MONTH) > bdMonth)
                isEighteen = true;
        } else if ((calendar.get(Calendar.YEAR) - bdYear) > 18)
            isEighteen = true;

        if (!isEighteen) {
            tilRegisterBirthday.setError("Только 18+");
            return false;
        } else {
            tilRegisterBirthday.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validateUsername() {
        if (etRegisterUsername.getText().length() < 6) {
            tilRegisterUsername.setError("Введите логин длиной не менее 6 символов");
            return false;
        } else if (!engPattern.matcher(etRegisterUsername.getText().toString()).matches()) {
            tilRegisterUsername.setError("Допускаются только буквы латинского алфавита, цифры и символы _.");
            return false;
        } else if (etRegisterUsername.getText().length() > 50) {
            tilRegisterUsername.setError("Длина не может быть более 50 символов");
            return false;
        } else {
            tilRegisterUsername.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePassword() {
        if (etRegisterPassword.getText().length() < 8) {
            tilRegisterPassword.setError("Введите пароль длиной не менее 8 символов");
            return false;
        } else if (!engPattern.matcher(etRegisterPassword.getText().toString()).matches()) {
            tilRegisterPassword.setError("Допускаются только буквы латинского алфавита, цифры и символы _.");
            return false;
        } else if (etRegisterPassword.getText().length() > 50) {
            tilRegisterPassword.setError("Длина не может быть более 50 символов");
            return false;
        } else {
            tilRegisterPassword.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePasswordRepeat() {
        if (!etRegisterPasswordRepeat.getText().toString().equals(etRegisterPassword.getText().toString())) {
            tilRegisterPasswordRepeat.setError("Пароли не совпадают");
            return false;
        } else {
            tilRegisterPasswordRepeat.setErrorEnabled(false);
        }
        return true;
    }

}
