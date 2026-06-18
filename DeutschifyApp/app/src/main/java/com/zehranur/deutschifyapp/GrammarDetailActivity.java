package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.zehranur.deutschifyapp.model.GrammarCheckRequest;
import com.zehranur.deutschifyapp.model.GrammarCheckResponse;
import com.zehranur.deutschifyapp.model.GrammarExercise;
import com.zehranur.deutschifyapp.model.GrammarTopicDetail;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GrammarDetailActivity extends AppCompatActivity {

    private ApiService api;
    private int userId;

    private WebView wvExplanation;
    private TextView tvTopicTitle;
    private TextView tvCounter;
    private ProgressBar progressBar;

    private LinearLayout layoutExplain;
    private View svExercise;
    private TextView tvQuestion;
    private LinearLayout layoutFill;
    private EditText etAnswer;
    private LinearLayout layoutChoices;
    private TextView tvFeedback;
    private TextView btnCheck;
    private TextView btnNext;
    private LinearLayout layoutSummary;
    private TextView tvCorrectCount;
    private TextView tvWrongCount;
    private TextView tvXpEarned;

    private List<GrammarExercise> exercises;
    private int currentIndex = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int xpEarned = 0;
    private boolean answered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grammar_detail);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        userId = prefs.getInt("user_id", 0);

        api = RetrofitClient.getInstance().create(ApiService.class);

        wvExplanation =findViewById(R.id.wv_explanation);
        tvTopicTitle = findViewById(R.id.tv_topic_title);
        tvCounter = findViewById(R.id.tv_counter);
        progressBar = findViewById(R.id.progress_bar);
        layoutExplain = findViewById(R.id.layout_explain);
        svExercise = findViewById(R.id.sv_exercise);
        tvQuestion = findViewById(R.id.tv_question);
        layoutFill = findViewById(R.id.layout_fill);
        etAnswer = findViewById(R.id.et_answer);
        layoutChoices = findViewById(R.id.layout_choices);
        tvFeedback = findViewById(R.id.tv_feedback);
        btnCheck = findViewById(R.id.btn_check);
        btnNext = findViewById(R.id.btn_next);
        layoutSummary = findViewById(R.id.layout_summary);
        tvCorrectCount = findViewById(R.id.tv_correct_count);
        tvWrongCount = findViewById(R.id.tv_wrong_count);
        tvXpEarned = findViewById(R.id.tv_xp_earned);

        String slug = getIntent().getStringExtra("slug");
        String title = getIntent().getStringExtra("title");
        if (title != null) tvTopicTitle.setText(title);

        setupWebView();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_start_exercises).setOnClickListener(v -> startExercises());
        btnCheck.setOnClickListener(v -> submitFillAnswer());
        btnNext.setOnClickListener(v -> nextQuestion());
        findViewById(R.id.btn_finish).setOnClickListener(v -> finish());

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

        if (slug != null) loadTopicDetail(slug);
    }

    private void setupWebView() {
        WebSettings settings = wvExplanation.getSettings();
        settings.setJavaScriptEnabled(false);
        settings.setDefaultTextEncodingName("UTF-8");
        wvExplanation.setBackgroundColor(Color.TRANSPARENT);
    }

    private void loadTopicDetail(String slug) {
        api.getGrammarTopicDetail(slug).enqueue(new Callback<GrammarTopicDetail>() {
            @Override
            public void onResponse(Call<GrammarTopicDetail> call, Response<GrammarTopicDetail> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GrammarTopicDetail detail = response.body();
                    exercises = detail.getExercises();
                    String explanation = detail.getExplanation();
                    if (explanation == null) explanation = "";
                    loadExplanationHtml(explanation);
                } else {
                    Toast.makeText(GrammarDetailActivity.this, "Konu yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GrammarTopicDetail> call, Throwable t) {
                Toast.makeText(GrammarDetailActivity.this, "Bağlantı hatası.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadExplanationHtml(String content) {
        String html = "<html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "body{font-family:sans-serif;font-size:15px;color:#1a2d6e;padding:0;margin:0;background:#ffffff}"
                + "table{width:100%;border-collapse:collapse;margin:10px 0;font-size:14px}"
                + "th{background:#B794D6;color:#fff;padding:8px;text-align:left}"
                + "td{border:1px solid  #ddd;padding:8px;color:#1a2d6e}"
                + "p{margin-bottom:10px}"
                + "strong,b{color:#B794D6}"
                + "tr:nth-child(even) td{background:#f8f5ff}"
                + "</style></head><body>" + content + "</body></html>";
        wvExplanation.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void startExercises() {
        if (exercises == null || exercises.isEmpty()) {
            Toast.makeText(this, "Bu konuda henüz alıştırma yok.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentIndex =0;
        correctCount = 0;
        wrongCount = 0;
        xpEarned = 0;

        layoutExplain.setVisibility(View.GONE);
        svExercise.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        tvCounter.setVisibility(View.VISIBLE);

        showExercise();
    }
    private void showExercise() {
        GrammarExercise ex = exercises.get(currentIndex);
        answered = false;

        int total = exercises.size();
        tvCounter.setText("SORU " + (currentIndex + 1) + "/" + total);
        progressBar.setMax(total);
        progressBar.setProgress(currentIndex);

        tvQuestion.setText(ex.getQuestion());
        tvFeedback.setVisibility(View.GONE);
        btnCheck.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        layoutFill.setVisibility(View.GONE);
        layoutChoices.setVisibility(View.GONE);
        layoutChoices.removeAllViews();
        etAnswer.setText("");
        etAnswer.setEnabled(true);

        if ("multiple_choice".equals(ex.getExerciseType()) && ex.getOptions() != null) {
            layoutChoices.setVisibility(View.VISIBLE);
            buildChoiceButtons(ex);
        } else{
            layoutFill.setVisibility(View.VISIBLE);
            btnCheck.setVisibility(View.VISIBLE);
            etAnswer.setOnEditorActionListener((v, actionId, event) -> {
                submitFillAnswer();
                return true;
            });
        }
    }

    private void buildChoiceButtons(GrammarExercise ex) {
        float density = getResources().getDisplayMetrics().density;
        java.util.List<String> options = ex.getOptions();

        for (int i = 0; i < options.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = (int)(8 * density);
            row.setLayoutParams(rowLp);

            for (int j = i; j < Math.min(i + 2, options.size()); j++) {
                final String option = options.get(j);
                TextView btn = new TextView(this);
                btn.setText(option);
                btn.setTextSize(13f);
                btn.setTextColor(Color.parseColor("#1a2d6e"));
                btn.setTypeface(null, android.graphics.Typeface.BOLD);
                btn.setGravity(android.view.Gravity.CENTER);
                btn.setBackground(getDrawable(R.drawable.bg_grammar_choice));
                btn.setClickable(true);
                btn.setFocusable(true);
                btn.setPadding((int)(10 * density), 0, (int)(10 * density), 0);
                LinearLayout.LayoutParams btnLp =new LinearLayout.LayoutParams(
                        0, (int)(52 * density), 1f);
                if (j % 2 == 0 && j + 1 < options.size())  {
                    btnLp.rightMargin = (int)(8 * density);
                }
                btn.setLayoutParams(btnLp);
                btn.setOnClickListener(v -> {
                    if (!answered) submitChoiceAnswer(option, btn);
                });
                row.addView(btn);
            }
            layoutChoices.addView(row);
        }
    }

    private void submitFillAnswer() {
        if (answered) return;
        String answer = etAnswer.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Bir cevap girin.", Toast.LENGTH_SHORT).show();
            return;
        }
        etAnswer.setEnabled(false);
        btnCheck.setVisibility(View.GONE);
        checkAnswer(answer, null);
    }

    private void submitChoiceAnswer(String answer, TextView clickedBtn) {
        if (answered) return;
        disableChoiceButtons() ;
        checkAnswer(answer, clickedBtn);
    }

    private void disableChoiceButtons() {
        for (int i = 0; i < layoutChoices.getChildCount(); i++) {
            View rowView = layoutChoices.getChildAt(i);
            if (rowView instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    row.getChildAt(j).setClickable(false);
                    row.getChildAt(j).setFocusable(false);
                }
            }
        }
    }

    private void checkAnswer(String answer, TextView clickedBtn) {
        answered = true;
        GrammarExercise ex=exercises.get(currentIndex);
        Integer uid = userId > 0 ? userId : null;
        GrammarCheckRequest request = new GrammarCheckRequest(ex.getId(), answer, uid);

        api.checkGrammarAnswer(request).enqueue(new Callback<GrammarCheckResponse>() {
            @Override
            public void onResponse(Call<GrammarCheckResponse> call, Response<GrammarCheckResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleCheckResult(response.body(), clickedBtn);
                } else {
                    answered = false;
                    etAnswer.setEnabled(true);
                    btnCheck.setVisibility(View.VISIBLE);
                    Toast.makeText(GrammarDetailActivity.this, "Sunucu hatası.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GrammarCheckResponse> call, Throwable t) {
                answered = false;
                etAnswer.setEnabled(true);
                btnCheck.setVisibility(View.VISIBLE);
                Toast.makeText(GrammarDetailActivity.this, "Bağlantı hatası.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCheckResult(GrammarCheckResponse result, TextView clickedBtn) {
        if (result.isCorrect()) {
            correctCount++;
            xpEarned += result.getXpEarned();
        } else {
            wrongCount++;
        }

        if (clickedBtn != null) {
            clickedBtn.setBackground(getDrawable(
                    result.isCorrect() ? R.drawable.bg_grammar_choice_correct : R.drawable.bg_grammar_choice_wrong));
            clickedBtn.setTextColor(result.isCorrect()
                    ? Color.parseColor("#2e7d32") : Color.parseColor("#c0392b"));
            if (!result.isCorrect()) {
                markCorrectChoice(result.getCorrectAnswer());
            }
        }

        String feedbackText = result.isCorrect()
                ? "Doğru!"
                : "Yanlış — Doğru cevap: " + result.getCorrectAnswer();
        if (result.getExplanation() != null && !result.getExplanation().isEmpty()) {
            feedbackText += "\n\n" + result.getExplanation();
        }
        tvFeedback.setText(feedbackText);
        tvFeedback.setTextColor(result.isCorrect()
                ? Color.parseColor("#2e7d32 " ) : Color.parseColor("#C0392B"));
        tvFeedback.setBackgroundResource(result.isCorrect()
                ? R.drawable.bg_feedback_correct : R.drawable.bg_feedback_wrong);
        tvFeedback.setVisibility(View.VISIBLE);

        boolean isLast = (currentIndex + 1) >= exercises.size();
        btnNext.setText(isLast ? "Tamamla" : "Sonraki Soru");
        btnNext.setVisibility(View.VISIBLE);
    }

    private void markCorrectChoice(String correctAnswer) {
        for (int i = 0; i <layoutChoices.getChildCount(); i++) {
            View rowView = layoutChoices.getChildAt(i);
            if (rowView instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) rowView;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View child = row.getChildAt(j);
                    if (child instanceof TextView) {
                        TextView btn = (TextView) child;
                        if (correctAnswer != null && correctAnswer.equalsIgnoreCase(btn.getText().toString())) {
                            btn.setBackground(getDrawable(R.drawable.bg_grammar_choice_correct));
                            btn.setTextColor(Color.parseColor("#2e7d32"));
                        }
                    }
                }
            }
        }
    }
    private void nextQuestion() {
        currentIndex++;
        if (currentIndex >= exercises.size()) {
            showSummary();
        } else {
            showExercise();
        }
    }
    private void showSummary() {
        progressBar.setProgress(exercises.size());
        svExercise.setVisibility(View.GONE);
        tvCorrectCount.setText(String.valueOf(correctCount));
        tvWrongCount.setText(String.valueOf(wrongCount));
        tvXpEarned.setText(String.valueOf(xpEarned));
        layoutSummary.setVisibility(View.VISIBLE);
    }
}