package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.ReviewResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewViewModel extends ViewModel {

    private final MutableLiveData<List<ReviewResponse.ReviewWord>> words = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<ReviewResponse.ReviewWord>> getWords() { return words; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<Boolean> isEmpty() { return isEmpty; }
    public LiveData<String>getErrorMessage() { return errorMessage;}

    public void loadReviewWords(String token) {
        isLoading.setValue(true);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getReviewWords("Bearer " + token).enqueue(new Callback<ReviewResponse>() {
            @Override
            public void onResponse(Call<ReviewResponse> call, Response<ReviewResponse> response) {
                isLoading.postValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<ReviewResponse.ReviewWord> list = response.body().getWords();
                    if (list == null || list.isEmpty()) {
                        isEmpty.postValue(true);
                    } else {
                        words.postValue(list);
                    }
                } else {
                    errorMessage.postValue("Kelimeler yüklenemedi");
                }
            }
            @Override
            public void onFailure(Call<ReviewResponse> call, Throwable t) {
                isLoading.postValue(false);
                errorMessage.postValue("Bağlantı hatası: " +t.getMessage());
            }
        });
    }
}