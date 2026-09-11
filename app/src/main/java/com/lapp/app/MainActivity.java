package com.lapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends Activity {
    
    private LinearLayout rootLayout;
    private LinearLayout contentArea;
    private LinearLayout appsGrid;
    private SharedPreferences prefs;
    private String currentTheme = "white";
    
    private int backgroundColor;
    private int cardColor;
    private int textColor;
    private int accentColor;
    private int subTextColor;
    
    private JSONObject updateApp = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("LappPrefs", MODE_PRIVATE);
        currentTheme = prefs.getString("theme", "white");
        
        applyTheme();
        createUI();
        loadApps();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        String newTheme = prefs.getString("theme", "white");
        if (!newTheme.equals(currentTheme)) {
            currentTheme = newTheme;
            recreate();
            return;
        }
        if (appsGrid != null) loadApps();
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
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(backgroundColor);
        
        // === ВЕРХНЯЯ ПАНЕЛЬ ===
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(16), dp(40), dp(16), dp(12));
        
        // Кнопка настроек (слева)
        ImageButton btnSettings = createIconButton("⚙");
        btnSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        
        // Заголовок (центр)
        TextView title = new TextView(this);
        title.setText("Lapp");
        title.setTextSize(24);
        title.setTextColor(textColor);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        
        // Кнопка добавить (справа)
        ImageButton btnAdd = createIconButton("+");
        btnAdd.setTextSize(28);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAddMenu(v);
            }
        });
        
        topBar.addView(btnSettings);
        topBar.addView(title);
        topBar.addView(btnAdd);
        rootLayout.addView(topBar);
        
        // === ОБЛАСТЬ КОНТЕНТА ===
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        scrollView.setClipToPadding(false);
        scrollView.setPadding(dp(12), dp(8), dp(12), dp(20));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        
        appsGrid = new LinearLayout(this);
        appsGrid.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(appsGrid);
        rootLayout.addView(scrollView);
        
        setContentView(rootLayout);
    }
    
    private ImageButton createIconButton(String symbol) {
        ImageButton btn = new ImageButton(this);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(cardColor);
        btn.setBackground(bg);
        
        // Используем TextView вместо иконки — рисуем символ
        btn.setImageDrawable(textToDrawable(symbol, textColor, 48));
        btn.setScaleType(ImageView.ScaleType.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(44));
        btn.setLayoutParams(params);
        btn.setPadding(dp(8), dp(8), dp(8), dp(8));
        
        return btn;
    }
    
    private Drawable textToDrawable(String text, int color, int sizePx) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(sizePx);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        
        int size = sizePx + dp(8);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2f, paint);
        
        return new BitmapDrawable(getResources(), bitmap);
    }
    
    private void showAddMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("🔗  Добавить ссылку");
        popup.getMenu().add("📄  Добавить файл");
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                String t = item.getTitle().toString();
                if (t.contains("ссылку")) {
                    showAddUrlDialog();
                } else {
                    openFilePicker(false, null);
                }
                return true;
            }
        });
        popup.show();
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
                        if (name == null) name = "Web";
                        if (name.startsWith("www.")) name = name.substring(4);
                        
                        JSONArray apps = getSavedApps();
                        JSONObject app = new JSONObject();
                        app.put("name", name);
                        app.put("url", url);
                        app.put("type", "url");
                        app.put("date", System.currentTimeMillis());
                        app.put("icon", "");
                        app.put("perm_camera", true);
                        app.put("perm_mic", true);
                        app.put("perm_notif", true);
                        apps.put(app);
                        prefs.edit().putString("apps", apps.toString()).apply();
                        loadApps();
                        Toast.makeText(MainActivity.this, "Добавлено!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void openFilePicker(boolean isUpdate, JSONObject appToUpdate) {
        updateApp = appToUpdate;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, isUpdate ? 2 : 1);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK || data == null) {
            updateApp = null;
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            updateApp = null;
            return;
        }
        
        if (requestCode == 1) {
            try {
                saveNewWebApp(uri);
                loadApps();
                Toast.makeText(this, "Приложение добавлено!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) {
            try {
                performUpdate(uri);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void saveNewWebApp(Uri uri) throws Exception {
        String html = readTextFromUri(uri);
        
        // Парсим название и иконку из HTML
        String htmlTitle = parseTitle(html);
        String iconUrl = parseIconUrl(html);
        
        String fileName = getFileName(uri);
        String baseName = fileName.replace(".html", "").replace(".htm", "");
        if (baseName.isEmpty()) baseName = "app";
        
        // Если есть <title> — используем его
        String finalName = (htmlTitle != null && !htmlTitle.isEmpty()) ? htmlTitle : baseName;
        
        // Создаём папку
        String folderName = "app_" + System.currentTimeMillis();
        File appDir = new File(new File(getFilesDir(), "webapps"), folderName);
        appDir.mkdirs();
        
        File destFile = new File(appDir, "index.html");
        FileOutputStream fos = new FileOutputStream(destFile);
        fos.write(html.getBytes("UTF-8"));
        fos.close();
        
        // Загружаем иконку, если она есть
        String savedIconPath = "";
        if (iconUrl != null && !iconUrl.isEmpty()) {
            savedIconPath = downloadIcon(iconUrl, appDir);
        }
        
        JSONArray apps = getSavedApps();
        JSONObject app = new JSONObject();
        app.put("name", finalName);
        app.put("folder", folderName);
        app.put("path", destFile.getAbsolutePath());
        app.put("icon", savedIconPath);
        app.put("type", "file");
        app.put("date", System.currentTimeMillis());
        app.put("perm_camera", true);
        app.put("perm_mic", true);
        app.put("perm_notif", true);
        apps.put(app);
        
        prefs.edit().putString("apps", apps.toString()).apply();
    }
    
    private String readTextFromUri(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) > 0) bos.write(buf, 0, len);
        is.close();
        return new String(bos.toByteArray(), "UTF-8");
    }
    
    private String parseTitle(String html) {
        try {
            Pattern p = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                String title = m.group(1).trim();
                if (title.length() > 30) title = title.substring(0, 30);
                return title;
            }
        } catch (Exception e) {}
        return null;
    }
    
    private String parseIconUrl(String html) {
        try {
            // <link rel="icon" href="...">
            Pattern p1 = Pattern.compile("<link[^>]*rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"'][^>]*href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m1 = p1.matcher(html);
            if (m1.find()) return m1.group(1);
            
            // <link href="..." rel="icon">
            Pattern p2 = Pattern.compile("<link[^>]*href=[\"']([^\"']+)[\"'][^>]*rel=[\"'](?:icon|shortcut icon|apple-touch-icon)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m2 = p2.matcher(html);
            if (m2.find()) return m2.group(1);
            
            // <meta property="og:image" content="...">
            Pattern p3 = Pattern.compile("<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher m3 = p3.matcher(html);
            if (m3.find()) return m3.group(1);
        } catch (Exception e) {}
        return null;
    }
    
    private String downloadIcon(String url, File appDir) {
        try {
            if (url.startsWith("//")) url = "https:" + url;
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("data:")) {
                return ""; // относительный путь — не поддерживаем
            }
            
            File iconFile = new File(appDir, "icon.png");
            
            if (url.startsWith("data:image")) {
                // base64
                int comma = url.indexOf(',');
                if (comma > 0) {
                    String b64 = url.substring(comma + 1);
                    byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                    FileOutputStream fos = new FileOutputStream(iconFile);
                    fos.write(bytes);
                    fos.close();
                    return iconFile.getAbsolutePath();
                }
                return "";
            }
            
            // Скачиваем
            URL u = new URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            
            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(iconFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            fos.close();
            is.close();
            
            return iconFile.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getFileName(Uri uri) {
        String result = "app.html";
        String path = uri.getLastPathSegment();
        if (path != null) {
            if (path.contains("/")) path = path.substring(path.lastIndexOf("/") + 1);
            result = path;
        }
        return result;
    }
    
    private void performUpdate(Uri uri) throws Exception {
        if (updateApp == null) return;
        
        final JSONObject app = updateApp;
        final Uri fileUri = uri;
        
        String newHtml = readTextFromUri(uri);
        String newTitle = parseTitle(newHtml);
        final String oldName = app.getString("name");
        
        if (newTitle != null && !newTitle.isEmpty() && !newTitle.equals(oldName)) {
            new AlertDialog.Builder(this)
                .setTitle("Переименовать?")
                .setMessage("Новое имя: \"" + newTitle + "\"\nТекущее: \"" + oldName + "\"\n\nПереименовать?")
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            doUpdateFile(app, fileUri, newTitle);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
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
            doUpdateFile(app, fileUri, null);
        }
    }
    
    private void doUpdateFile(JSONObject app, Uri uri, String newName) throws Exception {
        String html = readTextFromUri(uri);
        String iconUrl = parseIconUrl(html);
        
        String oldPath = app.getString("path");
        File oldFile = new File(oldPath);
        File appDir = oldFile.getParentFile();
        
        // Перезаписываем файл — origin сохраняется!
        FileOutputStream fos = new FileOutputStream(oldFile);
        fos.write(html.getBytes("UTF-8"));
        fos.close();
        
        // Обновляем иконку, если есть
        String newIcon = "";
        if (iconUrl != null && !iconUrl.isEmpty() && appDir != null) {
            newIcon = downloadIcon(iconUrl, appDir);
        }
        
        JSONArray apps = getSavedApps();
        for (int i = 0; i < apps.length(); i++) {
            JSONObject current = apps.getJSONObject(i);
            if (current.optString("path", "").equals(oldPath)) {
                if (newName != null) current.put("name", newName);
                if (!newIcon.isEmpty()) current.put("icon", newIcon);
                current.put("date", System.currentTimeMillis());
                break;
            }
        }
        prefs.edit().putString("apps", apps.toString()).apply();
        
        updateApp = null;
        loadApps();
        
        Toast.makeText(this, "Обновлено! Данные сохранены.", Toast.LENGTH_LONG).show();
    }
    
    private JSONArray getSavedApps() {
        try {
            return new JSONArray(prefs.getString("apps", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }
    
    private void loadApps() {
        appsGrid.removeAllViews();
        
        try {
            JSONArray apps = getSavedApps();
            
            if (apps.length() == 0) {
                LinearLayout emptyBox = new LinearLayout(this);
                emptyBox.setOrientation(LinearLayout.VERTICAL);
                emptyBox.setGravity(Gravity.CENTER);
                emptyBox.setPadding(0, dp(100), 0, dp(100));
                
                TextView emptyIcon = new TextView(this);
                emptyIcon.setText("📱");
                emptyIcon.setTextSize(56);
                emptyIcon.setGravity(Gravity.CENTER);
                
                TextView emptyText = new TextView(this);
                emptyText.setText("Пока ничего нет");
                emptyText.setTextColor(textColor);
                emptyText.setTextSize(18);
                emptyText.setTypeface(null, Typeface.BOLD);
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(0, dp(16), 0, dp(8));
                
                TextView emptyHint = new TextView(this);
                emptyHint.setText("Нажмите + чтобы добавить");
                emptyHint.setTextColor(subTextColor);
                emptyHint.setTextSize(14);
                emptyHint.setGravity(Gravity.CENTER);
                
                emptyBox.addView(emptyIcon);
                emptyBox.addView(emptyText);
                emptyBox.addView(emptyHint);
                appsGrid.addView(emptyBox);
                return;
            }
            
            // Строим сетку по 4 колонки
            int columns = 4;
            LinearLayout currentRow = null;
            
            for (int i = 0; i < apps.length(); i++) {
                if (i % columns == 0) {
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    currentRow.setPadding(0, 0, 0, dp(20));
                    appsGrid.addView(currentRow);
                }
                
                final JSONObject app = apps.getJSONObject(i);
                View iconView = createAppIcon(app);
                
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                iconView.setLayoutParams(itemParams);
                currentRow.addView(iconView);
            }
            
            // Добиваем пустыми местами в последней строке
            if (currentRow != null) {
                int lastRowCount = apps.length() % columns;
                if (lastRowCount > 0) {
                    for (int i = lastRowCount; i < columns; i++) {
                        View empty = new View(this);
                        empty.setLayoutParams(new LinearLayout.LayoutParams(0, dp(1), 1f));
                        currentRow.addView(empty);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private View createAppIcon(final JSONObject app) throws Exception {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        
        // Иконка (квадрат со скруглением)
        FrameLayout iconFrame = new FrameLayout(this);
        int iconSize = dp(64);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconFrame.setLayoutParams(frameParams);
        
        // Фон-скругление
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(cardColor);
        iconBg.setCornerRadius(dp(14));
        iconFrame.setBackground(iconBg);
        iconFrame.setClipToOutline(true);
        
        // Загружаем картинку или ставим букву
        String iconPath = app.optString("icon", "");
        boolean iconLoaded = false;
        
        if (!iconPath.isEmpty()) {
            File iconFile = new File(iconPath);
            if (iconFile.exists()) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(iconPath);
                    if (bmp != null) {
                        ImageView iv = new ImageView(this);
                        iv.setImageBitmap(bmp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, 
                            FrameLayout.LayoutParams.MATCH_PARENT));
                        iconFrame.addView(iv);
                        iconLoaded = true;
                    }
                } catch (Exception e) {}
            }
        }
        
        if (!iconLoaded) {
            // Заглушка — первая буква + цвет от хэша
            String name = app.getString("name");
            String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            
            TextView letterView = new TextView(this);
            letterView.setText(letter);
            letterView.setTextSize(28);
            letterView.setTextColor(Color.WHITE);
            letterView.setTypeface(null, Typeface.BOLD);
            letterView.setGravity(Gravity.CENTER);
            letterView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.MATCH_PARENT));
            
            GradientDrawable stubBg = new GradientDrawable();
            stubBg.setColor(generateColor(name));
            stubBg.setCornerRadius(dp(14));
            letterView.setBackground(stubBg);
            
            iconFrame.addView(letterView);
        }
        
        // Название под иконкой
        TextView nameView = new TextView(this);
        nameView.setText(app.getString("name"));
        nameView.setTextSize(11);
        nameView.setTextColor(textColor);
        nameView.setGravity(Gravity.CENTER);
        nameView.setMaxLines(2);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameView.setPadding(dp(2), dp(6), dp(2), 0);
        
        wrapper.addView(iconFrame);
        wrapper.addView(nameView);
        
        // Клик — открыть
        wrapper.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openApp(app);
            }
        });
        
        // Долгое нажатие — меню
        wrapper.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                showAppMenu(app, v);
                return true;
            }
        });
        
        return wrapper;
    }
    
    private int generateColor(String name) {
        int hash = name.hashCode();
        int[] colors = {
            0xFFE57373, 0xFFF06292, 0xFFBA68C8, 0xFF9575CD,
            0xFF7986CB, 0xFF64B5F6, 0xFF4FC3F7, 0xFF4DD0E1,
            0xFF4DB6AC, 0xFF81C784, 0xFFAED581, 0xFFFFB74D,
            0xFFFF8A65, 0xFFA1887F, 0xFF90A4AE
        };
        return colors[Math.abs(hash) % colors.length];
    }
    
    private void showAppMenu(final JSONObject app, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add("📂  Открыть");
        popup.getMenu().add("📌  Создать ярлык");
        
        String type = app.optString("type", "file");
        if (type.equals("file")) {
            popup.getMenu().add("🔄  Обновить");
        }
        
        popup.getMenu().add("✏️  Переименовать");
        popup.getMenu().add("🔐  Разрешения");
        popup.getMenu().add("🗑  Удалить");
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                String t = item.getTitle().toString();
                if (t.contains("Открыть")) {
                    openApp(app);
                } else if (t.contains("ярлык")) {
                    try {
                        createShortcut(app.getString("name"),
                            app.optString("path", ""),
                            app.optString("url", ""),
                            app.optString("type", "file"));
                    } catch (Exception e) {}
                } else if (t.contains("Обновить")) {
                    confirmUpdate(app);
                } else if (t.contains("Переименовать")) {
                    renameApp(app);
                } else if (t.contains("Разрешения")) {
                    showAppPermissions(app);
                } else if (t.contains("Удалить")) {
                    confirmDelete(app);
                }
                return true;
            }
        });
        popup.show();
    }
    
    private void openApp(JSONObject app) {
        try {
            Intent intent = new Intent(this, WebAppActivity.class);
            if (app.optString("type", "file").equals("url")) {
                intent.putExtra("url", app.optString("url", ""));
            } else {
                intent.putExtra("filePath", app.optString("path", ""));
            }
            intent.putExtra("appName", app.getString("name"));
            intent.putExtra("perm_camera", app.optBoolean("perm_camera", true));
            intent.putExtra("perm_mic", app.optBoolean("perm_mic", true));
            intent.putExtra("perm_notif", app.optBoolean("perm_notif", true));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void renameApp(final JSONObject app) {
        final EditText input = new EditText(this);
        try {
            input.setText(app.getString("name"));
        } catch (Exception e) {}
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        new AlertDialog.Builder(this)
            .setTitle("Переименовать")
            .setView(input)
            .setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    try {
                        String path = app.optString("path", "");
                        String url = app.optString("url", "");
                        JSONArray apps = getSavedApps();
                        for (int i = 0; i < apps.length(); i++) {
                            JSONObject current = apps.getJSONObject(i);
                            boolean match = (!path.isEmpty() && current.optString("path", "").equals(path))
                                         || (!url.isEmpty() && current.optString("url", "").equals(url));
                            if (match) {
                                current.put("name", newName);
                                break;
                            }
                        }
                        prefs.edit().putString("apps", apps.toString()).apply();
                        loadApps();
                    } catch (Exception e) {}
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void showAppPermissions(final JSONObject app) {
        try {
            String name = app.getString("name");
            final boolean[] perms = {
                app.optBoolean("perm_camera", true),
                app.optBoolean("perm_mic", true),
                app.optBoolean("perm_notif", true)
            };
            String[] labels = {"Камера", "Микрофон", "Уведомления"};
            
            new AlertDialog.Builder(this)
                .setTitle("Разрешения: " + name)
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
                            JSONArray apps = getSavedApps();
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
                            Toast.makeText(MainActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {}
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
        } catch (Exception e) {}
    }
    
    private void confirmUpdate(final JSONObject app) {
        new AlertDialog.Builder(this)
            .setTitle("Обновить приложение?")
            .setMessage("Файл будет заменён новой версией.\n\n✅ Данные сохранятся.\n\nПродолжить?")
            .setPositiveButton("Обновить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    openFilePicker(true, app);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void confirmDelete(final JSONObject app) {
        new AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage("⚠️ Все данные приложения будут потеряны.\n\nПродолжить?")
            .setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    deleteApp(app);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    
    private void createShortcut(String name, String path, String url, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                ShortcutManager sm = getSystemService(ShortcutManager.class);
                if (sm != null && sm.isRequestPinShortcutSupported()) {
                    Intent intent = new Intent(this, WebAppActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    if (type.equals("url")) intent.putExtra("url", url);
                    else intent.putExtra("filePath", path);
                    intent.putExtra("appName", name);
                    
                    ShortcutInfo shortcut = new ShortcutInfo.Builder(this,
                        "lapp_" + name + "_" + System.currentTimeMillis())
                        .setShortLabel(name)
                        .setLongLabel(name)
                        .setIcon(Icon.createWithResource(this, R.drawable.ic_launcher))
                        .setIntent(intent)
                        .build();
                    
                    sm.requestPinShortcut(shortcut, null);
                    Toast.makeText(this, "Запрос отправлен", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void deleteApp(JSONObject app) {
        try {
            String path = app.optString("path", "");
            String url = app.optString("url", "");
            
            if (!path.isEmpty()) {
                File file = new File(path);
                File parent = file.getParentFile();
                if (parent != null && parent.exists()) deleteRecursive(parent);
            }
            
            JSONArray apps = getSavedApps();
            JSONArray newApps = new JSONArray();
            for (int i = 0; i < apps.length(); i++) {
                JSONObject current = apps.getJSONObject(i);
                boolean matchPath = !path.isEmpty() && current.optString("path", "").equals(path);
                boolean matchUrl = !url.isEmpty() && current.optString("url", "").equals(url);
                if (!matchPath && !matchUrl) newApps.put(current);
            }
            prefs.edit().putString("apps", newApps.toString()).apply();
            loadApps();
            Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        file.delete();
    }
}
