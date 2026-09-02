package com.smartcare360.app;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View layoutError;
    private Button btnRetry;

    private ValueCallback<Uri[]> fileUploadCallback;
    private ActivityResultLauncher<Intent> fileChooserLauncher;

    private String targetUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        targetUrl = getString(R.string.web_url);

        webView = findViewById(R.id.web_view);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        progressBar = findViewById(R.id.progress_bar);
        layoutError = findViewById(R.id.layout_error);
        btnRetry = findViewById(R.id.btn_retry);

        initFileChooserLauncher();
        initWebView();
        initSwipeRefresh();
        initBackNavigation();

        btnRetry.setOnClickListener(v -> retryConnection());

        if (savedInstanceState == null) {
            loadInitialUrl();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void initFileChooserLauncher() {
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (fileUploadCallback == null) return;

                    Uri[] results = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            results = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                results[i] = result.getData().getClipData().getItemAt(i).getUri();
                            }
                        } else if (result.getData().getData() != null) {
                            results = new Uri[]{result.getData().getData()};
                        }
                    }
                    fileUploadCallback.onReceiveValue(results);
                    fileUploadCallback = null;
                }
        );
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        String defaultUa = settings.getUserAgentString();
        settings.setUserAgentString(defaultUa + " SmartCare360App/1.0");

        // Maintain persistent login cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrlScheme(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                layoutError.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showErrorView();
                }
            }
        });

        // WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;

                Intent contentSelectionIntent = fileChooserParams.createIntent();
                try {
                    fileChooserLauncher.launch(contentSelectionIntent);
                } catch (ActivityNotFoundException e) {
                    fileUploadCallback = null;
                    Toast.makeText(MainActivity.this, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        // Download Listener for invoices, lab reports, and files
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription(getString(R.string.download_started));

                String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(filename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), getString(R.string.download_started) + " " + filename, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Fallback: Open download in external browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean handleUrlScheme(String url) {
        if (url == null) return false;

        // Dial phone numbers
        if (url.startsWith("tel:")) {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
            return true;
        }

        // Send email
        if (url.startsWith("mailto:")) {
            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
            return true;
        }

        // WhatsApp direct link
        if (url.startsWith("whatsapp:") || url.contains("api.whatsapp.com") || url.contains("wa.me")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException ignored) {}
        }

        // If it's another domain not related to our app, open in browser
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host != null && !host.contains("onrender.com") && !host.contains("smartcare360") && !host.contains("localhost") && !host.contains("10.0.2.2")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (Exception ignored) {}
        }

        return false;
    }

    private void initSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                webView.reload();
            } else {
                swipeRefresh.setRefreshing(false);
                showErrorView();
            }
        });

        // Prevent pull-to-refresh when user is scrolling inside web page
        webView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (webView.getScrollY() == 0) {
                swipeRefresh.setEnabled(true);
            } else {
                swipeRefresh.setEnabled(false);
            }
        });
    }

    private void initBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (layoutError.getVisibility() == View.VISIBLE) {
                    retryConnection();
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void loadInitialUrl() {
        if (isNetworkAvailable()) {
            webView.loadUrl(targetUrl);
        } else {
            showErrorView();
        }
    }

    private void retryConnection() {
        if (isNetworkAvailable()) {
            layoutError.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            if (webView.getUrl() != null) {
                webView.reload();
            } else {
                webView.loadUrl(targetUrl);
            }
        } else {
            Toast.makeText(this, getString(R.string.no_internet_title), Toast.LENGTH_SHORT).show();
        }
    }

    private void showErrorView() {
        webView.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
