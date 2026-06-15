package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.HistoryResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryViewModel extends ViewModel {

    private final MutableLiveData<List<HistoryResponse.Session>> sessions = new MutableLiveData<>();

    public LiveData<List<HistoryResponse.Session>> getSessions() { return sessions; }

    public void loadHistory(String token) {
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getHistory("Bearer " + token).enqueue(new Callback<HistoryResponse>() {
            @Override
            public void onResponse(Call<HistoryResponse> call, Response<HistoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessions.postValue(response.body().getSessions());
                } else {
                    sessions.postValue(null);
                }
            }
            @Override
            public void onFailure(Call<HistoryResponse> call, Throwable t) {
                sessions.postValue(null);
            }
        });
    }
}