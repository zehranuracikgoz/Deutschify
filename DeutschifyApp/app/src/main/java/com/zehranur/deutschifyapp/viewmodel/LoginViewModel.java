package com.zehranur.deutschifyapp.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.zehranur.deutschifyapp.model.LoginRequest;
import com.zehranur.deutschifyapp.model.LoginResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginViewModel extends AndroidViewModel {

    public final MutableLiveData<LoginResponse> loginSuccess = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final ApiService api;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        api = RetrofitClient.getInstance().create(ApiService.class);
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    SharedPreferences.Editor editor = getApplication()
                            .getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                            .edit();
                    editor.putString("access_token", body.access_token);
                    editor.putInt("user_id", body.user_id);
                    editor.putString("username", body.username);
                    editor.apply();
                    loginSuccess.setValue(body);
                } else {
                    errorMessage.setValue("Geçersiz email veya şifre");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }
}