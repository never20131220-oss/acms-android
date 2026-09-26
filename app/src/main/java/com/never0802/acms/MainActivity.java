package com.never0802.acms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    public static final String EXTRA_OPEN = "acms_open";
    private static final String BASE_URL = "https://raspy-queen-055e.never0802.workers.dev/";
    private static final int FILE_CHOOSER = 8821;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);
        configureWebView();
        WidgetScheduler.schedule(this);
        loadFromIntent(getIntent());
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " ACMSNative/2026.09.26.6");
        webView.addJavascriptInterface(new NativeBridge(), "ACMSNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) {
                    view.loadUrl(u.toString());
                    return true;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = params.createIntent();
                pick.setType("image/*");
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(pick, FILE_CHOOSER); return true; }
                catch (Exception e) { fileCallback = null; return false; }
            }
        });
    }

    private void loadFromIntent(Intent intent) {
        String open = intent == null ? "" : intent.getStringExtra(EXTRA_OPEN);
        if ((open == null || open.isEmpty()) && intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            if ("acms".equalsIgnoreCase(data.getScheme())) {
                String host = data.getHost();
                if (host != null) open = host;
            }
        }
        String url = BASE_URL;
        if (open != null && !open.isEmpty()) {
            url += "?acms=" + Uri.encode(open);
        }
        webView.loadUrl(url);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadFromIntent(intent);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER || fileCallback == null) return;
        Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    public final class NativeBridge {
        @JavascriptInterface public void setSession(String accessToken, String refreshToken) {
            if (accessToken == null || accessToken.isEmpty()) TokenStore.clearSession(getApplicationContext());
            else TokenStore.saveSession(getApplicationContext(), accessToken, refreshToken == null ? "" : refreshToken);
            WidgetUpdater.refreshAsync(getApplicationContext());
        }

        @JavascriptInterface public void updateCounts(int waiting, int field, int plan) {
            WidgetUpdater.saveAndRender(getApplicationContext(), new WidgetCounts(waiting, field, plan));
        }
    }
}
