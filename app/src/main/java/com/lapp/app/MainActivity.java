package com.lapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import java.io.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    
    private LinearLayout mainLayout;
    private LinearLayout appsContainer;
    private SharedPreferences prefs;
    private String currentTheme = "white";
    
    private int backgroundColor;
    private int cardColor;
    private int textColor;
    private int accentColor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("LappPrefs", MODE_PRIVATE);
        currentTheme = prefs.getString("theme", "white");
        
        applyTheme();
        createUI();
        loadSavedApps();
    }
    
    private void applyTheme() {
        switch(currentTheme) {
            case "black":
                backgroundColor = Color.parseColor("#1a1a1a");
                cardColor = Color.parseColor("#2d2d2d");
                textColor = Color.parseColor("#ffffff");
                accentColor = Color.parseColor("#FFB6C1");
                break;
            case "pink":
                backgroundColor = Color.parseColor("#FFF0F5");
                cardColor = Color.parseColor("#FFE4E9");
                textColor = Color.parseColor("#4a4a4a");
                accentColor = Color.parseColor("#FF69B4");
                break;
            default:
                backgroundColor = Color.parseColor("#FFFFFF");
                cardColor = Color.parseColor("#F5F5F5");
                textColor = Color.parseColor("#333333");
                accentColor = Color.parseColor("#FFB6C1");
                break;
        }
    }
    
    private void createUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(backgroundColor);
        mainLayout.setPadding(32, 48, 32, 32);
        
        TextView title = new TextView(this);
        title.setText("Lapp");
        title.setTextSize(32);
        title.setTextColor(textColor);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Ваши web-приложения");
        subtitle.setTextSize(14);
        subtitle.setTextColor(textColor);
        subtitle.setAlpha(0.6f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 32);
        
        mainLayout.addView(title);
        mainLayout.addView(subtitle);
        
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        buttonRow.setPadding(0, 0, 0, 32);
        
        Button btnAdd = createButton("+ Добавить");
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openFilePicker(); }
        });
        
        Button btnTheme = createButton("Тема");
        btnTheme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showThemeDialog(); }
        });
        
        buttonRow.addView(btnAdd);
        buttonRow.addView(btnTheme);
        mainLayout.addView(buttonRow);
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        
        appsContainer = new LinearLayout(this);
        appsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appsContainer);
        mainLayout.addView(scrollView);
        
        setContentView(mainLayout);
    }
    
    private Button createButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(cardColor);
        drawable.setCornerRadius(24);
        drawable.setStroke(2, accentColor);
        btn.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        btn.setLayoutParams(params);
        btn.setPadding(24, 12, 24, 12);
        
        return btn;
    }
    
    private void showThemeDialog() {
        String[] themes = {"Белая", "Черная", "Нежно-розовая"};
        new AlertDialog.Builder(this)
            .setTitle("Выберите тему")
            .setItems(themes, new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface dialog, int which) {
                    switch(which) {
                        case 0: currentTheme = "white"; break;
                        case 1: currentTheme = "black"; break;
                        case 2: currentTheme = "pink"; break;
                    }
                    prefs.edit().putString("theme", currentTheme).apply();
                    recreate();
                }
            })
            .show();
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, 1);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    saveWebApp(uri);
                    loadSavedApps();
                    Toast.makeText(this, "Приложение добавлено!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
    private void saveWebApp(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        String fileName = getFileName(uri);
        
        File dir = new File(getFilesDir(), "webapps");
        if (!dir.exists()) dir.mkdirs();
        
        File destFile = new File(dir, fileName);
        FileOutputStream outputStream = new FileOutputStream(destFile);
        
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        outputStream.close();
        inputStream.close();
        
        JSONArray apps = getSavedApps();
        JSONObject app = new JSONObject();
        app.put("name", fileName.replace(".html", "").replace(".htm", ""));
        app.put("file", fileName);
        app.put("path", destFile.getAbsolutePath());
        app.put("date", System.currentTimeMillis());
        apps.put(app);
        
        prefs.edit().putString("apps", apps.toString()).apply();
    }
    
    private String getFileName(Uri uri) {
        String result = "app_" + System.currentTimeMillis() + ".html";
        String path = uri.getLastPathSegment();
        if (path != null && path.contains("/")) {
            result = path.substring(path.lastIndexOf("/") + 1);
        } else if (path != null) {
            result = path;
        }
        return result;
    }
    
    private JSONArray getSavedApps() {
        try {
            String saved = prefs.getString("apps", "[]");
            return new JSONArray(saved);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
    
    private void loadSavedApps() {
        appsContainer.removeAllViews();
        
        try {
            JSONArray apps = getSavedApps();
            
            if (apps.length() == 0) {
                TextView emptyText = new TextView(this);
                emptyText.setText("Нет загруженных приложений\nНажмите + чтобы добавить");
                emptyText.setTextColor(textColor);
                emptyText.setAlpha(0.5f);
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setTextSize(16);
                emptyText.setPadding(0, 64, 0, 64);
                appsContainer.addView(emptyText);
            } else {
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject app = apps.getJSONObject(i);
                    addAppCard(app);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addAppCard(JSONObject app) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 16, 24, 16);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(cardColor);
        cardBg.setCornerRadius(16);
        card.setBackground(cardBg);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        
        TextView nameText = new TextView(this);
        nameText.setText(app.getString("name"));
        nameText.setTextColor(textColor);
        nameText.setTextSize(18);
        nameText.setTypeface(null, Typeface.BOLD);
        
        TextView dateText = new TextView(this);
        long date = app.getLong("date");
        String dateStr = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(date));
        dateText.setText("Добавлено: " + dateStr);
        dateText.setTextColor(textColor);
        dateText.setAlpha(0.6f);
        dateText.setTextSize(12);
        
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 12, 0, 0);
        
        final String appName = app.getString("name");
        final String appPath = app.getString("path");
        
        Button btnOpen = createSmallButton("Открыть");
        btnOpen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openWebApp(appPath); }
        });
        
        Button btnShortcut = createSmallButton("Ярлык");
        btnShortcut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { createShortcut(appName, appPath); }
        });
        
        Button btnDelete = createSmallButton("Удалить");
        btnDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { deleteApp(appPath); }
        });
        
        btnRow.addView(btnOpen);
        btnRow.addView(btnShortcut);
        btnRow.addView(btnDelete);
        
        card.addView(nameText);
        card.addView(dateText);
        card.addView(btnRow);
        
        appsContainer.addView(card);
    }
    
    private Button createSmallButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTextSize(12);
        btn.setAllCaps(false);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setCornerRadius(16);
        drawable.setStroke(1, accentColor);
        btn.setBackground(drawable);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 8, 0);
        btn.setLayoutParams(params);
        btn.setPadding(16, 8, 16, 8);
        
        return btn;
    }
    
    private void openWebApp(String path) {
        Intent intent = new Intent(this, WebAppActivity.class);
        intent.putExtra("filePath", path);
        startActivity(intent);
    }
    
    private void createShortcut(String name, String path) {
        Intent shortcutIntent = new Intent(this, WebAppActivity.class);
        shortcutIntent.setAction(Intent.ACTION_VIEW);
        shortcutIntent.putExtra("filePath", path);
        
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(addIntent);
        
        Toast.makeText(this, "Ярлык создан!", Toast.LENGTH_SHORT).show();
    }
    
    private void deleteApp(String path) {
        try {
            File file = new File(path);
            if (file.exists()) file.delete();
            
            JSONArray apps = getSavedApps();
            JSONArray newApps = new JSONArray();
            
            for (int i = 0; i < apps.length(); i++) {
                JSONObject current = apps.getJSONObject(i);
                if (!current.getString("path").equals(path)) {
                    newApps.put(current);
                }
            }
            
            prefs.edit().putString("apps", newApps.toString()).apply();
            loadSavedApps();
            Toast.makeText(this, "Приложение удалено", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
