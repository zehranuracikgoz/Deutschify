package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.viewmodel.DashboardViewModel;

public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel dashboardViewModel;

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

        setContentView(R.layout.activity_dashboard);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        dashboardViewModel.getTotalXp().observe(this, xp -> {
            TextView tv = findViewById(R.id.tv_total_xp);
            if (tv != null) tv.setText(xp + " XP");
        });

        dashboardViewModel.getDailyStreak().observe(this, streak -> {
            TextView tv = findViewById(R.id.tv_daily_streak);
            if (tv != null) tv.setText(streak + " gün");
        });

        dashboardViewModel.getSuccessRate().observe(this, rate -> {
            TextView tv = findViewById(R.id.tv_success_rate);
            if (tv != null) tv.setText("%" + rate);
        });

        dashboardViewModel.getTotalCorrect().observe(this, correct -> {
            TextView tv = findViewById(R.id.tv_total_correct);
            if (tv != null) tv.setText(String.valueOf(correct));
        });

        dashboardViewModel.getTotalWrong().observe(this, wrong -> {
            TextView tv = findViewById(R.id.tv_total_wrong);
            if (tv != null) tv.setText(String.valueOf(wrong));
        });

        dashboardViewModel.getWeeklySessions().observe(this, counts -> {
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

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.nav_stats).setOnClickListener(v -> {
        });
        findViewById(R.id.nav_history).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        dashboardViewModel.loadStats(token);
    }
}