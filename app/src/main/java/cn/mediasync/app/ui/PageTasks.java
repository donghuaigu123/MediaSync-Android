package cn.mediasync.app.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.Ui;
import cn.mediasync.app.db.Db;

/** 下载任务队列：进度、暂停/继续/取消/重试、打开与删除。 */
public class PageTasks extends Page {
    private ListView list;
    private TextView summary;
    private final List<ContentValues> rows = new ArrayList<>();
    private BaseAdapter ad;

    public PageTasks(MainActivity a) { super(a, R.layout.page_tasks); }

    @Override
    protected void init() {
        list = $(R.id.taskList);
        summary = $(R.id.taskSummary);
        list.setEmptyView($(android.R.id.empty));

        ad = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int p) { return rows.get(p); }
            @Override public long getItemId(int p) { return Db.l(rows.get(p), "_id"); }
            @Override public View getView(int p, View cv, ViewGroup parent) {
                View v = cv;
                if (v == null) {
                    v = LayoutInflater.from(act).inflate(R.layout.item_task, null);
                    Ui.applyTheme(v);
                }
                ContentValues r = rows.get(p);
                final long id = Db.l(r, "_id");
                final String st = Db.s(r, "status");
                final String title = Db.s(r, "title");
                final String path = Db.s(r, "path");
                int pct = Db.i(r, "progress");
                long total = Db.l(r, "total");

                ((TextView) v.findViewById(R.id.itemTitle)).setText(title);
                String status = statusText(st);
                String err = Db.s(r, "err");
                ((TextView) v.findViewById(R.id.itemMeta)).setText(
                        Db.s(r, "platform") + "  ·  " + status
                                + (err.isEmpty() ? "" : "  ·  " + err)
                                + "\n" + Ui.size(total) + "  ·  " + pct + "%");

                ProgressBar pb = v.findViewById(R.id.itemProgress);
                pb.setProgress("done".equals(st) ? 100 : pct);
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(
                        Ui.color(act, R.attr.msAccent)));

                LinearLayout box = v.findViewById(R.id.itemActions);
                box.removeAllViews();
                if ("running".equals(st)) addBtn(box, "暂停", x -> { App.I.engine.pause(id); reload(); });
                else if ("paused".equals(st)) addBtn(box, "继续", x -> { App.I.engine.resume(id); reload(); });
                else if ("queued".equals(st)) addBtn(box, "取消", x -> { App.I.engine.cancel(id); reload(); });
                else if ("done".equals(st)) {
                    addBtn(box, "打开", x -> openFile(path));
                    addBtn(box, "分享", x -> shareFile(path));
                } else {
                    addBtn(box, "重试", x -> { App.I.engine.retry(id); reload(); });
                }
                addBtn(box, "删除", x -> Ui.confirm(act, "删除任务", "删除「" + title + "」？已下载文件不会被删除。",
                        () -> { App.I.engine.cancel(id); App.I.db.delTask(id); reload(); }));
                return v;
            }
        };
        list.setAdapter(ad);

        $(R.id.btnClearDone).setOnClickListener(v -> Ui.confirm(act, "清除已完成", "从队列中移除所有已完成任务？",
                () -> { App.I.db.clearDone(); reload(); }));
    }

    private void addBtn(LinearLayout box, String label, View.OnClickListener l) {
        TextView b = Ui.action(act, label, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(act, 8);
        box.addView(b, lp);
    }

    @Override
    public void onShow() { reload(); }

    private void reload() {
        rows.clear();
        rows.addAll(App.I.db.tasks());
        summary.setText("下载任务  ·  " + rows.size() + " 个");
        if (ad != null) ad.notifyDataSetChanged();
    }

    private static String statusText(String s) {
        switch (s) {
            case "queued": return "排队中";
            case "running": return "下载中";
            case "paused": return "已暂停";
            case "done": return "已完成";
            case "error": return "失败";
            case "canceled": return "已取消";
            default: return s;
        }
    }

    private void openFile(String path) {
        if (path.isEmpty()) { Ui.toast(act, "文件路径为空"); return; }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(new File(path)), Platforms.mimeOf(path));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(i);
        } catch (Exception e) { Ui.toast(act, "没有可打开该文件的应用"); }
    }

    private void shareFile(String path) {
        if (path.isEmpty()) return;
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(Platforms.mimeOf(path));
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(Intent.createChooser(i, "分享文件"));
        } catch (Exception e) { Ui.toast(act, "分享失败"); }
    }
}
