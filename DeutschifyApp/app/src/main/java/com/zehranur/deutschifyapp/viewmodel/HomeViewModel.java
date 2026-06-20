package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.StatsResponse;
import com.zehranur.deutschifyapp.model.WordResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> wordOfDay = new MutableLiveData<>();
    private final MutableLiveData<String> wordOfDayTranslation = new MutableLiveData<>();
    private final MutableLiveData<String> wordOfDayExampleDe = new MutableLiveData<>();
    private final MutableLiveData<String> wordOfDayExampleTr = new MutableLiveData<>();
    private final MutableLiveData<int[]> weeklySessionCounts = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalXp = new MutableLiveData<>();
    private final MutableLiveData<Integer> dailyStreak = new MutableLiveData<>();

    public LiveData<String> getWordOfDay() { return wordOfDay; }
    public LiveData<String> getWordOfDayTranslation() { return wordOfDayTranslation; }
    public LiveData<String> getWordOfDayExampleDe() { return wordOfDayExampleDe; }
    public LiveData<String> getWordOfDayExampleTr() { return wordOfDayExampleTr; }
    public LiveData<int[]> getWeeklySessionCounts() { return weeklySessionCounts; }
    public LiveData<Integer> getTotalXp() { return totalXp; }
    public LiveData<Integer> getDailyStreak() { return dailyStreak; }

    public void loadHomeData(String token) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);

        api.getDailyWord().enqueue(new Callback<WordResponse>() {
            @Override
            public void onResponse(Call<WordResponse> call, Response<WordResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WordResponse w = response.body();
                    wordOfDay.postValue(w.getGermanWord());
                    wordOfDayTranslation.postValue(w.getTurkishMeaning());
                    wordOfDayExampleDe.postValue(w.getExampleSentenceDe() != null ? w.getExampleSentenceDe() : "");
                    wordOfDayExampleTr.postValue(w.getExampleSentenceTr() != null ? w.getExampleSentenceTr() : "");
                }
            }
            @Override
            public void onFailure(Call<WordResponse> call, Throwable t) {}
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