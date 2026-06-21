package com.zehranur.deutschifyapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import okhttp3.ResponseBody;
import com.zehranur.deutschifyapp.model.AnswerRequest;
import com.zehranur.deutschifyapp.model.AnswerResponse;
import com.zehranur.deutschifyapp.model.ExampleRequest;
import com.zehranur.deutschifyapp.model.ExampleResponse;
import com.zehranur.deutschifyapp.model.QueueResponse;
import com.zehranur.deutschifyapp.model.SessionStartResponse;
import com.zehranur.deutschifyapp.model.Word;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private int sessionId = -1;
    private boolean isFlipped = false;
    private boolean isAnimating = false;
    private MediaPlayer mediaPlayer;
    private int totalXpEarned =0;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_card);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userId = prefs.getInt("user_id", 1);
        token = prefs.getString("access_token", null);

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

        findViewById(R.id.btn_back).setOnClickListener(v -> showExitDialog());
        cardContainer.setOnClickListener(v -> flipCard());
        findViewById(R.id.btn_telaffuz).setOnClickListener(v -> {
            if (wordQueue != null && currentIndex < wordQueue.size()) {
                playWordAudio(wordQueue.get(currentIndex).getGermanWord());
            }
        });
        findViewById(R.id.btn_yeni_cumle).setOnClickListener(v -> generateNewExample());

        findViewById(R.id.btn_quality_1).setOnClickListener(v -> submitAnswer(1));
        findViewById(R.id.btn_quality_2).setOnClickListener(v -> submitAnswer(2));
        findViewById(R.id.btn_quality_3).setOnClickListener(v -> submitAnswer(3));
        findViewById(R.id.btn_quality_4).setOnClickListener(v -> submitAnswer(4));
        findViewById(R.id.btn_quality_5).setOnClickListener(v -> submitAnswer(5));

        loadStudyQueue();
    }

    private void loadStudyQueue() {
        api.getStudyQueue("Bearer " + token, userId, SESSION_LIMIT).enqueue(new Callback<QueueResponse>() {
            @Override
            public void onResponse(Call<QueueResponse> call, Response<QueueResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    wordQueue = response.body().getQueue();
                    if (wordQueue != null && !wordQueue.isEmpty()) {
                        progressBar.setMax(wordQueue.size());
                        startStudySession(() -> showWord(0));
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

    private void startStudySession(Runnable onReady) {
        Map<String, Integer> body = new HashMap<>();
        body.put("user_id", userId);
        api.startSession("Bearer " + token, body).enqueue(new Callback<SessionStartResponse>() {
            @Override
            public void onResponse(Call<SessionStartResponse> call, Response<SessionStartResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getSessionId();
                }
                runOnUiThread(onReady::run);
            }

            @Override
            public void onFailure(Call<SessionStartResponse> call, Throwable t) {
                runOnUiThread(onReady::run);
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

    private void generateNewExample() {
        if (wordQueue == null || currentIndex >= wordQueue.size()) return;
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        if (token == null) {
            Toast.makeText(this, "Oturum süresi dolmuş", Toast.LENGTH_SHORT).show();
            return;
        }
        String germanWord = wordQueue.get(currentIndex).getGermanWord();
        api.getExampleSentence("Bearer " + token, new ExampleRequest(germanWord))
                .enqueue(new Callback<ExampleResponse>() {
            @Override
            public void onResponse(Call<ExampleResponse>call, Response<ExampleResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getExampleSentence() != null) {
                    tvExampleDe.setText(response.body().getExampleSentence());
                    tvExampleTr.setText("(çeviri mevcut değil)");
                } else {
                    Toast.makeText(WordCardActivity.this,
                            "Örnek cümle üretilemedi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExampleResponse>call, Throwable t) {
                Toast.makeText(WordCardActivity.this,
                        "Bağlantı hatası: "+t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAnswer(int quality) {
        layoutDifficulty.setVisibility(View.INVISIBLE);

        AnswerRequest request = sessionId != -1
                ? new AnswerRequest(userId, wordQueue.get(currentIndex).getWordId(), quality, sessionId)
                : new AnswerRequest(userId, wordQueue.get(currentIndex).getWordId(), quality);

        api.submitAnswer("Bearer " + token, request).enqueue(new Callback<AnswerResponse>() {
                    @Override
                    public void onResponse(Call<AnswerResponse> call, Response<AnswerResponse> response) {
                        if (response.isSuccessful()&&response.body() != null) {
                            totalXpEarned += response.body().getXpEarned();
                        }
                        int next = currentIndex + 1;
                        if (next < wordQueue.size()) {
                            showWord(next);
                        } else {
                            progressBar.setProgress(wordQueue.size());
                            showSessionComplete();
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

    private void endStudySession() {
        if (sessionId == -1) {
            navigateHome();
            return;
        }
        api.endSession("Bearer " + token, sessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                navigateHome();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                navigateHome();
            }
        });
    }

    private void playWordAudio(String word) {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        if (token == null) {
            Toast.makeText(this, "Ses yüklenemedi", Toast.LENGTH_SHORT).show();
            return;
        }
        api.getWordAudio(word, "Bearer " + token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(WordCardActivity.this, "Ses yüklenemedi", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    File tempFile = File.createTempFile("tts_", ".mp3", getCacheDir());
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        fos.write(response.body().bytes());
                    }
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mediaPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        mediaPlayer = null;
                        tempFile.delete();
                    });
                } catch (IOException e) {
                    Toast.makeText(WordCardActivity.this, "Ses yüklenemedi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(WordCardActivity.this, "Ses yüklenemedi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSessionComplete() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_session_complete, null);
        ((TextView) dialogView.findViewById(R.id.tv_summary_cards)).setText(String.valueOf(wordQueue.size()));
        ((TextView) dialogView.findViewById(R.id.tv_summary_xp)).setText("+" +totalXpEarned + " XP");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btn_summary_home).setOnClickListener(v -> {
            dialog.dismiss();
            endStudySession();
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Oturumdan çıkmak istediğinize emin misiniz?")
                .setPositiveButton("Evet",(dialog, which) ->endStudySession())
                .setNegativeButton("Hayır", (dialog, which) -> dialog.dismiss())
                .show();
    }
    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void navigateHome() {
        Intent intent = new Intent(WordCardActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}