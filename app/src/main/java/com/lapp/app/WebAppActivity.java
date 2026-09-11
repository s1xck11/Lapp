package com.lapp.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;

public class WebAppActivity extends Activity {
    
    private WebView webView;
    private boolean permCamera = true;
    private boolean permMic = true;
    private boolean permNotif = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        permCamera = getIntent().getBooleanExtra("perm_camera", true);
        permMic = getIntent().getBooleanExtra("perm_mic", true);
        permNotif = getIntent().getBooleanExtra("perm_notif", true);
        
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
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
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        // Обработка запросов разрешений от сайтов (камера, микрофон)
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
    
    private void handlePermissionRequest(PermissionRequest request) {
        String[] resources = request.getResources();
        boolean allow = false;
        
        for (String res : resources) {
            if (res.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                allow = permCamera;
            } else if (res.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                allow = permMic;
            }
        }
        
        if (allow) {
            request.grant(resources);
        } else {
            request.deny();
            Toast.makeText(this, "Доступ запрещён в настройках Lapp", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
