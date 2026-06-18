package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.zehranur.deutschifyapp.model.GrammarTopic;
import com.zehranur.deutschifyapp.network.ApiService;
import com.zehranur.deutschifyapp.network.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GrammarActivity extends AppCompatActivity {

    private LinearLayout topicListContainer;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        if (prefs.getString("access_token", null) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_grammar);

        topicListContainer=findViewById(R.id.topic_list_container);
        api = RetrofitClient.getInstance().create(ApiService.class);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

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

        loadTopics();
    }

    private void loadTopics() {
        api.getGrammarTopics().enqueue(new Callback<List<GrammarTopic>>() {
            @Override
            public void onResponse(Call<List<GrammarTopic>> call, Response<List<GrammarTopic>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderTopics(response.body());
                } else {
                    Toast.makeText(GrammarActivity.this, "Konular yüklenemedi.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<GrammarTopic>> call, Throwable t) {
                Toast.makeText(GrammarActivity.this, "Bağlantı hatası.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderTopics(List<GrammarTopic> topics) {
        List<GrammarTopic> a1 = new ArrayList<>();
        List<GrammarTopic> a2 = new ArrayList<>();
        for (GrammarTopic t : topics) {
            if ("A1".equals(t.getLevel())) a1.add(t);
            else if ("A2".equals(t.getLevel())) a2.add(t);
        }
        if (!a1.isEmpty()) {
            addSectionHeader("A1 — Başlangıç");
            for (GrammarTopic t : a1) addTopicCard(t);
        }
        if (!a2.isEmpty()) {
            addSectionHeader("A2 — Temel");
            for (GrammarTopic t : a2) addTopicCard(t);
        }
    }

    private void addSectionHeader(String label) {
        float density=getResources().getDisplayMetrics().density;
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(11f);
        tv.setTextColor(Color.parseColor("#8899cc"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setAllCaps(true);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int)(24 * density);
        lp.bottomMargin = (int)(10 * density);
        tv.setLayoutParams(lp);
        topicListContainer.addView(tv);
    }

    private void addTopicCard(GrammarTopic topic) {
        float density = getResources().getDisplayMetrics().density;
        int dp2 =(int)(2 * density);
        int dp4 = (int)(4 * density);
        int dp8 = (int)(8 * density);
        int dp10 = (int)(10 * density);
        int dp14 = (int)(14 * density);
        int dp22 = (int)(22 * density);
        int dp40 = (int)(40 * density);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(getDrawable(R.drawable.bg_card_white));
        card.setPadding(dp14, dp14, dp14, dp14);
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp8;
        card.setLayoutParams(cardLp);

        LinearLayout iconArea = new LinearLayout(this);
        iconArea.setGravity(Gravity.CENTER);
        iconArea.setBackground(getDrawable(R.drawable.bg_grammar_icon));
        LinearLayout.LayoutParams iconAreaLp = new LinearLayout.LayoutParams(dp40, dp40);
        iconAreaLp.rightMargin = dp14;
        iconArea.setLayoutParams(iconAreaLp);

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_book);
        iconView.setColorFilter(Color.WHITE);
        iconView.setLayoutParams(new LinearLayout.LayoutParams(dp22, dp22));
        iconArea.addView(iconView);

        LinearLayout infoArea = new LinearLayout(this);
        infoArea.setOrientation(LinearLayout.VERTICAL);
        infoArea.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(topic.getTitle());
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(Color.parseColor("#1a2d6e"));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp4;
        tvTitle.setLayoutParams(titleLp);

        TextView tvLevel = new TextView(this);
        tvLevel.setText(topic.getLevel());
        tvLevel.setTextSize(10f);
        tvLevel.setTextColor(Color.parseColor("#2D1B69 "));
        tvLevel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLevel.setBackground(getDrawable(R.drawable.bg_level_badge));
        tvLevel.setPadding(dp8, dp2, dp8, dp2);

        infoArea.addView(tvTitle);
        infoArea.addView(tvLevel);

        TextView tvArrow = new TextView(this);
        tvArrow.setText("›");
        tvArrow.setTextSize(20f);
        tvArrow.setTextColor(Color.parseColor("#8899cc"));
        LinearLayout.LayoutParams arrowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        arrowLp.leftMargin = dp8;
        tvArrow.setLayoutParams(arrowLp);

        card.addView(iconArea );
        card.addView(infoArea);
        card.addView(tvArrow);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, GrammarDetailActivity.class);
            intent.putExtra("slug", topic.getSlug());
            intent.putExtra("title" , topic.getTitle());
            startActivity(intent);
        });

        topicListContainer.addView(card);
    }
}