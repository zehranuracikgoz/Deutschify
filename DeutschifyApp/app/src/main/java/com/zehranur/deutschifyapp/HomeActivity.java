package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

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

        findViewById(R.id.btn_kelime_kartlari).setOnClickListener(v ->
                startActivity(new Intent(this, WordCardActivity.class)));

        findViewById(R.id.btn_dil_bilgisi).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_artikeller).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_tekrar).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.nav_stats).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.nav_history).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());
        findViewById(R.id.nav_profile).setOnClickListener(v ->
                Toast.makeText(this, "Yakında!", Toast.LENGTH_SHORT).show());
    }
}