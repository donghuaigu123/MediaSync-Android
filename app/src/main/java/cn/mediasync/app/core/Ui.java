package cn.mediasync.app.core;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cn.mediasync.app.R;

/** 界面通用工具：主题色、圆角背景、控件构建、格式化。 */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static int color(Context c, int attr) {
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(attr, tv, true);
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }
        if (tv.resourceId != 0) return c.getResources().getColor(tv.resourceId, c.getTheme());
        return 0xFF888888;
    }

    public static GradientDrawable round(Context c, int fillAttr, float radiusDp, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color(c, fillAttr));
        g.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), color(c, R.attr.msLine));
        return g;
    }

    public static GradientDrawable card(Context c) {
        return round(c, R.attr.msCard, 14, 1);
    }

    public static GradientDrawable brand(Context c) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF7054EC, 0xFF4B32C3});
        g.setCornerRadius(dp(c, 10));
        return g;
    }

    /** 按 android:tag 为整个视图树套用当前主题的背景。 */
    public static void applyTheme(View v) {
        if (v == null) return;
        Object t = v.getTag();
        if (t != null) {
            Context c = v.getContext();
            String tag = t.toString();
            if ("card".equals(tag)) v.setBackground(card(c));
            else if ("input".equals(tag)) v.setBackground(round(c, R.attr.msInput, 10, 0));
            else if ("soft".equals(tag)) v.setBackground(round(c, R.attr.msInput, 10, 0));
            else if ("badge".equals(tag)) v.setBackground(round(c, R.attr.msInput, 999, 0));
            else if ("brand".equals(tag)) v.setBackground(brand(c));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyTheme(g.getChildAt(i));
        }
    }

    public static TextView action(Context c, String label, View.OnClickListener l) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(11);
        t.setTextColor(color(c, R.attr.msAccent));
        t.setPadding(dp(c, 11), dp(c, 6), dp(c, 11), dp(c, 6));
        t.setBackground(round(c, R.attr.msInput, 8, 0));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    public static TextView text(Context c, String s, float sizeSp, int colorAttr, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color(c, colorAttr));
        t.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        return t;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static void toast(Context c, String s) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(c.getApplicationContext(), s, Toast.LENGTH_SHORT).show());
    }

    public static void confirm(Context c, String title, String msg, Runnable ok) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(msg)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (DialogInterface d, int w) -> ok.run()).show();
    }

    public static void alert(Context c, String title, String msg) {
        new AlertDialog.Builder(c).setTitle(title).setMessage(msg)
                .setPositiveButton("知道了", null).show();
    }

    /** 单行输入的对话框。 */
    public static void prompt(Context c, String title, String hint, String preset, OnText cb) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 18);
        box.setPadding(pad, dp(c, 8), pad, 0);
        final android.widget.EditText et = new android.widget.EditText(c);
        et.setHint(hint);
        et.setText(preset == null ? "" : preset);
        et.setSingleLine(true);
        et.setTextColor(color(c, R.attr.msText));
        et.setHintTextColor(color(c, R.attr.msSub));
        et.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        et.setBackground(round(c, R.attr.msInput, 10, 0));
        box.addView(et);
        new AlertDialog.Builder(c).setTitle(title).setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (DialogInterface d, int w) -> cb.onText(et.getText().toString().trim()))
                .show();
    }

    public interface OnText { void onText(String s); }

    public static String size(long b) {
        if (b <= 0) return "0 B";
        String[] u = {"B", "KB", "MB", "GB", "TB"};
        double v = b;
        int i = 0;
        while (v >= 1024 && i < u.length - 1) { v /= 1024; i++; }
        return String.format(Locale.CHINA, i == 0 ? "%.0f %s" : "%.1f %s", v, u[i]);
    }

    public static String time(long ms) {
        if (ms <= 0) return "尚未";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(ms));
    }

    public static String safeName(String s) {
        if (s == null || s.trim().isEmpty()) return "media_" + System.currentTimeMillis();
        String n = s.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        return n.length() > 80 ? n.substring(0, 80) : n;
    }
}
