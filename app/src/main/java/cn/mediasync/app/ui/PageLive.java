package cn.mediasync.app.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.Ui;
import cn.mediasync.app.db.Db;

/** 直播监控与录制管理。 */
public class PageLive extends ListPage {

    public PageLive(MainActivity a) { super(a); }

    @Override protected String title() { return "直播监控与录制"; }
    @Override protected String desc() { return "B站 / 抖音 / 虎牙 / 斗鱼 / Twitch 等；实际录制由服务端执行"; }
    @Override protected String addLabel() { return "＋ 添加直播间"; }
    @Override protected String emptyText() { return "还没有直播间\n点击右上角添加直播间链接"; }
    @Override protected List<ContentValues> load() { return App.I.db.rooms(); }

    @Override protected String rowTitle(ContentValues r) { return Db.s(r, "title"); }
    @Override protected String rowSub(ContentValues r) {
        return Db.s(r, "platform") + "  ·  " + Db.s(r, "quality") + "  ·  " + Db.s(r, "url")
                + "\n状态 " + Db.s(r, "status") + "  ·  上次录制 " + Ui.time(Db.l(r, "last_record"));
    }
    @Override protected String rowTag(ContentValues r) {
        return Db.i(r, "enabled") == 1 ? "监控中" : "已暂停";
    }

    @Override protected void rowActions(LinearLayout box, ContentValues r) {
        final long id = id(r);
        final boolean on = Db.i(r, "enabled") == 1;
        addBtn(box, on ? "暂停监控" : "开启监控", v -> { App.I.db.roomToggle(id, !on); reload(); });
        addBtn(box, "完整版", v -> act.openWeb(""));
        addBtn(box, "删除", v -> Ui.confirm(act, "删除直播间", "确定删除「" + Db.s(r, "title") + "」？",
                () -> { App.I.db.delRoom(id); reload(); }));
    }

    @Override protected void onAdd() {
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(act, 18);
        box.setPadding(p, Ui.dp(act, 8), p, 0);
        final EditText name = edit("直播间名称");
        final EditText url = edit("直播间链接");
        final EditText quality = edit("画质（如 原画 / 1080P）");
        quality.setText("原画");
        box.addView(name);
        box.addView(url, gap());
        box.addView(quality, gap());

        new AlertDialog.Builder(act).setTitle("添加直播间").setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String u = Platforms.normalize(url.getText().toString());
                    if (u.isEmpty()) { Ui.toast(act, "请填写直播间链接"); return; }
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) n = Platforms.detect(u) + " 直播间";
                    String q = quality.getText().toString().trim();
                    App.I.db.addRoom(n, Platforms.detect(u), u, Platforms.fileName(u), q.isEmpty() ? "原画" : q);
                    reload();
                    Ui.toast(act, "直播间已添加，需连接服务后自动录制");
                }).show();
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 8);
        return lp;
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
