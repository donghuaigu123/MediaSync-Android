package cn.mediasync.app.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import cn.mediasync.app.MainActivity;
import cn.mediasync.app.R;

/** 通知渠道与通知构建。 */
public final class Notify {
    public static final String CH_DOWN = "mediasync_download";
    public static final String CH_EVENT = "mediasync_event";
    private static final int ID_SERVICE = 1;
    private static final int ID_PROGRESS = 2;
    private static int eventId = 100;

    private Notify() { }

    public static void init(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel a = new NotificationChannel(CH_DOWN, "媒体同步 · 下载", NotificationManager.IMPORTANCE_LOW);
            a.setDescription("下载进度与前台服务");
            NotificationChannel b = new NotificationChannel(CH_EVENT, "媒体同步 · 事件", NotificationManager.IMPORTANCE_DEFAULT);
            b.setDescription("下载完成、订阅扫描与直播录制提醒");
            nm.createNotificationChannel(a);
            nm.createNotificationChannel(b);
        }
    }

    private static Notification.Builder builder(Context c, String ch) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return new Notification.Builder(c, ch);
        return new Notification.Builder(c);
    }

    private static PendingIntent openApp(Context c) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), flags);
    }

    public static Notification service(Context c, String text) {
        return builder(c, CH_DOWN)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("媒体同步")
                .setContentText(text)
                .setContentIntent(openApp(c))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    public static void serviceUpdate(Context c, String text) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_SERVICE, service(c, text));
    }

    public static void progress(Context c, String title, int pct) {
        Notification n = builder(c, CH_DOWN)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("正在下载")
                .setContentText(title)
                .setProgress(100, pct, false)
                .setContentIntent(openApp(c))
                .setOnlyAlertOnce(true)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(ID_PROGRESS, n);
    }

    public static void clearProgress(Context c) {
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ID_PROGRESS);
    }

    public static void complete(Context c, String title, String path) {
        clearProgress(c);
        event(c, "下载完成", title + "  ·  " + new java.io.File(path).getName());
    }

    public static void event(Context c, String title, String msg) {
        Notification n = builder(c, CH_EVENT)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentIntent(openApp(c))
                .setAutoCancel(true)
                .build();
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(eventId++, n);
    }
}
