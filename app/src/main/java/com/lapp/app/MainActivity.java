package com.lapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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
    private int subTextColor;
    
    // Для обновления: храним, какое приложение сейчас обновляем
    private int updateIndex = -1;
    private JSONObject updateApp = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("LappPrefs", MODE_PRIVATE);
        currentTheme = prefs.getString("theme", "white");
        
        applyTheme();
        createUI();
        loadSavedApps();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (appsContainer != null) loadSavedApps();
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
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(backgroundColor);
        mainLayout.setPadding(dp(20), dp(48), dp(20), dp(20));
        
        TextView title = new TextView(this);
        title.setText("Lapp");
        title.setTextSize(34);
        title.setTextColor(textColor);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.START);
        
        TextView subtitle = new TextView(this);
        subtitle.setText("Мои веб-приложения");
        subtitle.setTextSize(14);
        subtitle.setTextColor(subTextColor);
        subtitle.setGravity(Gravity.START);
        subtitle.setPadding(0, dp(4), 0, dp(24));
        
        mainLayout.addView(title);
        mainLayout.addView(subtitle);
        
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(0, dp(16), 0, dp(8));
        
        Button btnUrl = createButton("+ Ссылка", true);
        btnUrl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddUrlDialog(); }
        });
        
        Button btnAdd = createButton("+ Файл", true);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openFilePicker(false); }
        });
        
        Button btnTheme = createButton("Тема", false);
        btnTheme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showThemeDialog(); }
        });
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(dp(4), 0, dp(4), 0);
        btnUrl.setLayoutParams(btnParams);
        btnAdd.setLayoutParams(btnParams);
        btnTheme.setLayoutParams(btnParams);
        
        bottomBar.addView(btnUrl);
        bottomBar.addView(btnAdd);
        bottomBar.addView(btnTheme);
        
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setClipToPadding(false);
        
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        
        appsContainer = new LinearLayout(this);
        appsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appsContainer);
        
        mainLayout.addView(scrollView);
        mainLayout.addView(bottomBar);
        
        setContentView(mainLayout);
    }
    
    private Button createButton(String text, boolean primary) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(28));
        
        if (primary) {
            drawable.setColor(accentColor);
            btn.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(cardColor);
            drawable.setStroke(dp(1), accentColor);
            btn.setTextColor(textColor);
        }
        
        btn.setBackground(drawable);
        btn.setPadding(dp(8), dp(12), dp(8), dp(12));
        
        return btn;
    }
    
    private void showAddUrlDialog() {
        final EditText input = new EditText(this);
        input.setHint("https://example.com");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        new AlertDialog.Builder(this)
            .setTitle("Добавить ссылку")
            .setView(input)
            .setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) return;
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                    }
                    try {
                        String name = Uri.parse(url).getHost();
                        if (name == null) name = "Веб-приложение";
                        if (name.startsWith("www.")) name = name.substring(4);
                        
                        JSONArray apps = getSavedApps();
                        JSONObject app = new JSONObject();
                        app.put("name", name);
                        app.put("url", url);
                        app.put("type", "url");
                        app.put("date", System.currentTimeMillis());
                        apps.put(app);
                        prefs.edit().putString("apps", apps.toString()).apply();
                        loadSavedApps();
                        Toast.makeText(MainActivity.this, "Добавлено!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showThemeDialog() {
        String[] themes = {"Белая", "Черная", "Нежно-розовая"};
        new AlertDialog.Builder(this)
            .setTitle("Выберите тему")
            .setItems(themes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
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
    
    private void openFilePicker(boolean isUpdate) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, isUpdate ? 2 : 1);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        
        if (requestCode == 1) {
            // Добавление нового приложения
            try {
                saveNewWebApp(uri);
                loadSavedApps();
                Toast.makeText(this, "Приложение добавлено!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) {
            // Обновление существующего
            try {
                performUpdate(uri);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void saveNewWebApp(Uri uri) throws Exception {
        // Создаём уникальную папку для приложения
        String baseName = getCleanName(uri);
        String folderName = baseName + "_" + System.currentTimeMillis();
        
        File appDir = new File(new File(getFilesDir(), "webapps"), folderName);
        appDir.mkdirs();
        
        File destFile = new File(appDir, "index.html");
        copyFile(uri, destFile);
        
        JSONArray apps = getSavedApps();
        JSONObject app = new JSONObject();
        app.put("name", baseName);
        app.put("folder", folderName);
        app.put("path", destFile.getAbsolutePath());
        app.put("type", "file");
        app.put("date", System.currentTimeMillis());
        apps.put(app);
        
        prefs.edit().putString("apps", apps.toString()).apply();
    }
    
    private String getCleanName(Uri uri) {
        String name = "app";
        String path = uri.getLastPathSegment();
        if (path != null) {
            if (path.contains("/")) path = path.substring(path.lastIndexOf("/") + 1);
            name = path.replace(".html", "").replace(".htm", "");
            if (name.isEmpty()) name = "app";
        }
        return name;
    }
    
    private void copyFile(Uri uri, File dest) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        FileOutputStream os = new FileOutputStream(dest);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
        os.close();
        is.close();
    }
    
    private void performUpdate(Uri uri) throws Exception {
        if (updateApp == null) return;
        
        final JSONObject app = updateApp;
        final Uri fileUri = uri;
        
        // Определяем новое имя из файла
        String newName = getCleanName(uri);
        final String oldName = app.getString("name");
        
        if (!newName.equals(oldName)) {
            // Спрашиваем: переименовать?
            new AlertDialog.Builder(this)
                .setTitle("Переименовать?")
                .setMessage("Имя файла: \"" + newName + "\"\nТекущее имя: \"" + oldName + "\"\n\nПереименовать приложение?")
                .setPositiveButton("Да, переименовать", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            doUpdateFile(app, fileUri, newName);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Нет, оставить имя", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            doUpdateFile(app, fileUri, null);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
        } else {
            // Имя совпадает — обновляем без вопросов
            doUpdateFile(app, fileUri, null);
        }
    }
    
    private void doUpdateFile(JSONObject app, Uri uri, String newName) throws Exception {
        // Получаем путь к файлу в существующей папке
        String oldPath = app.getString("path");
        File oldFile = new File(oldPath);
        
        // Перезаписываем тот же самый файл index.html — origin сохранится!
        copyFile(uri, oldFile);
        
        // Если нужно переименовать — меняем только отображаемое имя в JSON
        if (newName != null) {
            app.put("name", newName);
            
            // Обновляем в общем списке
            JSONArray apps = getSavedApps();
            for (int i = 0; i < apps.length(); i++) {
                JSONObject current = apps.getJSONObject(i);
                if (current.optString("path", "").equals(oldPath)) {
                    current.put("name", newName);
                    break;
                }
            }
            prefs.edit().putString("apps", apps.toString()).apply();
        }
        
        app.put("date", System.currentTimeMillis());
        
        // Обновляем дату в списке
        JSONArray apps2 = getSavedApps();
        for (int i = 0; i < apps2.length(); i++) {
            JSONObject current = apps2.getJSONObject(i);
            if (current.optString("path", "").equals(oldPath)) {
                current.put("date", System.currentTimeMillis());
                break;
            }
        }
        prefs.edit().putString("apps", apps2.toString()).apply();
        
        updateIndex = -1;
        updateApp = null;
        loadSavedApps();
        
        Toast.makeText(this, "Обновлено! Данные сохранены.", Toast.LENGTH_LONG).show();
    }
    
    private JSONArray getSavedApps() {
        try {
            return new JSONArray(prefs.getString("apps", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }
    
    private void loadSavedApps() {
        appsContainer.removeAllViews();
        
        try {
            JSONArray apps = getSavedApps();
            
            if (apps.length() == 0) {
                LinearLayout emptyBox = new LinearLayout(this);
                emptyBox.setOrientation(LinearLayout.VERTICAL);
                emptyBox.setGravity(Gravity.CENTER);
                emptyBox.setPadding(0, dp(80), 0, dp(80));
                
                TextView emptyIcon = new TextView(this);
                emptyIcon.setText("📱");
                emptyIcon.setTextSize(48);
                emptyIcon.setGravity(Gravity.CENTER);
                
                TextView emptyText = new TextView(this);
                emptyText.setText("Пока ничего нет");
                emptyText.setTextColor(textColor);
                emptyText.setTextSize(18);
                emptyText.setTypeface(null, Typeface.BOLD);
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(0, dp(16), 0, dp(8));
                
                TextView emptyHint = new TextView(this);
                emptyHint.setText("Добавьте HTML-файл или ссылку");
                emptyHint.setTextColor(subTextColor);
                emptyHint.setTextSize(14);
                emptyHint.setGravity(Gravity.CENTER);
                
                emptyBox.addView(emptyIcon);
                emptyBox.addView(emptyText);
                emptyBox.addView(emptyHint);
                appsContainer.addView(emptyBox);
            } else {
                for (int i = 0; i < apps.length(); i++) {
                    addAppCard(apps.getJSONObject(i));
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addAppCard(final JSONObject app) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(cardColor);
        cardBg.setCornerRadius(dp(20));
        card.setBackground(cardBg);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        
        // Верхняя строка: иконка + название
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView icon = new TextView(this);
        String type = app.optString("type", "file");
        icon.setText(type.equals("url") ? "🌐" : "📄");
        icon.setTextSize(22);
        icon.setGravity(Gravity.CENTER);
        
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.OVAL);
        iconBg.setColor(adjustAlpha(accentColor, 0.15f));
        icon.setBackground(iconBg);
        
        int iconSize = dp(48);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(0, 0, dp(14), 0);
        icon.setLayoutParams(iconParams);
        
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView nameText = new TextView(this);
        nameText.setText(app.getString("name"));
        nameText.setTextColor(textColor);
        nameText.setTextSize(17);
        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setMaxLines(1);
        nameText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        TextView subText = new TextView(this);
        if (type.equals("url")) {
            subText.setText(app.optString("url", ""));
        } else {
            subText.setText("Локальный файл");
        }
        subText.setTextColor(subTextColor);
        subText.setTextSize(12);
        subText.setMaxLines(1);
        subText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        subText.setPadding(0, dp(4), 0, 0);
        
        textCol.addView(nameText);
        textCol.addView(subText);
        
        topRow.addView(icon);
        topRow.addView(textCol);
        card.addView(topRow);
        
        // Кнопки
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(14), 0, 0);
        
        final String appName = app.getString("name");
        final String appPath = app.optString("path", "");
        final String appUrl = app.optString("url", "");
        final String appType = type;
        
        // Открыть
        Button btnOpen = createSmallButton("Открыть", true);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WebAppActivity.class);
                if (appType.equals("url")) {
                    intent.putExtra("url", appUrl);
                } else {
                    intent.putExtra("filePath", appPath);
                }
                startActivity(intent);
            }
        });
        
        // Обновить (только для файлов) или Ярлык (для ссылок)
        Button btnMiddle;
        if (appType.equals("file")) {
            btnMiddle = createSmallButton("Обновить", false);
            btnMiddle.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    confirmUpdate(app);
                }
            });
        } else {
            btnMiddle = createSmallButton("Ярлык", false);
            btnMiddle.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    createShortcut(appName, appPath, appUrl, appType);
                }
            });
        }
        
        Button btnDelete = createSmallButton("Удалить", false);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                confirmDelete(appPath, appUrl);
            }
        });
        
        LinearLayout.LayoutParams smallParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        smallParams.setMargins(dp(3), 0, dp(3), 0);
        btnOpen.setLayoutParams(smallParams);
        btnMiddle.setLayoutParams(smallParams);
        btnDelete.setLayoutParams(smallParams);
        
        btnRow.addView(btnOpen);
        btnRow.addView(btnMiddle);
        btnRow.addView(btnDelete);
        card.addView(btnRow);
        
        // Вторая строка кнопок для файлов: ярлык отдельно
        if (appType.equals("file")) {
            LinearLayout btnRow2 = new LinearLayout(this);
            btnRow2.setOrientation(LinearLayout.HORIZONTAL);
            btnRow2.setPadding(0, dp(8), 0, 0);
            
            Button btnShortcut = createSmallButton("Создать ярлык", false);
            btnShortcut.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    createShortcut(appName, appPath, appUrl, appType);
                }
            });
            btnShortcut.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT));
            btnRow2.addView(btnShortcut);
            card.addView(btnRow2);
        }
        
        appsContainer.addView(card);
    }
    
    private void confirmUpdate(final JSONObject app) {
        new AlertDialog.Builder(this)
            .setTitle("Обновить приложение?")
            .setMessage("Файл будет заменён новой версией.\n\n✅ Данные (localStorage, cookies, настройки) сохранятся.\n\nПродолжить?")
            .setPositiveButton("Обновить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    updateApp = app;
                    openFilePicker(true);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void confirmDelete(String path, String url) {
        new AlertDialog.Builder(this)
            .setTitle("Удалить приложение?")
            .setMessage("⚠️ Все данные приложения будут потеряны безвозвратно.\n\nПродолжить?")
            .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    deleteApp(path, url);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
    
    private Button createSmallButton(String text, boolean primary) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(20));
        
        if (primary) {
            drawable.setColor(accentColor);
            btn.setTextColor(Color.WHITE);
        } else {
            drawable.setColor(adjustAlpha(textColor, 0.08f));
            btn.setTextColor(textColor);
        }
        
        btn.setBackground(drawable);
        btn.setPadding(dp(4), dp(8), dp(4), dp(8));
        
        return btn;
    }
    
    private void createShortcut(String name, String path, String url, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                
                if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                    Intent intent = new Intent(this, WebAppActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    if (type.equals("url")) {
                        intent.putExtra("url", url);
                    } else {
                        intent.putExtra("filePath", path);
                    }
                    
                    ShortcutInfo shortcut = new ShortcutInfo.Builder(this, 
                        "lapp_" + name + "_" + System.currentTimeMillis())
                        .setShortLabel(name)
                        .setLongLabel(name)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher))
                        .setIntent(intent)
                        .build();
                    
                    shortcutManager.requestPinShortcut(shortcut, null);
                    Toast.makeText(this, "Запрос на создание ярлыка отправлен", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Ярлыки не поддерживаются", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Intent shortcutIntent = new Intent(this, WebAppActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            if (type.equals("url")) {
                shortcutIntent.putExtra("url", url);
            } else {
                shortcutIntent.putExtra("filePath", path);
            }
            
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            sendBroadcast(addIntent);
            
            Toast.makeText(this, "Ярлык создан!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteApp(String path, String url) {
        try {
            // Удаляем всю папку приложения
            if (!path.isEmpty()) {
                File file = new File(path);
                File parentDir = file.getParentFile();
                if (parentDir != null && parentDir.exists()) {
                    deleteRecursive(parentDir);
                } else if (file.exists()) {
                    file.delete();
                }
            }
            
            JSONArray apps = getSavedApps();
            JSONArray newApps = new JSONArray();
            
            for (int i = 0; i < apps.length(); i++) {
                JSONObject current = apps.getJSONObject(i);
                boolean matchPath = !path.isEmpty() && current.optString("path", "").equals(path);
                boolean matchUrl = !url.isEmpty() && current.optString("url", "").equals(url);
                if (!matchPath && !matchUrl) {
                    newApps.put(current);
                }
            }
            
            prefs.edit().putString("apps", newApps.toString()).apply();
            loadSavedApps();
            Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }
}
