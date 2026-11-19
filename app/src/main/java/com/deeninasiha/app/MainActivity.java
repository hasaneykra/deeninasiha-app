package com.deeninasiha.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String HOST = "deeninasiha.com";
    private static final String BASE = "https://" + HOST;
    private static final String OFFLINE_PAGE = "file:///android_asset/offline.html";
    private static final int REQ_LOCATION = 11;

    private WebView web;
    private SwipeRefreshLayout swipe;
    private ProgressBar bar;
    private TextView tabHome;
    private TextView tabTools;
    private TextView tabPrayer;

    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        swipe = findViewById(R.id.swipe);
        bar = findViewById(R.id.bar);
        tabHome = findViewById(R.id.tab_home);
        tabTools = findViewById(R.id.tab_tools);
        tabPrayer = findViewById(R.id.tab_prayer);
        TextView tabSearch = findViewById(R.id.tab_search);
        TextView tabShare = findViewById(R.id.tab_share);

        tabHome.setText("🏠\n" + getString(R.string.tab_home));
        tabTools.setText("🧰\n" + getString(R.string.tab_tools));
        tabPrayer.setText("🕌\n" + getString(R.string.tab_prayer));
        tabSearch.setText("🔎\n" + getString(R.string.tab_search));
        tabShare.setText("📤\n" + getString(R.string.tab_share));

        tabHome.setOnClickListener(v -> open(BASE + "/"));
        tabTools.setOnClickListener(v -> open(BASE + "/tools/"));
        tabPrayer.setOnClickListener(v -> open(BASE + "/prayer-times/"));
        tabSearch.setOnClickListener(v -> showSearch());
        tabShare.setOnClickListener(v -> shareCurrent());

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(s.getUserAgentString() + " DeeniNasihaApp/1.0");

        web.setWebViewClient(new AppClient());
        web.setWebChromeClient(new AppChrome());

        swipe.setColorSchemeResources(R.color.emerald);
        swipe.setOnRefreshListener(() -> {
            applyCacheMode();
            String u = web.getUrl();
            if (u == null || u.startsWith("file:")) {
                web.loadUrl(BASE + "/");
            } else {
                web.reload();
            }
        });
        swipe.setOnChildScrollUpCallback((parent, child) -> web.getScrollY() > 0);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            open(startUrl(getIntent()));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        open(startUrl(intent));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }

    private String startUrl(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri d = intent.getData();
            if ("https".equals(d.getScheme()) && HOST.equals(d.getHost())) {
                return d.toString();
            }
        }
        return BASE + "/";
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities c = cm.getNetworkCapabilities(n);
        return c != null && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /** Online: normal HTTP caching. Offline: serve any cached copy so visited pages still open. */
    private void applyCacheMode() {
        web.getSettings().setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    private void open(String url) {
        applyCacheMode();
        web.loadUrl(url);
    }

    private void showSearch() {
        final EditText input = new EditText(this);
        input.setHint(R.string.search_hint);
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.search_title)
                .setView(input)
                .setPositiveButton(R.string.search_go, (d, w) -> runSearch(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .create();
        input.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(input.getText().toString());
                dialog.dismiss();
                return true;
            }
            return false;
        });
        dialog.show();
    }

    private void runSearch(String q) {
        String t = q.trim();
        if (t.isEmpty()) return;
        open(BASE + "/?s=" + Uri.encode(t));
    }

    private void shareCurrent() {
        String url = web.getUrl();
        if (url == null || !url.startsWith("http")) url = BASE + "/";
        String title = web.getTitle();
        String text = (title == null || title.isEmpty()) ? url : title + "\n" + url;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    private void highlight(String url) {
        String path = "";
        if (url != null && url.startsWith("http")) {
            String p = Uri.parse(url).getPath();
            path = p == null ? "" : p;
        }
        boolean home = path.isEmpty() || "/".equals(path);
        mark(tabHome, home);
        mark(tabTools, path.startsWith("/tools"));
        mark(tabPrayer, path.startsWith("/prayer-times"));
    }

    private void mark(TextView t, boolean on) {
        t.setTextColor(ContextCompat.getColor(this, on ? R.color.gold : R.color.mist));
        t.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
    }

    private boolean isOurHost(Uri u) {
        String h = u.getHost();
        return h != null && (h.equals(HOST) || h.endsWith("." + HOST));
    }

    private class AppClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri u = request.getUrl();
            String scheme = u.getScheme();
            if ("file".equals(scheme) || "about".equals(scheme)) return false;
            if (("https".equals(scheme) || "http".equals(scheme)) && isOurHost(u)) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, u));
            } catch (ActivityNotFoundException ignored) {
                // no app can handle this link
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            swipe.setRefreshing(false);
            highlight(url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                swipe.setRefreshing(false);
                view.loadUrl(OFFLINE_PAGE + "?u=" + Uri.encode(request.getUrl().toString()));
            }
        }
    }

    private class AppChrome extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int p) {
            if (p >= 100) {
                bar.setVisibility(View.GONE);
            } else {
                bar.setVisibility(View.VISIBLE);
                bar.setProgress(p);
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
                return;
            }
            geoCallback = callback;
            geoOrigin = origin;
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && geoCallback != null) {
            boolean ok = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) ok = true;
            }
            geoCallback.invoke(geoOrigin, ok, false);
            geoCallback = null;
            geoOrigin = null;
        }
    }
}
