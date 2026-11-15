package cn.mediasync.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import cn.mediasync.app.core.Ui;
import cn.mediasync.app.ui.Page;
import cn.mediasync.app.ui.PageFiles;
import cn.mediasync.app.ui.PageHome;
import cn.mediasync.app.ui.PageLive;
import cn.mediasync.app.ui.PageSettings;
import cn.mediasync.app.ui.PageSubscribe;
import cn.mediasync.app.ui.PageTasks;
import cn.mediasync.app.ui.PageWeb;

public class MainActivity extends Activity {
    public static final int TAB_PARSE = 0, TAB_TASKS = 1, TAB_SUB = 2, TAB_LIVE = 3,
            TAB_FILES = 4, PAGE_WEB = 5, PAGE_SETTINGS = 6;

    public App app;
    public String pendingShare;

    private FrameLayout container;
    private TextView statusBar;
    private View[] tabs;
    private Page[] pages;
    private int current = -1;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable ticker;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        app = (App) getApplication();
        setTheme(app.cfg.dark() ? R.style.Theme_MediaSync_Dark : R.style.Theme_MediaSync_Light);
        setContentView(R.layout.activity_main);
        Ui.applyTheme(findViewById(android.R.id.content));

        container = findViewById(R.id.pageContainer);
        statusBar = findViewById(R.id.statusBar);
        tabs = new View[]{
                findViewById(R.id.tabParse), findViewById(R.id.tabTasks),
                findViewById(R.id.tabSubscribe), findViewById(R.id.tabLive),
                findViewById(R.id.tabFiles)
        };
        for (int i = 0; i < tabs.length; i++) {
            final int x = i;
            tabs[i].setOnClickListener(v -> showPage(x));
        }

        pages = new Page[]{
                new PageHome(this), new PageTasks(this), new PageSubscribe(this),
                new PageLive(this), new PageFiles(this), new PageWeb(this),
                new PageSettings(this)
        };
        for (Page p : pages) {
            p.setVisibility(View.GONE);
            container.addView(p, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        findViewById(R.id.btnServer).setOnClickListener(v -> openWeb(""));
        findViewById(R.id.btnSettings).setOnClickListener(v -> showPage(PAGE_SETTINGS));

        updateStatus();
        handleIntent(getIntent());
        showPage(TAB_PARSE);
        requestNotifyPermission();
        startTicker();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        handleIntent(i);
        if (current == TAB_PARSE) pages[TAB_PARSE].onShow();
    }

    private void handleIntent(Intent i) {
        if (i != null && Intent.ACTION_SEND.equals(i.getAction()) && "text/plain".equals(i.getType())) {
            String t = i.getStringExtra(Intent.EXTRA_TEXT);
            if (t != null && !t.trim().isEmpty()) pendingShare = t.trim();
        }
    }

    private void requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void startTicker() {
        ticker = new Runnable() {
            @Override public void run() {
                if (current == TAB_TASKS) pages[TAB_TASKS].onShow();
                h.postDelayed(this, 1000);
            }
        };
        h.postDelayed(ticker, 1000);
    }

    @Override
    protected void onDestroy() {
        if (ticker != null) h.removeCallbacks(ticker);
        super.onDestroy();
    }

    public void showPage(int i) {
        if (i < 0 || i >= pages.length) return;
        if (current == i) { pages[i].onShow(); return; }
        if (current >= 0) pages[current].setVisibility(View.GONE);
        current = i;
        pages[i].setVisibility(View.VISIBLE);
        pages[i].onShow();
        updateStatus();
        for (int t = 0; t < tabs.length; t++) tintTab(t, t == i);
    }

    private void tintTab(int idx, boolean selected) {
        ViewGroup g = (ViewGroup) tabs[idx];
        int c = Ui.color(this, selected ? R.attr.msAccent : R.attr.msSub);
        for (int k = 0; k < g.getChildCount(); k++) {
            View v = g.getChildAt(k);
            if (v instanceof ImageView) ((ImageView) v).setColorFilter(c, PorterDuff.Mode.SRC_IN);
            else if (v instanceof TextView) ((TextView) v).setTextColor(c);
        }
    }

    public void updateStatus() {
        String s = app.cfg.server();
        statusBar.setText(s.isEmpty()
                ? "本地模式 · 通用媒体直链可直接下载；平台解析请连接自托管服务"
                : "已连接 " + s + "  ·  点击右上「完整版」使用完整功能");
    }

    /** 打开内嵌的完整 Web 界面；未配置服务地址时先引导填写。 */
    public void openWeb(String path) {
        final String p = path == null ? "" : path;
        if (app.cfg.server().isEmpty()) {
            Ui.prompt(this, "连接 MediaSync 服务", "例如 http://192.168.1.10:4399", "", v -> {
                if (v.isEmpty()) return;
                app.cfg.server(v);
                app.cfg.webPath(p);
                updateStatus();
                showPage(PAGE_WEB);
            });
            return;
        }
        app.cfg.webPath(p);
        showPage(PAGE_WEB);
    }

    public void toast(String s) { Ui.toast(this, s); }

    public int currentPage() { return current; }

    @Override
    public void onBackPressed() {
        if (current == PAGE_WEB && pages[PAGE_WEB].onBack()) return;
        if (current != TAB_PARSE) { showPage(TAB_PARSE); return; }
        super.onBackPressed();
    }
}
