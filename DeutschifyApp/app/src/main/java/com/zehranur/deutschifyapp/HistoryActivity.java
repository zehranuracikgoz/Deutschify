package com.zehranur.deutschifyapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.zehranur.deutschifyapp.model.HistoryResponse;
import com.zehranur.deutschifyapp.viewmodel.HistoryViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

            float d = getResources().getDisplayMetrics().density;
            int dp4  = (int)(4  * d);
            int dp5  = (int)(5  * d);
            int dp8  = (int)(8  * d);
            int dp10 = (int)(10 * d);
            int dp12 = (int)(12 * d);
            int dp16 = (int)(16 * d);
            int dp20 = (int)(20 * d);

            Map<String, List<HistoryResponse.Session>> groups = new LinkedHashMap<>();
            for (HistoryResponse.Session s :sessions) {
                String date = s.getDate() != null ? s.getDate() : "";
                if (!groups.containsKey(date)) groups.put(date, new ArrayList<>());
                groups.get(date).add(s);
            }

            boolean firstGroup = true;
            for (Map.Entry<String, List<HistoryResponse.Session>> entry : groups.entrySet()) {

                TextView tvDay = new TextView(this);
                tvDay.setText(formatDate(entry.getKey()));
                tvDay.setTextSize(11);
                tvDay.setTextColor(0xFF8899cc);
                tvDay.setTypeface(null, Typeface.BOLD);
                tvDay.setAllCaps(true);
                LinearLayout.LayoutParams dayParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                dayParams.setMargins(0, firstGroup ? dp4 : dp20, 0, dp8);
                tvDay.setLayoutParams(dayParams);
                container.addView(tvDay);
                firstGroup =false;

                for (HistoryResponse.Session session : entry.getValue()) {

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setColor(0xFFFFFFFF);
                    cardBg.setCornerRadius(12 * d);
                    card.setBackground(cardBg);
                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    cardParams.setMargins(0, 0, 0, dp8);
                    card.setLayoutParams(cardParams);
                    card.setPadding(dp16, dp12, dp16, dp12);

                    LinearLayout row=new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

                    String moduleType = session.getModuleType();
                    int[] bc = getBadgeColors(moduleType);

                    TextView tvBadge = new TextView(this);
                    tvBadge.setText(getBadgeLabel(moduleType));
                    tvBadge.setTextSize(11);
                    tvBadge.setTextColor(bc[1]);
                    tvBadge.setTypeface(null, Typeface.BOLD);
                    tvBadge.setPadding(dp12, dp4, dp12, dp4);
                    GradientDrawable badgeBg= new GradientDrawable();
                    badgeBg.setColor(bc[0]);
                    badgeBg.setCornerRadius(6 * d);
                    tvBadge.setBackground(badgeBg);

                    TextView tvTime = new TextView(this);
                    tvTime.setText(session.getTime() != null ? session.getTime() : "");
                    tvTime.setTextSize(12);
                    tvTime.setTextColor(0xFF8899cc);
                    LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    timeParams.setMarginStart(dp10);
                    tvTime.setLayoutParams(timeParams);

                    TextView tvXp = new TextView(this);
                    tvXp.setText("+" + session.getXpEarned() + " XP");
                    tvXp.setTextSize(12);
                    tvXp.setTextColor(0xFF7C5CBF);
                    tvXp.setTypeface(null, Typeface.BOLD);

                    row.addView(tvBadge);
                    row.addView(tvTime);
                    row.addView(tvXp);
                    card.addView(row) ;

                    boolean hasData = session.getCorrect() + session.getWrong() > 0;
                    if (!"flashcard".equals(moduleType) && hasData) {
                        int rate=session.getSuccessRate() != null ? session.getSuccessRate() : 0;

                        FrameLayout barOuter = new FrameLayout(this);
                        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dp5);
                        barParams.setMargins(0,  dp10, 0, 0);
                        barOuter.setLayoutParams(barParams);
                        GradientDrawable barBg=new GradientDrawable();
                        barBg.setColor(0xFFe8e4f7);
                        barBg.setCornerRadius(3 * d);
                        barOuter.setBackground(barBg);

                        View barFill = new View(this);
                        GradientDrawable fillBg = new GradientDrawable();
                        fillBg.setColor(0xFF7C5CBF) ;
                        fillBg.setCornerRadius(3 * d);
                        barFill.setBackground(fillBg);
                        barFill.setLayoutParams(new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT));
                        barOuter.addView(barFill);

                        barOuter.post(() -> {
                            int w = barOuter.getWidth();
                            if (w > 0) {
                                FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
                                        (int)(w * rate / 100.0),
                                        FrameLayout.LayoutParams.MATCH_PARENT);
                                barFill.setLayoutParams(fp);
                            }
                        });
                        card.addView(barOuter);

                        TextView tvRate = new TextView(this);
                        tvRate.setText(rate + "% başarı — " + session.getCorrect() + " doğru, " + session.getWrong() + " yanlış");
                        tvRate.setTextSize(11);
                        tvRate.setTextColor(0xFF8899cc);
                        LinearLayout.LayoutParams rateParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        rateParams.setMargins(0, dp5, 0, 0) ;
                        tvRate.setLayoutParams(rateParams);
                        card.addView(tvRate);
                    }

                    container.addView(card);
                }
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
        findViewById(R.id.nav_history).setOnClickListener(v -> {});
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        historyViewModel.loadHistory(token);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date parsed = sdf.parse(dateStr);
            String today = sdf.format(new Date());
            String yesterday = sdf.format(new Date(System.currentTimeMillis() - 86400000L));
            if (dateStr.equals(today)) return "Bugün";
            if (dateStr.equals(yesterday)) return "Dün";
            String[] months = {"Ocak","Şubat","Mart","Nisan","Mayıs","Haziran",
                               "Temmuz","Ağustos","Eylül","Ekim","Kasım","Aralık"};
            Calendar cal = Calendar.getInstance();
            cal.setTime(parsed);
            return cal.get(Calendar.DAY_OF_MONTH)+ " " + months[cal.get(Calendar.MONTH)];
        } catch (Exception e) {
            return dateStr;
        }
    }

    private int[] getBadgeColors(String moduleType) {
        if ("artikel".equals(moduleType)) return new int[]{0xFFdbeafe , 0xFF1e40af};
        if ("review".equals(moduleType))  return new int[]{0xFFfef3c7, 0xFF92400e};
        if ("grammar".equals(moduleType)) return new int[]{0xFFd1fae5, 0xFF065f46};
        return new int[]{0xFFede9ff, 0xFF2D1B69};
    }

    private String getBadgeLabel(String moduleType) {
        if ("artikel".equals(moduleType)) return "ARTIKEL" ;
        if ("review".equals(moduleType))  return "TEKRAR";
        if ("grammar".equals(moduleType)) return "DİL BİLGİSİ";
        return "FLASHCARD";
    }
}