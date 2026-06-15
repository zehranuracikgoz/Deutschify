package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.model.HistoryResponse;
import com.zehranur.deutschifyapp.viewmodel.HistoryViewModel;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private HistoryViewModel historyViewModel;

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

        setContentView(R.layout.activity_history);

        historyViewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        historyViewModel.getSessions().observe(this, sessions -> {
            LinearLayout container = findViewById(R.id.container_sessions);
            TextView tvEmpty = findViewById(R.id.tv_empty);
            container.removeAllViews();

            if (sessions == null || sessions.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            tvEmpty.setVisibility(View.GONE);

            float density = getResources().getDisplayMetrics().density;
            int dp16 = (int)(16 * density);
            int dp12 = (int)(12 * density);
            int dp8  = (int)(8  * density);

            for (HistoryResponse.Session session : sessions) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(getResources().getDrawable(R.drawable.bg_card_light, getTheme()));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, dp8);
                card.setLayoutParams(cardParams);
                card.setPadding(dp16, dp12, dp16, dp12);

                TextView tvDate = new TextView(this);
                tvDate.setText(session.getDate());
                tvDate.setTextColor(0xFFFFFFFF);
                tvDate.setTextSize(14);
                tvDate.setTypeface(null, Typeface.BOLD);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, dp8, 0, 0);
                row.setLayoutParams(rowParams);

                TextView tvCorrect = new TextView(this);
                tvCorrect.setText("Doğru: " + session.getCorrect());
                tvCorrect.setTextColor(0xFF4CAF50);
                tvCorrect.setTextSize(13);

                TextView tvWrong = new TextView(this);
                tvWrong.setText("   Yanlış: " + session.getWrong());
                tvWrong.setTextColor(0xFFC0392B);
                tvWrong.setTextSize(13);

                row.addView(tvCorrect);
                row.addView(tvWrong);
                card.addView(tvDate);
                card.addView(row);
                container.addView(card);
            }
        });

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.nav_stats).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        findViewById(R.id.nav_history).setOnClickListener(v -> {
        });
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        historyViewModel.loadHistory(token);
    }
}