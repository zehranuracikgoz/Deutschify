package com.zehranur.deutschifyapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.zehranur.deutschifyapp.model.AnswerRequest;
import com.zehranur.deutschifyapp.model.AnswerResponse;
import com.zehranur.deutschifyapp.model.QueueResponse;
import com.zehranur.deutschifyapp.model.Word;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WordCardActivity extends AppCompatActivity {

    private static final int SESSION_LIMIT = 5;

    private View cardFront;
    private View cardBack;
    private View cardContainer;
    private TextView tvGermanWord;
    private TextView tvTurkishWord;
    private TextView tvExampleDe;
    private TextView tvExampleTr;
    private TextView tvKartCounter;
    private LinearLayout layoutDifficulty;
    private ProgressBar progressBar;

    private ApiService api;
    private List<Word> wordQueue;
    private int currentIndex = 0;
    private int userId;
    private boolean isFlipped = false;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_card);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userId = prefs.getInt("user_id", 1);

        cardContainer = findViewById(R.id.card_container);
        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        tvGermanWord = findViewById(R.id.tv_german_word);
        tvTurkishWord = findViewById(R.id.tv_turkish_word);
        tvExampleDe = findViewById(R.id.tv_example_de);
        tvExampleTr = findViewById(R.id.tv_example_tr);
        tvKartCounter = findViewById(R.id.tv_kart_counter);
        layoutDifficulty = findViewById(R.id.layout_difficulty);
        progressBar = findViewById(R.id.progress_bar);

        api = RetrofitClient.getInstance().create(ApiService.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        cardContainer.setOnClickListener(v -> flipCard());
        findViewById(R.id.btn_telafuz).setOnClickListener(v ->
                Toast.makeText(this, "Telafuz yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_quality_1).setOnClickListener(v -> submitAnswer(1));
        findViewById(R.id.btn_quality_2).setOnClickListener(v -> submitAnswer(2));
        findViewById(R.id.btn_quality_3).setOnClickListener(v -> submitAnswer(3));
        findViewById(R.id.btn_quality_4).setOnClickListener(v -> submitAnswer(4));
        findViewById(R.id.btn_quality_5).setOnClickListener(v -> submitAnswer(5));

        loadStudyQueue();
    }

    private void loadStudyQueue() {
        api.getStudyQueue(userId, SESSION_LIMIT).enqueue(new Callback<QueueResponse>() {
            @Override
            public void onResponse(Call<QueueResponse> call, Response<QueueResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    wordQueue = response.body().getQueue();
                    if (wordQueue != null && !wordQueue.isEmpty()) {
                        progressBar.setMax(wordQueue.size());
                        showWord(0);
                    } else {
                        tvGermanWord.setText("Bugün çalışılacak kelime yok!");
                    }
                } else {
                    Toast.makeText(WordCardActivity.this,
                            "Kelimeler yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<QueueResponse> call, Throwable t) {
                Toast.makeText(WordCardActivity.this,
                        "Bağlantı hatası: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showWord(int index) {
        currentIndex = index;
        Word word = wordQueue.get(index);
        tvGermanWord.setText(word.getGermanWord());
        tvTurkishWord.setText(word.getTurkishMeaning());
        tvExampleDe.setText(word.getExampleSentenceDe());
        tvExampleTr.setText(word.getExampleSentenceTr());
        tvKartCounter.setText("KART " + (index + 1));
        progressBar.setProgress(index);

        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        cardFront.setRotationY(0f);
        cardBack.setRotationY(0f);
        layoutDifficulty.setVisibility(View.INVISIBLE);
        isFlipped = false;
        isAnimating = false;
    }

    private void flipCard() {
        if (isAnimating) return;
        isAnimating = true;

        View hideView = isFlipped ? cardBack : cardFront;
        View showView = isFlipped ? cardFront : cardBack;
        boolean flippingToBack = !isFlipped;

        ObjectAnimator flipOut = ObjectAnimator.ofFloat(hideView, "rotationY", 0f, 90f);
        flipOut.setDuration(180);
        flipOut.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator flipIn = ObjectAnimator.ofFloat(showView, "rotationY", -90f, 0f);
        flipIn.setDuration(180);
        flipIn.setInterpolator(new DecelerateInterpolator());

        flipIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }
        });

        flipOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hideView.setVisibility(View.GONE);
                showView.setVisibility(View.VISIBLE);
                flipIn.start();
                isFlipped = flippingToBack;
                layoutDifficulty.setVisibility(isFlipped ? View.VISIBLE : View.INVISIBLE);
            }
        });

        flipOut.start();
    }

    private void submitAnswer(int quality) {
        layoutDifficulty.setVisibility(View.INVISIBLE);

        api.submitAnswer(new AnswerRequest(userId, wordQueue.get(currentIndex).getWordId(), quality))
                .enqueue(new Callback<AnswerResponse>() {
                    @Override
                    public void onResponse(Call<AnswerResponse> call, Response<AnswerResponse> response) {
                        int next = currentIndex + 1;
                        if (next < wordQueue.size()) {
                            showWord(next);
                        } else {
                            progressBar.setProgress(wordQueue.size());
                            Toast.makeText(WordCardActivity.this,
                                    "Oturum tamamlandı!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(WordCardActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<AnswerResponse> call, Throwable t) {
                        layoutDifficulty.setVisibility(View.VISIBLE);
                        Toast.makeText(WordCardActivity.this,
                                "Hata: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}