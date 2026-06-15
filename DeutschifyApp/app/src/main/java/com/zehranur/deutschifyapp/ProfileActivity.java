package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.viewmodel.ProfileViewModel;

public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel profileViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");

        if (token == null || token.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        profileViewModel.getUsername().observe(this, name -> {
            TextView tv = findViewById(R.id.tv_username);
            if (tv != null) tv.setText(name);
        });

        profileViewModel.getEmail().observe(this, email -> {
            TextView tv = findViewById(R.id.tv_email);
            if (tv != null) tv.setText(email);
        });

        profileViewModel.getTotalXp().observe(this, xp -> {
            TextView tv = findViewById(R.id.tv_total_xp);
            if (tv != null) tv.setText(xp + " XP");
        });

        profileViewModel.getDailyStreak().observe(this, streak -> {
            TextView tv = findViewById(R.id.tv_daily_streak);
            if (tv != null) tv.setText(streak + " gün");
        });

        profileViewModel.getLevelName().observe(this, level -> {
            TextView tv = findViewById(R.id.tv_level);
            if (tv != null) tv.setText(level != null ? level : "A1");
        });

        Button btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }

        findViewById(R.id.nav_home).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        findViewById(R.id.nav_stats).setOnClickListener(v ->
            startActivity(new Intent(this, DashboardActivity.class))
        );
        findViewById(R.id.nav_history).setOnClickListener(v ->
            startActivity(new Intent(this, HistoryActivity.class))
        );
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
        });

        profileViewModel.loadProfile(token);
    }
}