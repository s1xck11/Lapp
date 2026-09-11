package com.lapp.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.graphics.*;
import java.io.*;

public class WebAppActivity extends Activity {
    
    private WebView webView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        
        String filePath = getIntent().getStringExtra("filePath");
        
        if (filePath != null && new File(filePath).exists()) {
            webView.loadUrl("file://" + filePath);
        } else {
            webView.loadData("<h1>Файл не найден</h1>", "text/html", "UTF-8");
        }
        
        setContentView(webView);
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
