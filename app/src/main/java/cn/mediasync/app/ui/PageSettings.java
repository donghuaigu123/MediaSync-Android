package cn.mediasync.app.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Analyzer;
import cn.mediasync.app.core.Notify;
import cn.mediasync.app.core.ScanReceiver;
import cn.mediasync.app.core.Ui;

/** 设置中心：服务连接、下载、自动化、通知、外观、备份与关于。 */
public class PageSettings extends Page {
    private LinearLayout root;
    private EditText etServer, etApi, etDir, etProxy, etTgToken, etTgChat, etWecom;
    private TextView tvConcurrency, tvQuality, tvInterval;

    public PageSettings(MainActivity a) { super(a, R.layout.page_settings); }

    @Override
    protected void init() {
        root = $(R.id.settingsRoot);
        buildServer();
        buildDownload();
        buildAutomation();
        buildNotify();
        buildAppearance();
        buildBackup();
        buildAbout();

        TextView save = new TextView(act);
        save.setText("保存设置");
        save.setTextSize(15);
        save.setGravity(android.view.Gravity.CENTER);
        save.setTextColor(0xFFFFFFFF);
        save.setPadding(0, Ui.dp(act, 13), 0, Ui.dp(act, 13));
        save.setTag("brand");
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(act, 20);
        root.addView(save, slp);
        save.setOnClickListener(v -> { saveAll(); act.updateStatus(); Ui.toast(act, "设置已保存"); });

        Ui.applyTheme(root);
    }

    private void buildServer() {
        LinearLayout g = group("服务连接");
        etServer = field(g, "MediaSync 服务地址", App.I.cfg.server(), "http://192.168.1.10:4399");
        etApi = field(g, "解析接口路径", App.I.cfg.parseApi(), "/api/parse");
        clickRow(g, "测试连接", "检查服务是否可达", v -> testServer());
        clickRow(g, "打开完整版 Web 界面", "使用 MediaSync 全部功能", x -> {
            saveAll(); act.updateStatus(); act.openWeb("");
        });
    }

    private void buildDownload() {
        LinearLayout g = group("下载");
        tvConcurrency = clickRow(g, "并发下载数", "当前 " + App.I.cfg.concurrency() + " 个", v -> choose("并发下载数",
                new String[]{"1", "2", "3", "4", "5"}, App.I.cfg.concurrency() - 1, (which, val) -> {
                    App.I.cfg.concurrency(Integer.parseInt(val));
                    App.I.engine.setConcurrency(Integer.parseInt(val));
                    if (tvConcurrency != null) tvConcurrency.setText("当前 " + val + " 个");
                }));
        Switch swWifi = toggleRow(g, "仅 WiFi 下载", "移动网络下自动暂停下载任务", App.I.cfg.wifiOnly());
        swWifi.setOnCheckedChangeListener((b, on) -> App.I.cfg.wifiOnly(on));

        String[] qualities = {"最高画质", "1080P", "720P", "480P", "仅音频"};
        tvQuality = clickRow(g, "默认画质", "当前 " + App.I.cfg.quality(), v -> choose("默认画质",
                qualities, idxOf(qualities, App.I.cfg.quality()), (which, val) -> {
                    App.I.cfg.quality(val);
                    if (tvQuality != null) tvQuality.setText("当前 " + val);
                }));
        etDir = field(g, "自定义保存目录（留空使用应用私有目录）", App.I.cfg.downloadDir(),
                App.I.downloadDir().getAbsolutePath());
        clickRow(g, "复制保存目录路径", App.I.downloadDir().getAbsolutePath(), x -> {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("路径", App.I.downloadDir().getAbsolutePath()));
            Ui.toast(act, "路径已复制");
        });
    }

    private void buildAutomation() {
        LinearLayout g = group("自动化");
        String[] intervals = {"15 分钟", "30 分钟", "60 分钟", "180 分钟", "360 分钟"};
        tvInterval = clickRow(g, "订阅扫描间隔", "当前 " + App.I.cfg.scanInterval() + " 分钟",
                v -> choose("扫描间隔", intervals,
                        idxOf(intervals, App.I.cfg.scanInterval() + " 分钟"), (which, val) -> {
                            int m = Integer.parseInt(val.replace(" 分钟", ""));
                            App.I.cfg.scanInterval(m);
                            ScanReceiver.schedule(act);
                            if (tvInterval != null) tvInterval.setText("当前 " + val);
                        }));
        clickRow(g, "立即扫描一次", "手动触发订阅检查", x -> {
            act.sendBroadcast(new Intent(act, ScanReceiver.class).setAction(ScanReceiver.ACTION));
            Ui.toast(act, "已触发扫描");
        });
        etProxy = field(g, "HTTP / SOCKS 代理", App.I.cfg.proxy(), "http://127.0.0.1:7890 或 socks5://host:port");
    }

    private void buildNotify() {
        LinearLayout g = group("通知与远程操作");
        Switch swNotify = toggleRow(g, "下载与事件通知", "下载完成、订阅扫描、失败提醒", App.I.cfg.notifyEnabled());
        swNotify.setOnCheckedChangeListener((b, on) -> App.I.cfg.notifyEnabled(on));
        etTgToken = field(g, "Telegram Bot Token", App.I.cfg.tgToken(), "123456:ABC-DEF…");
        etTgChat = field(g, "Telegram Chat ID", App.I.cfg.tgChat(), "用于 /analyze /download /status 命令");
        etWecom = field(g, "企业微信机器人 Webhook", App.I.cfg.wecom(), "https://qyapi.weixin.qq.com/…");
        clickRow(g, "发送测试通知", "验证本机通知渠道是否可用", x ->
                Notify.event(act, "媒体同步", "测试通知：渠道工作正常"));
    }

    private void buildAppearance() {
        LinearLayout g = group("外观");
        Switch swDark = toggleRow(g, "深色模式", "切换后立即重建界面", App.I.cfg.dark());
        swDark.setOnCheckedChangeListener((b, on) -> {
            if (on == App.I.cfg.dark()) return;
            saveAll();
            App.I.cfg.dark(on);
            act.recreate();
        });
    }

    private void buildBackup() {
        LinearLayout g = group("数据备份");
        clickRow(g, "导出配置与数据", "保存为 JSON 并复制到剪贴板", v -> exportBackup());
        clickRow(g, "从剪贴板导入", "恢复设置、订阅、直播间与历史", v -> importBackup());
        clickRow(g, "清空解析历史", "不影响已下载文件", v -> Ui.confirm(act, "清空历史", "确定清空解析历史记录？",
                () -> { App.I.db.clearHistory(); Ui.toast(act, "已清空"); }));
    }

    private void buildAbout() {
        LinearLayout g = group("关于");
        TextView t = Ui.text(act,
                "媒体同步 MediaSync Android  v1.0.0\n\n"
                        + "一款独立实现的 Android 客户端，复刻 MediaSync 公开介绍的功能设计：多平台媒体解析、批量下载、"
                        + "订阅同步、直播监控录制、文件浏览、通知与数据备份。\n\n"
                        + "原项目 lincolnpark2000/MediaSync 未在仓库中公开源代码，也未附带开源许可证；"
                        + "本项目不包含其任何代码、图片或素材，全部功能为独立编写。\n\n"
                        + "本项目以 MIT 许可证开源。请仅下载和保存自己创作、获授权或依法可使用的内容。",
                12, R.attr.msSub, false);
        t.setLineSpacing(Ui.dp(act, 4), 1f);
        t.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 12));
        g.addView(t);
    }

    // ---------- 构建工具 ----------
    private LinearLayout group(String name) {
        TextView tv = Ui.text(act, name, 12, R.attr.msSub, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 16);
        lp.leftMargin = Ui.dp(act, 2);
        root.addView(tv, lp);

        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setTag("card");
        box.setPadding(Ui.dp(act, 14), Ui.dp(act, 4), Ui.dp(act, 14), Ui.dp(act, 8));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = Ui.dp(act, 8);
        root.addView(box, lp2);
        return box;
    }

    private EditText field(LinearLayout box, String label, String value, String hint) {
        TextView l = Ui.text(act, label, 11, R.attr.msSub, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 10);
        box.addView(l, lp);

        EditText e = new EditText(act);
        e.setText(value == null ? "" : value);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(13);
        e.setTextColor(Ui.color(act, R.attr.msText));
        e.setHintTextColor(Ui.color(act, R.attr.msSub));
        e.setPadding(Ui.dp(act, 12), Ui.dp(act, 9), Ui.dp(act, 12), Ui.dp(act, 9));
        e.setBackground(Ui.round(act, R.attr.msInput, 9, 0));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = Ui.dp(act, 6);
        box.addView(e, lp2);
        return e;
    }

    private TextView clickRow(LinearLayout box, String label, String value, View.OnClickListener l) {
        LinearLayout line = new LinearLayout(act);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(android.view.Gravity.CENTER_VERTICAL);
        line.setPadding(0, Ui.dp(act, 11), 0, Ui.dp(act, 11));

        LinearLayout left = new LinearLayout(act);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        left.addView(Ui.text(act, label, 13, R.attr.msText, false));
        TextView valView = null;
        if (value != null && !value.isEmpty()) {
            valView = Ui.text(act, value, 11, R.attr.msSub, false);
            valView.setMaxLines(2);
            valView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            left.addView(valView);
        }
        line.addView(left);
        line.addView(Ui.text(act, "›", 18, R.attr.msSub, false));
        line.setClickable(true);
        line.setOnClickListener(l);
        box.addView(line);
        divider(box);
        return valView;
    }

    private static int idxOf(String[] a, String s) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(s)) return i;
        return 0;
    }

    private Switch toggleRow(LinearLayout box, String label, String desc, boolean on) {
        LinearLayout line = new LinearLayout(act);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(android.view.Gravity.CENTER_VERTICAL);
        line.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 10));

        LinearLayout left = new LinearLayout(act);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        left.addView(Ui.text(act, label, 13, R.attr.msText, false));
        left.addView(Ui.text(act, desc, 11, R.attr.msSub, false));
        line.addView(left);

        Switch sw = new Switch(act);
        sw.setChecked(on);
        line.addView(sw);
        box.addView(line);
        divider(box);
        return sw;
    }

    private void divider(LinearLayout box) {
        View v = new View(act);
        v.setBackgroundColor(Ui.color(act, R.attr.msLine));
        box.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(act, 0.6f))));
    }

    private void choose(String title, String[] opts, int checked, OnPick cb) {
        new AlertDialog.Builder(act).setTitle(title)
                .setSingleChoiceItems(opts, checked, (d, w) -> { cb.onPick(w, opts[w]); d.dismiss(); })
                .setNegativeButton("取消", null).show();
    }

    private interface OnPick { void onPick(int which, String value); }

    // ---------- 行为 ----------
    private void saveAll() {
        App.I.cfg.server(etServer.getText().toString());
        App.I.cfg.parseApi(etApi.getText().toString());
        App.I.cfg.downloadDir(etDir.getText().toString());
        App.I.cfg.proxy(etProxy.getText().toString());
        App.I.cfg.tgToken(etTgToken.getText().toString());
        App.I.cfg.tgChat(etTgChat.getText().toString());
        App.I.cfg.wecom(etWecom.getText().toString());
    }

    private void testServer() {
        saveAll();
        String s = App.I.cfg.server();
        if (s.isEmpty()) { Ui.toast(act, "请先填写服务地址"); return; }
        Ui.toast(act, "正在连接 " + s + " …");
        new Thread(() -> {
            try {
                String body = Analyzer.httpGet(s, 12000);
                Ui.toast(act, "连接成功，服务返回 " + body.length() + " 字节");
            } catch (Exception e) {
                Ui.toast(act, "连接失败：" + (e.getMessage() == null ? "无法访问" : e.getMessage()));
            }
        }, "mediasync-test").start();
    }

    private void exportBackup() {
        try {
            JSONObject o = new JSONObject(App.I.db.exportJson());
            o.put("settings", new JSONObject(App.I.cfg.asJson()));
            String json = o.toString(2);
            String name = "mediasync-backup-" +
                    new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(new Date()) + ".json";
            File f = new File(App.I.downloadDir(), name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(json.getBytes("UTF-8"));
            fos.close();
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("MediaSync 备份", json));
            Ui.toast(act, "已导出 " + name + "，并复制到剪贴板");
        } catch (Exception e) { Ui.toast(act, "导出失败：" + e.getMessage()); }
    }

    private void importBackup() {
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || cm.getPrimaryClip() == null || cm.getPrimaryClip().getItemCount() == 0) {
                Ui.toast(act, "剪贴板为空"); return;
            }
            CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
            if (t == null || !t.toString().trim().startsWith("{")) {
                Ui.toast(act, "剪贴板中没有有效的 JSON 备份"); return;
            }
            JSONObject o = new JSONObject(t.toString());
            if (o.has("settings")) App.I.cfg.fromJson(o.getJSONObject("settings").toString());
            int n = App.I.db.importJson(o.toString());
            act.updateStatus();
            Ui.toast(act, "导入完成，恢复 " + n + " 条记录");
        } catch (Exception e) { Ui.toast(act, "导入失败：" + e.getMessage()); }
    }
}
