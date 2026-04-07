package com.zehranur.deutschifyapp.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.zehranur.deutschifyapp.model.RegisterRequest;
import com.zehranur.deutschifyapp.model.RegisterResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterViewModel extends AndroidViewModel {

    public final MutableLiveData<String> registerSuccess = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final ApiService api;

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        api = RetrofitClient.getInstance().create(ApiService.class);
    }

    public void register(String username, String email, String password) {
        isLoading.setValue(true);
        api.register(new RegisterRequest(username, email, password)).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    registerSuccess.setValue(response.body().message);
                } else if (response.code() == 409) {
                    errorMessage.setValue("Bu email zaten kayıtlı");
                } else {
                    errorMessage.setValue("Kayıt başarısız, tekrar deneyin");
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }
}