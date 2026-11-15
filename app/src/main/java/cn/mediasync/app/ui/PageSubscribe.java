package cn.mediasync.app.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.util.List;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Analyzer;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.ScanReceiver;
import cn.mediasync.app.core.Ui;
import cn.mediasync.app.db.Db;

/** 订阅同步。 */
public class PageSubscribe extends ListPage {

    public PageSubscribe(MainActivity a) { super(a); }

    @Override protected String title() { return "订阅同步"; }
    @Override protected String desc() { return "定时检查更新；自动下载需连接 MediaSync 服务"; }
    @Override protected String addLabel() { return "＋ 新建订阅"; }
    @Override protected String emptyText() { return "还没有订阅\n点击右上角添加 UP 主、频道或合集链接"; }
    @Override protected List<ContentValues> load() { return App.I.db.subs(); }

    @Override protected String rowTitle(ContentValues r) { return Db.s(r, "title"); }
    @Override protected String rowSub(ContentValues r) {
        return Db.s(r, "platform") + "  ·  " + Db.s(r, "url")
                + "\n上次扫描 " + Ui.time(Db.l(r, "last_scan")) + "  ·  新增 " + Db.i(r, "new_count") + " 条";
    }
    @Override protected String rowTag(ContentValues r) {
        return Db.i(r, "enabled") == 1 ? "已启用  ·  每 " + Db.i(r, "interval") + " 分钟" : "已暂停";
    }

    @Override protected void rowActions(LinearLayout box, ContentValues r) {
        final long id = id(r);
        final boolean on = Db.i(r, "enabled") == 1;
        addBtn(box, "立即扫描", v -> scanOne(id));
        addBtn(box, on ? "暂停" : "启用", v -> { App.I.db.subToggle(id, !on); reload(); });
        addBtn(box, "删除", v -> Ui.confirm(act, "删除订阅", "确定删除「" + Db.s(r, "title") + "」？",
                () -> { App.I.db.delSub(id); reload(); }));
    }

    private void scanOne(long id) {
        Ui.toast(act, "正在扫描…");
        new Thread(() -> {
            int found = 0;
            String err = "";
            if (!App.I.cfg.server().isEmpty()) {
                try {
                    String body = Analyzer.httpGet(
                            App.I.cfg.server() + "/api/subscriptions/scan?id=" + id, 30000);
                    JSONObject o = new JSONObject(body);
                    if (o.has("data") && o.get("data") instanceof JSONObject) o = o.getJSONObject("data");
                    found = o.optInt("new_count", o.optInt("new", 0));
                } catch (Exception e) { err = e.getMessage() == null ? "连接失败" : e.getMessage(); }
            }
            App.I.db.subTouch(id, found);
            String msg = err.isEmpty()
                    ? "扫描完成" + (found > 0 ? "，新增 " + found + " 条内容" : "，暂无更新")
                    : "服务端扫描失败：" + err;
            Ui.toast(act, msg);
            act.runOnUiThread(this::reload);
        }, "mediasync-scan-one").start();
    }

    @Override protected void onAdd() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(act, 18);
        box.setPadding(p, Ui.dp(act, 8), p, 0);
        final EditText name = edit("订阅名称（如：某 UP 主）");
        final EditText url = edit("链接：UP 主 / 频道 / 合集 / 播放列表");
        box.addView(name);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 8);
        box.addView(url, lp);

        new AlertDialog.Builder(act).setTitle("新建订阅").setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String u = Platforms.normalize(url.getText().toString());
                    if (u.isEmpty()) { Ui.toast(act, "请填写链接"); return; }
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) n = Platforms.detect(u) + " 订阅";
                    App.I.db.addSub(n, Platforms.detect(u), u, App.I.cfg.scanInterval());
                    ScanReceiver.schedule(act);
                    reload();
                    Ui.toast(act, "订阅已创建");
                }).show();
    }

    private EditText edit(String hint) {
        EditText e = new EditText(act);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(14);
        e.setTextColor(Ui.color(act, R.attr.msText));
        e.setHintTextColor(Ui.color(act, R.attr.msSub));
        e.setPadding(Ui.dp(act, 12), Ui.dp(act, 10), Ui.dp(act, 12), Ui.dp(act, 10));
        e.setBackground(Ui.round(act, R.attr.msInput, 10, 0));
        return e;
    }
}
