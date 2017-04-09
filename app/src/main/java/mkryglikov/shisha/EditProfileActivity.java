package mkryglikov.shisha;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;

import com.philliphsu.bottomsheetpickers.date.BottomSheetDatePickerDialog;
import com.philliphsu.bottomsheetpickers.date.DatePickerDialog;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Pattern;

import mkryglikov.data.RetrofitClient;
import mkryglikov.data.models.ClientInfo;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class EditProfileActivity extends AppCompatActivity implements com.philliphsu.bottomsheetpickers.date.DatePickerDialog.OnDateSetListener {

    private static final int IMAGE_PICKER = 7;
    private Account account;
    private AccountManager am;
    private ConnectivityManager cm;
    private ImageView ivAvatar;
    private EditText etEditName, etEditSurname, etEditPhone, etEditBirthday;
    private TextInputLayout tilEditName, tilEditSurname, tilEditPhone, tilEditBirthday;
    private Button btnEditSave;
    private ProgressBar pbEdit, pbWait;
    private ScrollView sv;
    private int bdYear, bdMonth, bdDay;
    private String token, name, surname, birthday, id;
    private Pattern ruPattern = Pattern.compile("[а-яА-ЯёЁ]*");
    private Long phone;
    private Calendar calendar = Calendar.getInstance();
    private BottomSheetDatePickerDialog dpBirthday;
    private MultipartBody.Part avatar;
    private SimpleDateFormat dateParse = new SimpleDateFormat("yyyy-MM-dd");
    private Api api = RetrofitClient.getClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        am = AccountManager.get(this);
        cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (am.getAccountsByType(LoginActivity.ACCOUNT_TYPE).length > 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
                return;
            account = am.getAccountsByType(LoginActivity.ACCOUNT_TYPE)[0];
            am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, this, new OnTokenAcquired(), null);
        } else {
            am.addAccount(LoginActivity.ACCOUNT_TYPE, LoginActivity.AUTH_TOKEN_TYPE, null, null, this, null, null);
            finish();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.title_activity_edit_profile);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_close);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        tilEditName = (TextInputLayout) findViewById(R.id.tilEditName);
        etEditName = (EditText) findViewById(R.id.etEditName);
        etEditName.addTextChangedListener(new InputTextWatcher(etEditName));

        tilEditSurname = (TextInputLayout) findViewById(R.id.tilEditSurname);
        etEditSurname = (EditText) findViewById(R.id.etEditSurname);
        etEditSurname.addTextChangedListener(new InputTextWatcher(etEditSurname));

        tilEditPhone = (TextInputLayout) findViewById(R.id.tilEditPhone);
        etEditPhone = (EditText) findViewById(R.id.etEditPhone);
        etEditPhone.addTextChangedListener(new InputTextWatcher(etEditPhone));

        tilEditBirthday = (TextInputLayout) findViewById(R.id.tilEditBirthday);
        etEditBirthday = (EditText) findViewById(R.id.etEditBirthday);
        etEditBirthday.addTextChangedListener(new InputTextWatcher(etEditBirthday));
        etEditBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dpBirthday.show(getSupportFragmentManager(), "Date");
            }
        });

        sv = (ScrollView) findViewById(R.id.sv);
        pbEdit = (ProgressBar) findViewById(R.id.pbEdit);
        pbWait = (ProgressBar) findViewById(R.id.pbWait);

        ivAvatar = (ImageView) findViewById(R.id.ivAvatar);
        ivAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, IMAGE_PICKER);

            }
        });

        btnEditSave = (Button) findViewById(R.id.btnEditSave);
        btnEditSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateName() && validateSurname() && validatePhone() && validateBirthday() && checkNetwork()) {
                    btnEditSave.setVisibility(View.GONE);
                    pbEdit.setVisibility(View.VISIBLE);
                    updateUserInfo();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (requestCode == IMAGE_PICKER && resultCode == RESULT_OK && imageReturnedIntent != null) {
            Uri selectedImage = imageReturnedIntent.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            File file = new File(cursor.getString(columnIndex));
            RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
            avatar = MultipartBody.Part.createFormData("file", file.getName(), requestBody);


        }
    }

    @Override
    public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
        bdYear = year;
        bdMonth = monthOfYear;
        bdDay = dayOfMonth;
        birthday = year + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
        etEditBirthday.setText(dayOfMonth + "." + (monthOfYear + 1) + "." + year);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED, null);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkNetwork() {
        if (cm.getActiveNetworkInfo() == null) {
            Snackbar.make(findViewById(R.id.activity_edit_profile), "Нет соединения с сетью", Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
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
                getUserInfo();
        }
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
                case R.id.etEditName:
                    validateName();
                    break;
                case R.id.etEditSurname:
                    validateSurname();
                    break;
                case R.id.etEditPhone:
                    validatePhone();
                    break;
                case R.id.etEditBirthday:
                    validateBirthday();
                    break;
            }
        }
    }

    private void getUserInfo() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                sv.setVisibility(View.GONE);
                pbWait.setVisibility(View.VISIBLE);
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Response<ClientInfo> response = api.getClientInfo(LoginActivity.AUTH_TOKEN_TYPE + " " + token).execute();
                    if (response.body() != null && response.code() == 200) {
                        Calendar bdCalendar = Calendar.getInstance();
                        name = (response.body().getName()).split(" ")[0];
                        surname = (response.body().getName()).split(" ")[1];
                        birthday = response.body().getBirthday();
                        phone = response.body().getPhone();
                        id = response.body().getId();

                        try {
                            bdCalendar.setTime(dateParse.parse(response.body().getBirthday()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        bdYear = bdCalendar.get(Calendar.YEAR);
                        bdMonth = bdCalendar.get(Calendar.MONTH);
                        bdDay = bdCalendar.get(Calendar.DAY_OF_MONTH);

                        dpBirthday = BottomSheetDatePickerDialog.newInstance(
                                EditProfileActivity.this,
                                bdYear,
                                bdMonth,
                                bdDay);


                    } else if (response.code() == 401) {
                        am.invalidateAuthToken(LoginActivity.ACCOUNT_TYPE, token);
                        am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, EditProfileActivity.this, new OnTokenAcquired(), null);
                    } else
                        Log.w("FUCK", "Не получили иннформацию о пользователе в страницу редактирования данных " + response.code());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Picasso.with(EditProfileActivity.this)
                        .load("https://api.mkryglikov.ru/avatars/" + id + ".jpg")
                        .into(ivAvatar);
                etEditName.setText(name);
                etEditSurname.setText(surname);
                etEditPhone.setText(String.valueOf(phone));
                etEditBirthday.setText(bdDay + "." + (bdMonth + 1) + "." + bdYear);

                pbWait.setVisibility(View.GONE);
                sv.setVisibility(View.VISIBLE);
                sv.startAnimation(AnimationUtils.loadAnimation(EditProfileActivity.this, R.anim.fade_in));
            }
        }.execute();
    }

    private void updateUserInfo() {
        String fullName = etEditName.getText().toString() + " " + etEditSurname.getText().toString();
        phone = Long.valueOf(etEditPhone.getText().toString());
        birthday = String.valueOf(bdYear) + "-" + String.valueOf(bdMonth + 1) + "-" + String.valueOf(bdDay);

        api.updateUserInfo(LoginActivity.AUTH_TOKEN_TYPE + " " + token, fullName, phone, birthday).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    if (avatar != null) {
                        api.setAvatar(LoginActivity.AUTH_TOKEN_TYPE + " " + token, avatar).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if (response.code() == 200) {
                                    Log.d("FUCK", "Загрузили!");
                                    setResult(RESULT_OK, null);
                                    finish();
                                } else
                                    Log.d("FUCK", "Не 200!");
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                Log.d("FUCK", "onFailure!");
                            }
                        });
                    } else {
                        setResult(RESULT_OK, null);
                        finish();
                    }
                } else if (response.code() == 401) {
                    am.invalidateAuthToken(LoginActivity.ACCOUNT_TYPE, token);
                    am.getAuthToken(account, LoginActivity.AUTH_TOKEN_TYPE, null, EditProfileActivity.this, new OnTokenAcquired(), null);
                } else {
                    Log.w("FUCK", "Ошибка при отправке обновленных данных клиента " + response.code());
                    pbEdit.setVisibility(View.GONE);
                    btnEditSave.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private boolean validateName() {
        if (etEditName.getText().length() < 2) {
            tilEditName.setError("Введите имя");
            return false;
        } else if (!ruPattern.matcher(etEditName.getText().toString()).matches()) {
            tilEditName.setError("Допускаются только буквы русского алфавита");
            return false;
        } else if (etEditName.getText().length() > 20) {
            tilEditName.setError("Длина не может быть более 20 символов");
            return false;
        } else
            tilEditName.setErrorEnabled(false);

        return true;
    }

    private boolean validateSurname() {
        if (etEditSurname.getText().length() < 2) {
            tilEditSurname.setError("Введите фамилию");
            return false;
        } else if (!ruPattern.matcher(etEditSurname.getText().toString()).matches()) {
            tilEditSurname.setError("Допускаются только буквы русского алфавита");
            return false;
        } else if (etEditSurname.getText().length() > 20) {
            tilEditSurname.setError("Длина не может быть более 20 символов");
            return false;
        } else
            tilEditSurname.setErrorEnabled(false);

        return true;
    }

    private boolean validatePhone() {
        if (etEditPhone.getText().length() != 10) {
            tilEditPhone.setError("Введите 10-значный номер телефона");
            return false;
        } else
            tilEditPhone.setErrorEnabled(false);

        return true;
    }

    private boolean validateBirthday() {
        if (TextUtils.isEmpty(etEditBirthday.getText().toString())) {
            tilEditBirthday.setError("Выберите дату рождения");
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
            tilEditBirthday.setError("Только 18+");
            return false;
        } else
            tilEditBirthday.setErrorEnabled(false);

        return true;
    }
}
