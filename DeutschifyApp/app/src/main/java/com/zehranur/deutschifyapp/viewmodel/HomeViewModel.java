package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.QueueResponse;
import com.zehranur.deutschifyapp.model.Word;
import com.zehranur.deutschifyapp.model.StatsResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> wordOfDay = new MutableLiveData<>();
    private final MutableLiveData<String> wordOfDayTranslation = new MutableLiveData<>();
    private final MutableLiveData<int[]> weeklySessionCounts = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalXp = new MutableLiveData<>();
    private final MutableLiveData<Integer> dailyStreak = new MutableLiveData<>();

    public LiveData<String> getWordOfDay() { return wordOfDay; }
    public LiveData<String> getWordOfDayTranslation() { return wordOfDayTranslation; }
    public LiveData<int[]> getWeeklySessionCounts() { return weeklySessionCounts; }
    public LiveData<Integer> getTotalXp() { return totalXp; }
    public LiveData<Integer> getDailyStreak() { return dailyStreak; }

    public void loadHomeData(String token, int userId) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);

        api.getStudyQueue(userId, 1).enqueue(new Callback<QueueResponse>() {
            @Override
            public void onResponse(Call<QueueResponse> call, Response<QueueResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getQueue() != null
                        && !response.body().getQueue().isEmpty()) {
                    Word word = response.body().getQueue().get(0);
                    wordOfDay.postValue(word.getGermanWord());
                    wordOfDayTranslation.postValue(word.getTurkishMeaning());
                } else {
                    wordOfDay.postValue("lernen");
                    wordOfDayTranslation.postValue("öğrenmek");
                }
            }
            @Override
            public void onFailure(Call<QueueResponse> call, Throwable t) {
                wordOfDay.postValue("lernen");
                wordOfDayTranslation.postValue("öğrenmek");
            }
        });

        api.getStats("Bearer " + token).enqueue(new Callback<StatsResponse>() {
            @Override
            public void onResponse(Call<StatsResponse> call, Response<StatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    totalXp.postValue(response.body().getTotalXp());
                    dailyStreak.postValue(response.body().getDailyStreak());
                    List<Integer> sessions = response.body().getWeeklySessions();
                    int[] counts = new int[7];
                    if (sessions != null) {
                        for (int i = 0; i < Math.min(sessions.size(), 7); i++) {
                            counts[i] = sessions.get(i);
                        }
                    }
                    weeklySessionCounts.postValue(counts);
                }
            }
            @Override
            public void onFailure(Call<StatsResponse> call, Throwable t) {}
        });
    }
}