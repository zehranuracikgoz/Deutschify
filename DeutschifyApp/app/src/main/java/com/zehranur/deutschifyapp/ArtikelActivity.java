package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.model.AiFeedbackRequest;
import com.zehranur.deutschifyapp.model.AiFeedbackResponse;
import com.zehranur.deutschifyapp.model.SessionStartResponse;
import com.zehranur.deutschifyapp.model.WordResponse;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import com.zehranur.deutschifyapp.viewmodel.ArtikelViewModel;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtikelActivity extends AppCompatActivity {

    private static final int SESSION_LIMIT = 5;
    private static final int XP_PER_CORRECT = 5;

    private ArtikelViewModel viewModel;
    private ApiService api;

    private LinearLayout cardArtikeli;
    private TextView tvGermanWord;
    private TextView tvTurkishMeaning;
    private TextView tvCounter;
    private ProgressBar progressBar;
    private LinearLayout layoutSummary;
    private TextView tvCorrectCount;
    private TextView tvWrongCount;
    private TextView tvXpEarned;

    private LinearLayout layoutAiFeedback;
    private TextView tvAiUserAnswer;
    private TextView tvAiCorrectAnswer;
    private TextView tvAiFeedbackLoading;
    private TextView tvAiFeedbackText;

    private boolean isAnswering = false;
    private boolean aiFeedbackPending = false;
    private boolean aiFeedbackShowing = false;
    private boolean isLastQuestion = false;
    private Call<AiFeedbackResponse> pendingFeedbackCall;
    private Runnable pendingCardResetRunnable;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int sessionId =-1;
    private int userId;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int answeredCount = 0;
    private int xpEarned = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artikel);

        cardArtikeli = findViewById(R.id.card_artikel);
        tvGermanWord = findViewById(R.id.tv_german_word);
        tvTurkishMeaning = findViewById(R.id.tv_turkish_meaning);
        tvCounter = findViewById(R.id.tv_art_counter);
        progressBar = findViewById(R.id.progress_bar);
        layoutSummary = findViewById(R.id.layout_summary);
        tvCorrectCount =findViewById(R.id.tv_correct_count);
        tvWrongCount =findViewById(R.id.tv_wrong_count);
        tvXpEarned = findViewById(R.id.tv_xp_earned);

        layoutAiFeedback = findViewById(R.id.layout_ai_feedback);
        tvAiUserAnswer = findViewById(R.id.tv_ai_user_answer);
        tvAiCorrectAnswer = findViewById(R.id.tv_ai_correct_answer);
        tvAiFeedbackLoading = findViewById(R.id.tv_ai_feedback_loading);
        tvAiFeedbackText = findViewById(R.id.tv_ai_feedback_text);

        viewModel = new ViewModelProvider(this).get(ArtikelViewModel.class);

        viewModel.getCurrentWord().observe(this, word -> {
            if (word != null) {
                tvGermanWord.setText(word.getGermanWord());
                tvTurkishMeaning.setText(word.getTurkishMeaning());
                tvCounter.setText("SORU " + (answeredCount + 1) + "/" +SESSION_LIMIT);
                progressBar.setProgress(answeredCount);
            }
        });

        viewModel.getFeedbackCorrect().observe(this, correct -> {
            if (correct != null) {
                cardArtikeli.setBackground(getDrawable(
                        correct ? R.drawable.bg_artikel_card_correct :R.drawable.bg_artikel_card_wrong));
            }
        });

        viewModel.getErrorMessage().observe(this, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());

        tvCounter.setText("SORU 1/" + SESSION_LIMIT);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_der).setOnClickListener(v -> handleAnswer(ArtikelViewModel.DER));
        findViewById(R.id.btn_die).setOnClickListener(v -> handleAnswer(ArtikelViewModel.DIE));
        findViewById(R.id.btn_das).setOnClickListener(v -> handleAnswer(ArtikelViewModel.DAS));
        findViewById(R.id.btn_finish).setOnClickListener(v -> navigateHome());

        findViewById(R.id.btn_ai_close).setOnClickListener(v -> dismissAiFeedback());
        findViewById(R.id.btn_ai_understood).setOnClickListener(v -> dismissAiFeedback());

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

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userId = prefs.getInt("user_id", 1);
        api = RetrofitClient.getInstance().create(ApiService.class);
        startStudySession();
        viewModel.loadWords();
    }

    private void handleAnswer(int articleId){
        if (isAnswering) return;
        isAnswering = true;
        isLastQuestion = false;
        viewModel.checkAnswer(articleId);
        boolean correct = Boolean.TRUE.equals(viewModel.getFeedbackCorrect().getValue());
        answeredCount++;

        if (correct) {
            correctCount++;
            xpEarned +=XP_PER_CORRECT;
        } else {
            wrongCount++;
            WordResponse cw = viewModel.getCurrentWord().getValue();
            if (cw != null) {
                String userArticle = articleIdToName(articleId);
                String correctArticle = articleIdToName(cw.getArticleId());
                isLastQuestion = answeredCount >= SESSION_LIMIT;
                showAiFeedbackModal(userArticle, correctArticle);
                requestAiFeedback(cw.getGermanWord(), userArticle, correctArticle);
            }
        }

        if (answeredCount >= SESSION_LIMIT) {
            if (correct) {
                handler.postDelayed(this::endStudySession, 1200);
            } else {
                pendingCardResetRunnable = () -> {
                    cardArtikeli.setBackground(getDrawable(R.drawable.bg_card_white));
                    pendingCardResetRunnable = null;
                };
                handler.postDelayed(pendingCardResetRunnable, 1200);
            }
        } else {
            if (correct) {
                handler.postDelayed(() -> {
                    viewModel.nextWord();
                    cardArtikeli.setBackground(getDrawable(R.drawable.bg_card_white));
                    isAnswering =false;
                }, 1200);
            } else{
                pendingCardResetRunnable = () -> {
                    cardArtikeli.setBackground(getDrawable(R.drawable.bg_card_white));
                    pendingCardResetRunnable = null;
                };
                handler.postDelayed(pendingCardResetRunnable, 1200);
            }
        }
    }
    private void showAiFeedbackModal(String userAnswer, String correctAnswer) {
        tvAiUserAnswer.setText(userAnswer);
        tvAiCorrectAnswer.setText(correctAnswer);
        tvAiFeedbackLoading.setVisibility(View.VISIBLE);
        tvAiFeedbackText.setVisibility(View.GONE);
        layoutAiFeedback.setVisibility(View.VISIBLE);
        aiFeedbackShowing =true;
    }

    private void dismissAiFeedback() {
        aiFeedbackShowing = false;
        aiFeedbackPending = false;
        layoutAiFeedback.setVisibility(View.GONE);
        if (pendingFeedbackCall != null) { pendingFeedbackCall.cancel(); pendingFeedbackCall = null; }
        if (pendingCardResetRunnable!= null) {
            handler.removeCallbacks(pendingCardResetRunnable);
            pendingCardResetRunnable = null;
        }
        cardArtikeli.setBackground(getDrawable(R.drawable.bg_card_white));
        isAnswering = false;
        boolean wasLast = isLastQuestion;
        isLastQuestion = false;
        if (wasLast){
            endStudySession();
        } else {
            viewModel.nextWord();
        }
    }

    private void startStudySession() {
        Map<String, Integer> body = new HashMap<>();
        body.put("user_id", userId);
        api.startSession(body).enqueue(new Callback<SessionStartResponse>() {
            @Override
            public void onResponse(Call<SessionStartResponse> call, Response<SessionStartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionId=response.body().getSessionId();
                }
            }

            @Override
            public void onFailure(Call<SessionStartResponse> call, Throwable t) {}
        });
    }
    private void endStudySession() {
        if (sessionId == -1) {
            showSummaryOverlay();
            return;
        }
        api.endSession(sessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                showSummaryOverlay();
            }

            @Override
            public void onFailure(Call<Void> call,  Throwable t) {
                showSummaryOverlay();
            }
        });
    }

    private void requestAiFeedback(String word, String userAnswer, String correctAnswer) {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        if (token ==null) {
            tvAiFeedbackLoading.setVisibility(View.GONE);
            tvAiFeedbackText.setText("Analiz için giriş yapmanız gerekiyor.");
            tvAiFeedbackText.setVisibility(View.VISIBLE);
            return;
        }
        aiFeedbackPending = true;
        if (pendingFeedbackCall != null) pendingFeedbackCall.cancel();
        pendingFeedbackCall = api.getAiFeedback("Bearer " + token,
                new AiFeedbackRequest(word, userAnswer, correctAnswer));
        pendingFeedbackCall.enqueue(new Callback<AiFeedbackResponse>() {
            @Override
            public void onResponse(Call<AiFeedbackResponse> call, Response<AiFeedbackResponse> response) {
                aiFeedbackPending = false;
                if (!aiFeedbackShowing) return;
                tvAiFeedbackLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getFeedback() != null) {
                    tvAiFeedbackText.setText(response.body().getFeedback());
                } else {
                    tvAiFeedbackText.setText("Analiz şu an alınamadı.");
                }
                tvAiFeedbackText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<AiFeedbackResponse> call, Throwable t) {
                aiFeedbackPending = false;
                if (!aiFeedbackShowing) return;
                tvAiFeedbackLoading.setVisibility(View.GONE);
                tvAiFeedbackText.setText("Analiz şu an alınamadı.");
                tvAiFeedbackText.setVisibility(View.VISIBLE);
            }
        });
    }

    private String articleIdToName(Integer id) {
        if (id == null) return "";
        switch (id) {
            case 1: return "DER";
            case 2: return "DIE";
            case 3: return "DAS";
            default: return "";
        }
    }

    private void showSummaryOverlay() {
        aiFeedbackShowing=false;
        aiFeedbackPending=false;
        if (pendingFeedbackCall != null) { pendingFeedbackCall.cancel(); pendingFeedbackCall = null; }
        layoutAiFeedback.setVisibility(View.GONE);
        tvCorrectCount.setText(String.valueOf(correctCount));
        tvWrongCount.setText(String.valueOf(wrongCount));
        tvXpEarned.setText(String.valueOf(xpEarned));
        layoutSummary.setVisibility(View.VISIBLE);
    }

    private void navigateHome() {
        startActivity(new Intent(ArtikelActivity.this, HomeActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (pendingFeedbackCall != null) { pendingFeedbackCall.cancel(); pendingFeedbackCall = null; }
    }
}