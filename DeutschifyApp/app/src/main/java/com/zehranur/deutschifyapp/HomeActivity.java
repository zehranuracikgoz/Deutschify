package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.viewmodel.HomeViewModel;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        homeViewModel.getWordOfDay().observe(this, word -> {
            TextView tvWord = findViewById(R.id.tv_word_of_day);
            if (tvWord != null) tvWord.setText(word);
        });

        homeViewModel.getWordOfDayTranslation().observe(this, translation -> {
            TextView tvTranslation = findViewById(R.id.tv_word_translation);
            if (tvTranslation != null) tvTranslation.setText(translation);
        });

        homeViewModel.getWeeklySessionCounts().observe(this, counts -> {
            int[] barIds = {
                R.id.bar1, R.id.bar2, R.id.bar3,
                R.id.bar4, R.id.bar5, R.id.bar6, R.id.bar7
            };
            int maxCount = 1;
            for (int c : counts) if (c > maxCount) maxCount = c;
            int maxHeightDp = 60;
            float density = getResources().getDisplayMetrics().density;
            for (int i = 0; i < barIds.length; i++) {
                View bar = findViewById(barIds[i]);
                if (bar != null) {
                    int heightPx = (int)((counts[i] / (float) maxCount) * maxHeightDp * density);
                    if (heightPx < (int)(4 * density)) heightPx = (int)(4 * density);
                    bar.getLayoutParams().height = heightPx;
                    bar.requestLayout();
                }
            }
        });

        homeViewModel.loadHomeData(token);

        findViewById(R.id.btn_kelime_kartlari).setOnClickListener(v ->
                startActivity(new Intent(this, WordCardActivity.class)));

        findViewById(R.id.btn_dil_bilgisi).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_artikeller).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_tekrar).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.nav_stats).setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.nav_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
        });
    }
}