package com.lapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;

public class SettingsActivity extends Activity {
    
    private SharedPreferences prefs;
    private String currentTheme = "white";
    
    private int backgroundColor;
    private int cardColor;
    private int textColor;
    private int accentColor;
    private int subTextColor;
    
    private LinearLayout appsListContainer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("LappPrefs", MODE_PRIVATE);
        currentTheme = prefs.getString("theme", "white");
        applyTheme();
        createUI();
    }
    
    private void applyTheme() {
        switch(currentTheme) {
            case "black":
                backgroundColor = Color.parseColor("#0F0F0F");
                cardColor = Color.parseColor("#1E1E1E");
                textColor = Color.parseColor("#FFFFFF");
                subTextColor = Color.parseColor("#999999");
                accentColor = Color.parseColor("#FFB6C1");
                break;
            case "pink":
                backgroundColor = Color.parseColor("#FFF5F8");
                cardColor = Color.parseColor("#FFFFFF");
                textColor = Color.parseColor("#4A2B3A");
                subTextColor = Color.parseColor("#B08897");
                accentColor = Color.parseColor("#FF8FB1");
                break;
            default:
                backgroundColor = Color.parseColor("#FAFAFA");
                cardColor = Color.parseColor("#FFFFFF");
                textColor = Color.parseColor("#1A1A1A");
                subTextColor = Color.parseColor("#888888");
                accentColor = Color.parseColor("#FFB6C1");
                break;
        }
    }
    
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
    
    private void createUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(backgroundColor);
        root.setPadding(dp(20), dp(40), dp(20), dp(20));
        
        // Верхняя панель
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(24));
        
        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextSize(28);
        btnBack.setTextColor(textColor);
        btnBack.setPadding(dp(8), dp(8), dp(16), dp(8));
        btnBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        
        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextSize(24);
        title.setTextColor(textColor);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        topBar.addView(btnBack);
        topBar.addView(title);
        root.addView(topBar);
        
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll);
        
        // === ТЕМА ===
        content.addView(sectionTitle("🎨  Тема оформления"));
        
        LinearLayout themesRow = new LinearLayout(this);
        themesRow.setOrientation(LinearLayout.HORIZONTAL);
        themesRow.setPadding(0, dp(8), 0, dp(24));
        
        themesRow.addView(themeButton("Белая", "white", Color.WHITE));
        themesRow.addView(themeButton("Чёрная", "black", Color.parseColor("#1E1E1E")));
        themesRow.addView(themeButton("Розовая", "pink", Color.parseColor("#FFB6C1")));
        
        content.addView(themesRow);
        
        // === РАЗРЕШЕНИЯ ===
        content.addView(sectionTitle("🔐  Разрешения приложений"));
        
        TextView permHint = new TextView(this);
        permHint.setText("Выберите приложение, чтобы настроить доступ к камере, микрофону и уведомлениям");
        permHint.setTextSize(13);
        permHint.setTextColor(subTextColor);
        permHint.setPadding(0, dp(4), 0, dp(12));
        content.addView(permHint);
        
        appsListContainer = new LinearLayout(this);
        appsListContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(appsListContainer);
        
        loadAppPermissions();
        
        // === О ПРИЛОЖЕНИИ ===
        content.addView(sectionTitle("ℹ️  О приложении"));
        
        LinearLayout aboutBox = new LinearLayout(this);
        aboutBox.setOrientation(LinearLayout.VERTICAL);
        aboutBox.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable aboutBg = new GradientDrawable();
        aboutBg.setColor(cardColor);
        aboutBg.setCornerRadius(dp(16));
        aboutBox.setBackground(aboutBg);
        
        TextView aboutText = new TextView(this);
        aboutText.setText("Lapp v1.0\nОболочка для веб-приложений\n\nПозволяет запускать HTML-приложения в виде ярлыков на рабочем столе.");
        aboutText.setTextSize(13);
        aboutText.setTextColor(textColor);
        aboutText.setLineSpacing(0, 1.3f);
        
        aboutBox.addView(aboutText);
        content.addView(aboutBox);
        
        setContentView(root);
    }
    
    private TextView sectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(accentColor);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, dp(16), 0, dp(8));
        return tv;
    }
    
    private View themeButton(String label, final String themeCode, final int previewColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(12), dp(8), dp(12));
        
        boolean active = currentTheme.equals(themeCode);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardColor);
        bg.setCornerRadius(dp(16));
        if (active) bg.setStroke(dp(3), accentColor);
        else bg.setStroke(dp(1), Color.parseColor("#33000000"));
        box.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        box.setLayoutParams(params);
        
        // Цветной кружок
        View colorCircle = new View(this);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(previewColor);
        circleBg.setStroke(dp(2), Color.parseColor("#44000000"));
        colorCircle.setBackground(circleBg);
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        colorCircle.setLayoutParams(circleParams);
        
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTextColor(textColor);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(8), 0, 0);
        
        box.addView(colorCircle);
        box.addView(tv);
        
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit().putString("theme", themeCode).apply();
                currentTheme = themeCode;
                applyTheme();
                recreate();
            }
        });
        
        return box;
    }
    
    private void loadAppPermissions() {
        appsListContainer.removeAllViews();
        
        try {
            JSONArray apps = new JSONArray(prefs.getString("apps", "[]"));
            
            if (apps.length() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Нет добавленных приложений");
                empty.setTextSize(13);
                empty.setTextColor(subTextColor);
                empty.setPadding(0, dp(8), 0, dp(8));
                appsListContainer.addView(empty);
                return;
            }
            
            for (int i = 0; i < apps.length(); i++) {
                final JSONObject app = apps.getJSONObject(i);
                appsListContainer.addView(createAppPermRow(app));
            }
        } catch (Exception e) {}
    }
    
    private View createAppPermRow(final JSONObject app) throws Exception {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardColor);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(params);
        
        // Название
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView name = new TextView(this);
        name.setText(app.getString("name"));
        name.setTextSize(15);
        name.setTextColor(textColor);
        name.setTypeface(null, Typeface.BOLD);
        
        // Сводка разрешений
        StringBuilder sb = new StringBuilder();
        if (app.optBoolean("perm_camera", true)) sb.append("📷 ");
        if (app.optBoolean("perm_mic", true)) sb.append("🎤 ");
        if (app.optBoolean("perm_notif", true)) sb.append("🔔");
        if (sb.length() == 0) sb.append("Все выключены");
        
        TextView summary = new TextView(this);
        summary.setText(sb.toString().trim());
        summary.setTextSize(12);
        summary.setTextColor(subTextColor);
        summary.setPadding(0, dp(3), 0, 0);
        
        textCol.addView(name);
        textCol.addView(summary);
        
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(24);
        arrow.setTextColor(subTextColor);
        arrow.setPadding(dp(8), 0, 0, 0);
        
        row.addView(textCol);
        row.addView(arrow);
        
        row.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editPermissions(app);
            }
        });
        
        return row;
    }
    
    private void editPermissions(final JSONObject app) {
        try {
            final boolean[] perms = {
                app.optBoolean("perm_camera", true),
                app.optBoolean("perm_mic", true),
                app.optBoolean("perm_notif", true)
            };
            String[] labels = {"📷  Камера", "🎤  Микрофон", "🔔  Уведомления"};
            
            new AlertDialog.Builder(this)
                .setTitle(app.getString("name"))
                .setMultiChoiceItems(labels, perms, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface d, int which, boolean isChecked) {
                        perms[which] = isChecked;
                    }
                })
                .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            String path = app.optString("path", "");
                            String url = app.optString("url", "");
                            JSONArray apps = new JSONArray(prefs.getString("apps", "[]"));
                            for (int i = 0; i < apps.length(); i++) {
                                JSONObject current = apps.getJSONObject(i);
                                boolean match = (!path.isEmpty() && current.optString("path", "").equals(path))
                                             || (!url.isEmpty() && current.optString("url", "").equals(url));
                                if (match) {
                                    current.put("perm_camera", perms[0]);
                                    current.put("perm_mic", perms[1]);
                                    current.put("perm_notif", perms[2]);
                                    break;
                                }
                            }
                            prefs.edit().putString("apps", apps.toString()).apply();
                            loadAppPermissions();
                            Toast.makeText(SettingsActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {}
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        } catch (Exception e) {}
    }
}
