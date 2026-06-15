package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.MeResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalXp = new MutableLiveData<>();
    private final MutableLiveData<Integer> dailyStreak = new MutableLiveData<>();
    private final MutableLiveData<String> levelName = new MutableLiveData<>();

    public LiveData<String> getUsername() { return username; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<Integer> getTotalXp() { return totalXp; }
    public LiveData<Integer> getDailyStreak() { return dailyStreak; }
    public LiveData<String> getLevelName() { return levelName; }

    public void loadProfile(String token) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getMe("Bearer " + token).enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(Call<MeResponse> call, Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MeResponse me = response.body();
                    username.postValue(me.getUsername());
                    email.postValue(me.getEmail());
                    totalXp.postValue(me.getTotalXp());
                    dailyStreak.postValue(me.getDailyStreak());
                    levelName.postValue(me.getLevelName());
                }
            }
            @Override
            public void onFailure(Call<MeResponse> call, Throwable t) {}
        });
    }
}