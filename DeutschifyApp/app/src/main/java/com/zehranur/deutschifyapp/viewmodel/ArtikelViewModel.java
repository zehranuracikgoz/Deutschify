package com.zehranur.deutschifyapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.zehranur.deutschifyapp.model.WordResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtikelViewModel extends ViewModel {

    public static final int DER = 1;
    public static final int DIE = 2;
    public static final int DAS = 3;

    private final MutableLiveData<WordResponse> currentWord = new MutableLiveData<>();
    private final MutableLiveData<String> feedbackText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> feedbackCorrect = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private List<WordResponse> words;
    private int currentIndex = 0;

    public LiveData<WordResponse> getCurrentWord() { return currentWord; }
    public LiveData<String> getFeedbackText() { return feedbackText; }
    public LiveData<Boolean> getFeedbackCorrect() { return feedbackCorrect; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadWords() {
        isLoading.setValue(true);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        api.getWords(true).enqueue(new Callback<List<WordResponse>>() {
            @Override
            public void onResponse(Call<List<WordResponse>> call, Response<List<WordResponse>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    words=response.body();
                    Collections.shuffle(words);
                    currentIndex = 0;
                    currentWord.setValue(words.get(0));
                } else {
                    errorMessage.setValue("Kelimeler yüklenemedi");
                }
            }

            @Override
            public void onFailure(Call<List<WordResponse>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Bağlantı hatası: " + t.getMessage());
            }
        });
    }

    public void checkAnswer(int selectedArticleId) {
        if (words==null || currentIndex >= words.size()) return;
        WordResponse word = words.get(currentIndex);
        if (word.getArticleId() != null && word.getArticleId() == selectedArticleId) {
            feedbackCorrect.setValue(true);
            feedbackText.setValue("Doğru!");
        } else {
            feedbackCorrect.setValue(false);
            feedbackText.setValue("Yanlış! Doğrusu: " + articleName(word.getArticleId()));
        }
    }

    public void nextWord() {
        if (words == null) return;
        currentIndex++;
        if (currentIndex >= words.size()) {
            Collections.shuffle(words);
            currentIndex =0;
        }
        currentWord.setValue(words.get(currentIndex));
        feedbackText.setValue(null);
    }

    private String articleName(Integer id) {
        if (id == null) return "";
        switch (id) {
            case 1: return "DER";
            case 2: return "DIE";
            case 3: return "DAS";
            default: return "";
        }
    }
}