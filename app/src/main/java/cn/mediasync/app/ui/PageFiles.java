package cn.mediasync.app.ui;

import android.content.ContentValues;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.mediasync.app.App;
import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Ui;
import cn.mediasync.app.db.Db;

/** 文件浏览：查看、打开、播放、分享与删除已保存文件。 */
public class PageFiles extends ListPage {

    public PageFiles(MainActivity a) { super(a); }

    @Override protected String title() { return "文件浏览"; }
    @Override protected String desc() { return App.I.downloadDir().getAbsolutePath(); }
    @Override protected String addLabel() { return "刷新"; }
    @Override protected String emptyText() { return "目录还是空的\n下载完成后文件会保存在这里"; }

    @Override protected List<ContentValues> load() {
        List<ContentValues> out = new ArrayList<>();
        File dir = App.I.downloadDir();
        File[] fs = dir.listFiles();
        if (fs != null) {
            Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (File f : fs) {
                if (!f.isFile()) continue;
                ContentValues v = new ContentValues();
                v.put("name", f.getName());
                v.put("path", f.getAbsolutePath());
                v.put("size", f.length());
                v.put("time", f.lastModified());
                out.add(v);
            }
        }
        return out;
    }

    @Override protected String rowTitle(ContentValues r) { return Db.s(r, "name"); }
    @Override protected String rowSub(ContentValues r) {
        return Ui.size(Db.l(r, "size")) + "  ·  " + Ui.time(Db.l(r, "time"));
    }
    @Override protected String rowTag(ContentValues r) { return kind(Db.s(r, "name")); }

    private String kind(String n) {
        String x = n.toLowerCase();
        if (x.endsWith(".mp4") || x.endsWith(".webm") || x.endsWith(".mkv") || x.endsWith(".flv")) return "视频";
        if (x.endsWith(".mp3") || x.endsWith(".m4a") || x.endsWith(".flac")) return "音频";
        if (x.endsWith(".jpg") || x.endsWith(".png") || x.endsWith(".gif") || x.endsWith(".webp")) return "图片";
        if (x.endsWith(".srt") || x.endsWith(".ass") || x.endsWith(".vtt")) return "字幕";
        if (x.endsWith(".json")) return "备份";
        return "文件";
    }

    @Override protected void rowActions(LinearLayout box, ContentValues r) {
        final String path = Db.s(r, "path");
        final String name = Db.s(r, "name");
        addBtn(box, "打开", v -> openFile(path));
        addBtn(box, "分享", v -> shareFile(path));
        addBtn(box, "删除", v -> Ui.confirm(act, "删除文件", "确定删除「" + name + "」？该操作不可恢复。",
                () -> {
                    try { new File(path).delete(); } catch (Exception ignored) { }
                    reload();
                }));
    }

    @Override protected void onAdd() {
        ((android.widget.TextView) $(R.id.pageDesc)).setText(App.I.downloadDir().getAbsolutePath());
        reload();
        Ui.toast(act, "已刷新文件列表");
    }
}
