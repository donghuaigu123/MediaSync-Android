package cn.mediasync.app.core;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.mediasync.app.App;
import cn.mediasync.app.db.Db;

/** 下载引擎：并发队列、断点续传、暂停/继续/取消/重试、进度与通知。 */
public class DownloadEngine {
    private final Context c;
    private final Db db;
    private final Settings cfg;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<Long, Worker> active = new ConcurrentHashMap<>();
    private ExecutorService pool;

    public DownloadEngine(Context c, Db db, Settings cfg) {
        this.c = c.getApplicationContext();
        this.db = db;
        this.cfg = cfg;
        this.pool = Executors.newFixedThreadPool(cfg.concurrency());
    }

    public long enqueue(String title, String url, String platform, String quality) {
        long id = db.addTask(title, url, platform, quality);
        startService();
        submit(id);
        return id;
    }

    private void startService() {
        try {
            Intent i = new Intent(c, DownloadService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable ignored) { }
    }

    private void submit(final long id) {
        if (active.containsKey(id)) return;
        if (pool == null || pool.isShutdown()) pool = Executors.newFixedThreadPool(cfg.concurrency());
        Worker w = new Worker(id);
        active.put(id, w);
        try { pool.execute(w); } catch (Exception e) { active.remove(id); }
    }

    /** 应用启动时恢复未完成的任务（进程被杀后 status=running 的任务重置为 queued）。 */
    public void resumePending() {
        List<ContentValues> rows = db.queued();
        if (rows.isEmpty()) return;
        startService();
        for (ContentValues v : rows) {
            long id = Db.l(v, "_id");
            if ("running".equals(Db.s(v, "status"))) db.taskStatus(id, "queued");
            submit(id);
        }
    }

    public void pause(long id) {
        Worker w = active.get(id);
        if (w != null) w.paused = true;
        db.taskStatus(id, "paused");
    }

    public void resume(long id) {
        Worker w = active.get(id);
        if (w != null && !w.cancelled) {
            w.paused = false;
            db.taskStatus(id, "running");
        } else {
            db.taskStatus(id, "queued");
            scheduleSubmit(id, 0);
        }
    }

    private void scheduleSubmit(final long id, final int attempt) {
        main.postDelayed(() -> {
            if (active.containsKey(id)) {
                if (attempt < 6) scheduleSubmit(id, attempt + 1);
                return;
            }
            submit(id);
        }, 300L);
    }

    public void cancel(long id) {
        Worker w = active.get(id);
        if (w != null) w.cancelled = true;
        db.taskStatus(id, "canceled");
    }

    public void retry(long id) {
        ContentValues cv = new ContentValues();
        cv.put("err", "");
        db.taskUpdate(id, cv);
        db.taskStatus(id, "queued");
        startService();
        scheduleSubmit(id, 0);
    }

    public void setConcurrency(int n) {
        ExecutorService old = pool;
        pool = Executors.newFixedThreadPool(n);
        if (old != null) old.shutdown();
    }

    public int activeCount() { return active.size(); }

    private boolean isWifi() {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI;
        } catch (Exception e) { return true; }
    }

    private Proxy buildProxy() {
        String p = cfg.proxy();
        if (p == null || p.trim().isEmpty()) return null;
        try {
            String s = p.trim();
            Proxy.Type t = s.toLowerCase().startsWith("socks") ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            s = s.replaceFirst("^[a-zA-Z]+://", "");
            int colon = s.lastIndexOf(':');
            String host = s.substring(0, colon);
            int port = Integer.parseInt(s.substring(colon + 1));
            return new Proxy(t, new InetSocketAddress(host, port));
        } catch (Exception e) { return null; }
    }

    private class Worker implements Runnable {
        final long id;
        volatile boolean paused = false;
        volatile boolean cancelled = false;

        Worker(long id) { this.id = id; }

        @Override
        public void run() {
            ContentValues t = db.task(id);
            if (t == null) { active.remove(id); return; }

            final String url = Db.s(t, "url");
            final String title = Db.s(t, "title");
            final String ext = Platforms.ext(url, null);
            File dir = App.I.downloadDir();
            File file = new File(dir, Ui.safeName(title) + ext);
            int n = 1;
            while (file.exists() && file.length() == 0 && n < 2) {
                file = new File(dir, Ui.safeName(title) + "_" + (n++) + ext);
            }

            HttpURLConnection conn = null;
            InputStream in = null;
            RandomAccessFile raf = null;
            db.taskStatus(id, "running");
            Notify.serviceUpdate(c, "正在下载：" + title);

            try {
                if (cfg.wifiOnly() && !isWifi()) {
                    db.taskStatus(id, "paused");
                    Notify.event(c, "已暂停", "当前非 WiFi 网络：" + title);
                    return;
                }

                long exist = file.exists() ? file.length() : 0;
                URL u = new URL(url);
                Proxy proxy = buildProxy();
                conn = proxy == null
                        ? (HttpURLConnection) u.openConnection()
                        : (HttpURLConnection) u.openConnection(proxy);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 MediaSync-Android/1.0");
                conn.setRequestProperty("Accept", "*/*");
                if (exist > 0) conn.setRequestProperty("Range", "bytes=" + exist + "-");

                int code = conn.getResponseCode();
                if (code >= 400) throw new Exception("HTTP " + code);
                boolean append = code == 206 && exist > 0;
                long len = conn.getHeaderFieldLong("Content-Length", 0);
                long total = append ? exist + len : len;

                raf = new RandomAccessFile(file, "rwd");
                if (append) raf.seek(exist); else raf.setLength(0);
                in = new BufferedInputStream(conn.getInputStream());

                byte[] buf = new byte[16384];
                long done = append ? exist : 0;
                long lastUp = 0;
                int read;
                while ((read = in.read(buf)) != -1) {
                    if (cancelled || paused || Thread.currentThread().isInterrupted()) break;
                    raf.write(buf, 0, read);
                    done += read;
                    long now = System.currentTimeMillis();
                    if (now - lastUp > 400) {
                        int pct = total > 0 ? (int) (done * 100 / total) : 0;
                        db.taskProgress(id, Math.min(99, Math.max(0, pct)), total);
                        Notify.progress(c, title, Math.min(99, Math.max(0, pct)));
                        lastUp = now;
                    }
                }

                if (cancelled) {
                    db.taskStatus(id, "canceled");
                    Notify.clearProgress(c);
                } else if (paused) {
                    ContentValues cur = db.task(id);
                    // 若用户已点「继续」把状态改回 queued，则不要覆盖
                    if (cur != null && "running".equals(Db.s(cur, "status"))) db.taskStatus(id, "paused");
                } else {
                    db.taskDone(id, file.getAbsolutePath());
                    if (cfg.notifyEnabled()) Notify.complete(c, title, file.getAbsolutePath());
                    else Notify.clearProgress(c);
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                db.taskError(id, msg);
                if (cfg.notifyEnabled()) Notify.event(c, "下载失败", title + " · " + msg);
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) { }
                try { if (raf != null) raf.close(); } catch (Exception ignored) { }
                if (conn != null) conn.disconnect();
                active.remove(id);
                Notify.serviceUpdate(c, active.isEmpty() ? "下载服务已就绪" : "剩余 " + active.size() + " 个任务");
            }
        }
    }
}
