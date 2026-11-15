package cn.mediasync.app.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.Ui;

/** 完整版：内嵌 MediaSync Web UI，获得原项目全部功能。 */
public class PageWeb extends Page {
    private WebView web;
    private TextView address;
    private ProgressBar progress;
    private String lastUrl = "";

    public PageWeb(MainActivity a) { super(a, R.layout.page_web); }

    @Override
    protected void init() {
        address = $(R.id.webAddress);
        progress = $(R.id.webProgress);
        progress.setProgressTintList(ColorStateList.valueOf(Ui.color(act, R.attr.msAccent)));

        $(R.id.btnWebBack).setOnClickListener(v -> act.showPage(MainActivity.TAB_PARSE));
        $(R.id.btnWebRefresh).setOnClickListener(v -> { if (web != null) web.reload(); });

        web = new WebView(act);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 MediaSync-Android/1.0");
        web.setBackgroundColor(Color.TRANSPARENT);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                progress.setVisibility(View.GONE);
                address.setText(url);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req != null && req.isForMainFrame()) {
                    Ui.toast(act, "页面加载失败，请检查服务地址与网络");
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }
        });

        // Web 界面里的下载交给本地下载引擎，享受断点续传与任务管理
        web.setDownloadListener((url, ua, contentDisposition, mime, len) -> {
            String name = Platforms.fileName(url);
            App.I.engine.enqueue(name, url, Platforms.detect(url), "WebView");
            Ui.toast(act, "已加入下载队列：" + name);
        });

        ((FrameLayout) $(R.id.webContainer)).addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void onShow() {
        String base = App.I.cfg.server();
        if (base.isEmpty()) {
            address.setText("未配置 MediaSync 服务地址");
            lastUrl = "";
            web.loadData("<html><body style=\"padding:28px;font-family:sans-serif;"
                    + "color:#888;line-height:1.8\">"
                    + "<h2>尚未连接服务</h2>"
                    + "<p>请在「设置 → 服务连接」中填入自托管 MediaSync 地址（例如 http://192.168.1.10:4399），"
                    + "即可在此使用完整 Web 界面：解析下载、关注收藏、聚合搜索、订阅同步、直播录制与文件浏览。</p>"
                    + "</body></html>", "text/html; charset=utf-8", "UTF-8");
            return;
        }
        String path = App.I.cfg.webPath();
        App.I.cfg.webPath("");
        String full = base + (path.isEmpty() ? "" : (path.startsWith("/") ? path : "/" + path));
        address.setText(full);
        if (!full.equals(lastUrl)) {
            lastUrl = full;
            web.loadUrl(full);
        }
    }

    @Override
    public boolean onBack() {
        if (web != null && web.canGoBack()) { web.goBack(); return true; }
        return false;
    }
}
