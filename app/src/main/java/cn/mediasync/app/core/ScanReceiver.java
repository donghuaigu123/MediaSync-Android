package cn.mediasync.app.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import org.json.JSONObject;

import java.util.List;

import cn.mediasync.app.db.Db;

/** 定时订阅扫描：连接服务端时触发增量扫描，否则仅更新本地扫描记录。 */
public class ScanReceiver extends BroadcastReceiver {
    public static final String ACTION = "cn.mediasync.app.SCAN";
    private static final int REQ = 771;

    public static void schedule(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(c, ScanReceiver.class).setAction(ACTION);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(c, REQ, i, flags);
            long interval = Math.max(15, new Settings(c).scanInterval()) * 60_000L;
            am.cancel(pi);
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + interval, interval, pi);
        } catch (Throwable ignored) { }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;
        final Context c = context.getApplicationContext();
        new Thread(() -> {
            Db db = new Db(c);
            Settings cfg = new Settings(c);
            List<ContentValues> subs = db.subs();
            int enabled = 0;
            for (ContentValues v : subs) if (Db.i(v, "enabled") == 1) enabled++;
            if (enabled == 0) return;

            int found = 0;
            String server = cfg.server();
            if (!server.isEmpty()) {
                try {
                    String body = Analyzer.httpGet(
                            server + "/api/subscriptions/scan?sync=1", 30000);
                    JSONObject o = new JSONObject(body);
                    if (o.has("data") && o.get("data") instanceof JSONObject) o = o.getJSONObject("data");
                    found = o.optInt("new_count", o.optInt("new", o.optInt("added", 0)));
                } catch (Exception ignored) { }
            }
            for (ContentValues v : subs) {
                if (Db.i(v, "enabled") == 1) db.subTouch(Db.l(v, "_id"), found);
            }
            if (cfg.notifyEnabled()) {
                Notify.event(c, "订阅扫描完成",
                        "已检查 " + enabled + " 个订阅" + (found > 0 ? "，新增 " + found + " 条内容" : "，暂无更新"));
            }
        }, "mediasync-scan").start();
    }
}
