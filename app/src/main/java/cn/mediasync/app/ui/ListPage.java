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
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;
import cn.mediasync.app.core.Platforms;
import cn.mediasync.app.core.Ui;
import cn.mediasync.app.db.Db;

/** 通用列表页（订阅 / 直播 / 文件）。 */
public abstract class ListPage extends Page {
    protected ListView list;
    protected final List<ContentValues> rows = new ArrayList<>();
    private BaseAdapter ad;

    public ListPage(MainActivity a) { super(a, R.layout.page_list); }

    protected abstract String title();
    protected abstract String desc();
    protected abstract String addLabel();
    protected abstract String emptyText();
    protected abstract List<ContentValues> load();
    protected abstract String rowTitle(ContentValues r);
    protected abstract String rowSub(ContentValues r);
    protected abstract String rowTag(ContentValues r);
    protected abstract void rowActions(LinearLayout box, ContentValues r);
    protected abstract void onAdd();

    @Override
    protected void init() {
        list = $(R.id.pageList);
        ((TextView) $(R.id.pageTitle)).setText(title());
        ((TextView) $(R.id.pageDesc)).setText(desc());
        ((TextView) $(R.id.btnAdd)).setText(addLabel());
        TextView empty = $(android.R.id.empty);
        empty.setText(emptyText());
        list.setEmptyView(empty);
        $(R.id.btnAdd).setOnClickListener(v -> onAdd());

        ad = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int p) { return rows.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View cv, ViewGroup parent) {
                View v = cv;
                if (v == null) {
                    v = LayoutInflater.from(act).inflate(R.layout.item_generic, null);
                    Ui.applyTheme(v);
                }
                ContentValues r = rows.get(p);
                ((TextView) v.findViewById(R.id.gTitle)).setText(rowTitle(r));
                ((TextView) v.findViewById(R.id.gSubtitle)).setText(rowSub(r));
                ((TextView) v.findViewById(R.id.gTag)).setText(rowTag(r));
                LinearLayout box = v.findViewById(R.id.gActions);
                box.removeAllViews();
                rowActions(box, r);
                return v;
            }
        };
        list.setAdapter(ad);
    }

    @Override
    public void onShow() { reload(); }

    protected void reload() {
        rows.clear();
        rows.addAll(load());
        if (ad != null) ad.notifyDataSetChanged();
    }

    protected void addBtn(LinearLayout box, String label, View.OnClickListener l) {
        TextView b = Ui.action(act, label, l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(act, 6);
        box.addView(b, lp);
    }

    protected void openFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(new File(path)), Platforms.mimeOf(path));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(i);
        } catch (Exception e) { Ui.toast(act, "没有可打开该文件的应用"); }
    }

    protected void shareFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(Platforms.mimeOf(path));
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(Intent.createChooser(i, "分享文件"));
        } catch (Exception e) { Ui.toast(act, "分享失败"); }
    }

    protected long id(ContentValues r) { return Db.l(r, "_id"); }
}
