package cn.mediasync.app.ui;

import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Analyzer;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.Ui;

/** 媒体解析：单链接 / 批量链接、平台识别、画质选择、加入下载队列。 */
public class PageHome extends Page {
    private EditText urlInput;
    private TextView platformHint;
    private LinearLayout resultArea, quickGrid;

    public PageHome(MainActivity a) { super(a, R.layout.page_home); }

    @Override
    protected void init() {
        urlInput = $(R.id.urlInput);
        platformHint = $(R.id.platformHint);
        resultArea = $(R.id.resultArea);
        quickGrid = $(R.id.quickGrid);

        urlInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { updateHint(); }
            public void afterTextChanged(Editable s) { }
        });

        $(R.id.btnPaste).setOnClickListener(v -> {
            try {
                ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence t = cm.getPrimaryClip().getItemAt(0).getText();
                    if (t != null) { urlInput.setText(t.toString().trim()); updateHint(); }
                } else Ui.toast(act, "剪贴板为空");
            } catch (Exception e) { Ui.toast(act, "读取剪贴板失败"); }
        });

        $(R.id.btnExample).setOnClickListener(v -> {
            urlInput.setText("https://media.w3.org/2010/05/sintel/trailer.mp4");
            updateHint();
            Ui.toast(act, "已填入公开示例直链，可直接解析下载");
        });

        $(R.id.btnClear).setOnClickListener(v -> {
            urlInput.setText("");
            resultArea.removeAllViews();
            updateHint();
        });

        $(R.id.btnAnalyze).setOnClickListener(v -> analyze());
        $(R.id.btnBatch).setOnClickListener(v -> batch());
        buildQuick();
    }

    @Override
    public void onShow() {
        if (act.pendingShare != null) {
            String s = act.pendingShare;
            act.pendingShare = null;
            urlInput.setText(s);
            updateHint();
            Ui.toast(act, "已填入分享链接，点击「解析链接」继续");
        }
    }

    private void updateHint() {
        String first = firstLink();
        if (first.isEmpty()) platformHint.setText("等待输入链接");
        else platformHint.setText("识别平台：" + Platforms.detect(first)
                + (Platforms.isDirect(first) ? "  ·  通用直链，可本地下载" : "  ·  需服务端解析"));
    }

    private String firstLink() {
        String s = urlInput.getText() == null ? "" : urlInput.getText().toString();
        for (String line : s.split("\\n")) if (!line.trim().isEmpty()) return line.trim();
        return "";
    }

    private void analyze() {
        String link = firstLink();
        if (link.isEmpty()) { Ui.toast(act, "请先粘贴链接"); return; }
        resultArea.removeAllViews();
        TextView loading = Ui.text(act, "正在解析…", 13, R.attr.msSub, false);
        loading.setPadding(Ui.dp(act, 4), Ui.dp(act, 8), 0, Ui.dp(act, 8));
        resultArea.addView(loading);

        Analyzer.analyze(act, link, new Analyzer.Callback() {
            @Override public void ok(Analyzer.Result r) {
                resultArea.removeAllViews();
                render(r);
                App.I.db.addHistory(r.title, r.url, r.platform);
            }
            @Override public void fail(String m) {
                resultArea.removeAllViews();
                TextView t = Ui.text(act, m, 13, R.attr.msText, false);
                t.setPadding(Ui.dp(act, 4), Ui.dp(act, 8), 0, 0);
                resultArea.addView(t);
            }
        });
    }

    private void render(Analyzer.Result r) {
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setTag("card");
        int pad = Ui.dp(act, 14);
        card.setPadding(pad, pad, pad, pad);

        TextView title = Ui.text(act, r.title, 15, R.attr.msText, true);
        card.addView(title);

        TextView meta = Ui.text(act, r.platform + "  ·  " + (r.direct ? "通用直链" : "平台链接")
                + "\n" + r.url, 11, R.attr.msSub, false);
        meta.setSingleLine(false);
        meta.setMaxLines(3);
        meta.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        meta.setPadding(0, Ui.dp(act, 5), 0, 0);
        card.addView(meta);

        if (r.note != null && !r.note.isEmpty()) {
            TextView note = Ui.text(act, r.note, 12, R.attr.msText, false);
            note.setTextColor(Ui.color(act, R.attr.msAccent));
            note.setLineSpacing(Ui.dp(act, 3), 1f);
            note.setPadding(0, Ui.dp(act, 10), 0, Ui.dp(act, 4));
            card.addView(note);
        }

        if (r.formats.isEmpty()) {
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(Ui.text(act, "暂无可下载格式", 12, R.attr.msSub, false));
            TextView open = Ui.action(act, "在完整版中打开", v -> act.openWeb(""));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = Ui.dp(act, 10);
            row.addView(open, lp);
            card.addView(row);
        } else {
            for (Analyzer.Format f : r.formats) {
                final Analyzer.Format fmt = f;
                LinearLayout line = new LinearLayout(act);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(android.view.Gravity.CENTER_VERTICAL);
                line.setPadding(0, Ui.dp(act, 8), 0, 0);

                TextView label = Ui.text(act, f.label + "  " + f.ext, 13, R.attr.msText, false);
                label.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                line.addView(label);
                line.addView(Ui.action(act, "下载", v -> {
                    String t = r.title + (r.formats.size() > 1 ? " - " + fmt.label : "");
                    App.I.engine.enqueue(t, fmt.url, r.platform, fmt.label);
                    Ui.toast(act, "已加入下载队列，可在「任务」查看进度");
                }));
                card.addView(line);
            }
            if (r.formats.size() > 1) {
                TextView all = Ui.action(act, "全部加入下载队列", v -> {
                    for (Analyzer.Format f : r.formats) {
                        App.I.engine.enqueue(r.title + " - " + f.label, f.url, r.platform, f.label);
                    }
                    Ui.toast(act, "已加入 " + r.formats.size() + " 个任务");
                    act.showPage(MainActivity.TAB_TASKS);
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = Ui.dp(act, 12);
                card.addView(all, lp);
            }
        }

        Ui.applyTheme(card);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resultArea.addView(card, clp);
    }

    private void batch() {
        String raw = urlInput.getText() == null ? "" : urlInput.getText().toString();
        int n = 0;
        for (String line : raw.split("\\n")) {
            if (line.trim().isEmpty()) continue;
            String link = Platforms.normalize(line);
            App.I.engine.enqueue(Platforms.fileName(link), link, Platforms.detect(link), "默认画质");
            n++;
        }
        if (n == 0) { Ui.toast(act, "请先输入链接（每行一个）"); return; }
        Ui.toast(act, "已加入 " + n + " 个任务；平台链接需服务端解析后才有内容");
        act.showPage(MainActivity.TAB_TASKS);
    }

    private void buildQuick() {
        String[][] items = {
                {"完整版 Web 界面", "使用 MediaSync 全部功能（需自托管服务）", "web"},
                {"聚合搜索 / 关注收藏", "跨平台搜索与收藏夹浏览", "search"},
                {"订阅同步", "定时检查更新并自动下载", "sub"},
                {"直播监控录制", "开播状态监控与录制管理", "live"},
                {"文件浏览", "查看、播放与分享已保存文件", "files"},
                {"下载任务", "队列进度、暂停与重试", "tasks"}
        };
        for (String[] it : items) {
            final String key = it[2];
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setTag("card");
            int p = Ui.dp(act, 14);
            row.setPadding(p, Ui.dp(act, 12), p, Ui.dp(act, 12));

            LinearLayout left = new LinearLayout(act);
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            left.addView(Ui.text(act, it[0], 14, R.attr.msText, true));
            TextView d = Ui.text(act, it[1], 11, R.attr.msSub, false);
            d.setPadding(0, Ui.dp(act, 3), 0, 0);
            left.addView(d);
            row.addView(left);
            row.addView(Ui.text(act, "›", 20, R.attr.msSub, false));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(act, 8);
            quickGrid.addView(row, lp);
            row.setClickable(true);
            row.setOnClickListener(v -> {
                switch (key) {
                    case "web": act.openWeb(""); break;
                    case "search": act.openWeb("/search"); break;
                    case "sub": act.showPage(MainActivity.TAB_SUB); break;
                    case "live": act.showPage(MainActivity.TAB_LIVE); break;
                    case "files": act.showPage(MainActivity.TAB_FILES); break;
                    default: act.showPage(MainActivity.TAB_TASKS); break;
                }
            });
        }
        Ui.applyTheme(quickGrid);
    }
}
