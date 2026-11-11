package com.lapp.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;
import java.util.*;

public class WebAppActivity extends Activity {
    
    private WebView webView;
    private boolean permCamera = true;
    private boolean permMic = true;
    
    // Маскировка под реальный Chrome Mobile
    private static final String USER_AGENT = 
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        permCamera = getIntent().getBooleanExtra("perm_camera", true);
        permMic = getIntent().getBooleanExtra("perm_mic", true);
        
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        
        // 1. Подмена User-Agent
        settings.setUserAgentString(USER_AGENT);
        
        // Остальные настройки
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        
        // 2. Подключаем мост для скачивания Blob (картинок из WebView)
        webView.addJavascriptInterface(new BlobBridge(), "AndroidBlob");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        handlePermissionRequest(request);
                    }
                });
            }
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, 
                    GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        
        // 3. Обработка скачивания (HTTP ссылки)
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, 
                                        String contentDisposition, 
                                        String mimeType, long contentLength) {
                if (url.startsWith("blob:") || url.startsWith("data:")) {
                    handleBlobDownload(url, mimeType);
                } else {
                    startDownload(url, contentDisposition, mimeType);
                }
            }
        });
        
        String filePath = getIntent().getStringExtra("filePath");
        String url = getIntent().getStringExtra("url");
        
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        } else if (filePath != null && new File(filePath).exists()) {
            webView.loadUrl("file://" + filePath);
        } else {
            webView.loadData("<h1>Файл не найден</h1>", "text/html", "UTF-8");
        }
        
        setContentView(webView);
    }
    
    // === СКАЧИВАНИЕ ОБЫЧНЫХ ССЫЛОК (HTTP/HTTPS) ===
    private void startDownload(String url, String contentDisposition, String mimeType) {
        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", USER_AGENT);
            request.setTitle(fileName);
            request.setDescription("Загрузка из Lapp");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // Сохраняем в Download/Lapp
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, 
                "Lapp/" + fileName
            );
            
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);
            
            Toast.makeText(this, "Скачивание в Download/Lapp", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    // === СКАЧИВАНИЕ BLOB (ГЕНЕРИРУЕМЫЕ ДАННЫЕ ИЗ JS) ===
    private void handleBlobDownload(String blobUrl, String mimeType) {
        Toast.makeText(this, "Подготовка файла...", Toast.LENGTH_SHORT).show();
        
        String js = 
            "javascript:(function(){" +
            "  fetch('" + blobUrl + "')" +
            "    .then(r => r.blob())" +
            "    .then(b => {" +
            "      const reader = new FileReader();" +
            "      reader.onloadend = () => {" +
            "        const base64 = reader.result.split(',')[1];" +
            "        AndroidBlob.saveFile(base64, '" + (mimeType != null ? mimeType : "application/octet-stream") + "');" +
            "      };" +
            "      reader.readAsDataURL(b);" +
            "    })" +
            "    .catch(e => AndroidBlob.onError(e.toString()));" +
            "})()";
        
        webView.evaluateJavascript(js, null);
    }
    
    // === JS-МОСТ ДЛЯ СОХРАНЕНИЯ BLOB В DOWNLOAD/LAPP (Android 10+) ===
    public class BlobBridge {
        @android.webkit.JavascriptInterface
        public void saveFile(String base64Data, String mimeType) {
            try {
                byte[] fileBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                
                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (ext == null) ext = "bin";
                String fileName = "Lapp_" + System.currentTimeMillis() + "." + ext;
                
                // На Android 10+ используем MediaStore, чтобы файл попал в Download/Lapp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Lapp");
                    
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        os.write(fileBytes);
                        os.close();
                    }
                } else {
                    // Для старых версий (до 10) пишем напрямую
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "Lapp");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(fileBytes);
                    fos.close();
                }
                
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(WebAppActivity.this, "Сохранено в Download/Lapp", Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                final String err = e.getMessage();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(WebAppActivity.this, "Ошибка: " + err, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        
        @android.webkit.JavascriptInterface
        public void onError(String error) {
            final String err = error;
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(WebAppActivity.this, "Ошибка JS: " + err, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    private void handlePermissionRequest(PermissionRequest request) {
        String[] resources = request.getResources();
        boolean allow = false;
        for (String res : resources) {
            if (res.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) allow = permCamera;
            else if (res.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) allow = permMic;
        }
        if (allow) request.grant(resources);
        else {
            request.deny();
            Toast.makeText(this, "Доступ запрещён в настройках Lapp", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}