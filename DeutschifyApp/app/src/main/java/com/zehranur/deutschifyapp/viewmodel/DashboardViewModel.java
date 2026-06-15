package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.StatsResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<Integer> totalXp = new MutableLiveData<>();
    private final MutableLiveData<Integer> dailyStreak = new MutableLiveData<>();
    private final MutableLiveData<Integer> successRate = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCorrect = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalWrong = new MutableLiveData<>();
    private final MutableLiveData<int[]> weeklySessions = new MutableLiveData<>();

    public LiveData<Integer> getTotalXp() { return totalXp; }
    public LiveData<Integer> getDailyStreak() { return dailyStreak; }
    public LiveData<Integer> getSuccessRate() { return successRate; }
    public LiveData<Integer> getTotalCorrect() { return totalCorrect; }
    public LiveData<Integer> getTotalWrong() { return totalWrong; }
    public LiveData<int[]> getWeeklySessions() { return weeklySessions; }

    public void loadStats(String token) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getStats("Bearer " + token).enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatsResponse body = response.body();
                    totalXp.postValue(body.getTotalXp());
                    dailyStreak.postValue(body.getDailyStreak());
                    successRate.postValue(body.getSuccessRate());
                    totalCorrect.postValue(body.getTotalCorrect());
                    totalWrong.postValue(body.getTotalWrong());
                    List<Integer> sessions = body.getWeeklySessions();
                    int[] counts = new int[7];
                    if (sessions != null) {
                        for (int i = 0; i < Math.min(sessions.size(), 7); i++) {
                            counts[i] = sessions.get(i);
                        }
                    }
                    weeklySessions.postValue(counts);
                }
            }
            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {}
        });
    }
}