package com.zehranur.deutschifyapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
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
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.model.AnswerRequest;
import com.zehranur.deutschifyapp.model.AnswerResponse;
import com.zehranur.deutschifyapp.model.ReviewResponse;
import com.zehranur.deutschifyapp.model.SessionStartResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import com.zehranur.deutschifyapp.viewmodel.ReviewViewModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewActivity extends AppCompatActivity {

    private ReviewViewModel viewModel;
    private ApiService api;

    private View cardContainer;
    private View cardFront;
    private View cardBack;
    private TextView tvGermanWord;
    private TextView tvTurkishWord;
    private TextView tvExampleDe;
    private TextView tvExampleTr;
    private TextView tvKartCounter;
    private LinearLayout layoutDifficulty;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private View spacerTop;
    private View spacerBottom;

    private List<ReviewResponse.ReviewWord> wordList;
    private int currentIndex = 0;
    private int userId;
    private int sessionId =-1;
    private int correctCount = 0;
    private int wrongCount = 0;
    private boolean isFlipped = false;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        userId=prefs.getInt("user_id", 1);

        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        cardContainer=findViewById(R.id.card_container);
        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        tvGermanWord = findViewById(R.id.tv_german_word);
        tvTurkishWord = findViewById(R.id.tv_turkish_word);
        tvExampleDe = findViewById(R.id.tv_example_de);
        tvExampleTr = findViewById(R.id.tv_example_tr);
        tvKartCounter = findViewById(R.id.tv_kart_counter);
        layoutDifficulty = findViewById(R.id.layout_difficulty);
        progressBar = findViewById(R.id.progress_bar);
        layoutEmpty =findViewById(R.id.layout_empty);
        spacerTop = findViewById(R.id.spacer_top);
        spacerBottom = findViewById(R.id.spacer_bottom);

        api = RetrofitClient.getInstance().create(ApiService.class);
        startStudySession();
        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        viewModel.getWords().observe(this, words -> {
            wordList = words;
            showCardArea(true);
            progressBar.setMax(words.size());
            tvKartCounter.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            showWord(0);
        });
        viewModel.isEmpty().observe(this, empty -> {
            if (empty) showCardArea(false);
        });

        viewModel.getErrorMessage().observe(this, msg ->
                Toast.makeText(this, msg,  Toast.LENGTH_SHORT).show());

        cardContainer.setOnClickListener(v -> flipCard());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_quality_1).setOnClickListener(v -> submitAnswer(1));
        findViewById(R.id.btn_quality_2).setOnClickListener(v -> submitAnswer(2));
        findViewById(R.id.btn_quality_3).setOnClickListener(v -> submitAnswer(3));
        findViewById(R.id.btn_quality_4).setOnClickListener(v -> submitAnswer(4));
        findViewById(R.id.btn_quality_5).setOnClickListener(v -> submitAnswer(5));

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
        findViewById(R.id.nav_stats).setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.nav_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.nav_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        viewModel.loadReviewWords(token);
    }

    private void showCardArea(boolean hasWords) {
        int cardVis = hasWords ? View.VISIBLE :  View.GONE;
        int emptyVis = hasWords ? View.GONE : View.VISIBLE;
        layoutEmpty.setVisibility(emptyVis);
        cardContainer.setVisibility(cardVis);
        spacerTop.setVisibility(cardVis);
        spacerBottom.setVisibility(cardVis);
    }

    private void showWord(int index) {
        currentIndex = index;
        ReviewResponse.ReviewWord word = wordList.get(index);
        tvGermanWord.setText(word.getGermanWord());
        tvTurkishWord.setText(word.getTurkishMeaning());
        tvExampleDe.setText(word.getExampleSentenceDe());
        tvExampleTr.setText(word.getExampleSentenceTr());
        tvKartCounter.setText("KART " +(index + 1));
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
        View showView =isFlipped ? cardFront : cardBack;
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
                flipIn.start() ;
                isFlipped = flippingToBack;
                layoutDifficulty.setVisibility(isFlipped ? View.VISIBLE : View.INVISIBLE);
            }
        });

        flipOut.start();
    }

    private void submitAnswer(int quality) {
        if (wordList ==null) return;
        layoutDifficulty.setVisibility(View.INVISIBLE);
        if (quality >= 3) correctCount++;
        else wrongCount++;

        AnswerRequest request = new AnswerRequest(userId, wordList.get(currentIndex).getId(), quality);
        api.submitAnswer(request).enqueue(new Callback<AnswerResponse>() {
            @Override
            public void onResponse(Call<AnswerResponse> call, Response<AnswerResponse> response) {
                int next = currentIndex + 1;
                if (next < wordList.size()) {
                    showWord(next);
                } else {
                    progressBar.setProgress(wordList.size());
                    endStudySession();
                }
            }

            @Override
            public void onFailure(Call<AnswerResponse> call, Throwable t) {
                layoutDifficulty.setVisibility(View.VISIBLE);
                Toast.makeText(ReviewActivity.this,
                        "Hata: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startStudySession() {
        Map<String, Integer> body =new HashMap<>();
        body.put("user_id", userId);
        api.startSession(body).enqueue(new Callback<SessionStartResponse>() {
            @Override
            public void onResponse(Call<SessionStartResponse> call, Response<SessionStartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getSessionId();
                }
            }

            @Override
            public void onFailure(Call<SessionStartResponse> call, Throwable t) {
            }
        });
    }

    private void endStudySession() {
        if (sessionId== -1) {
            showResultDialog();
            return;
        }
        api.endSession(sessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showResultDialog();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showResultDialog();
            }
        });
    }

    private void showResultDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tekrar Tamamlandı")
                .setMessage("Tekrar tamamlandı! " + correctCount + " doğru, " + wrongCount + " yanlış")
                .setPositiveButton("Ana Sayfaya Dön", (dialog, which) -> navigateHome())
                .setCancelable(false)
                .show();
    }

    private void navigateHome() {
        Intent intent=new Intent(ReviewActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}