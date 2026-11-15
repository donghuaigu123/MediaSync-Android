package cn.mediasync.app;

import android.app.Application;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;

import cn.mediasync.app.core.DownloadEngine;
import cn.mediasync.app.core.Notify;
import cn.mediasync.app.core.ScanReceiver;
import cn.mediasync.app.core.Settings;
import cn.mediasync.app.db.Db;

public class App extends Application {
    public static App I;

    public Db db;
    public Settings cfg;
    public DownloadEngine engine;

    @Override
    public void onCreate() {
        super.onCreate();
        I = this;
        // 允许通过 file:// Intent 打开/分享应用私有目录里的文件（无 AndroidX FileProvider）
        try { StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build()); } catch (Throwable ignored) { }

        db = new Db(this);
        cfg = new Settings(this);
        engine = new DownloadEngine(this, db, cfg);
        Notify.init(this);
        ScanReceiver.schedule(this);
        engine.resumePending();
    }

    /** 下载保存目录，优先使用设置中的自定义路径，不可写时回落到应用私有目录。 */
    public File downloadDir() {
        File fallback = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "媒体同步");
        String custom = cfg.downloadDir();
        if (custom != null && !custom.trim().isEmpty()) {
            File d = new File(custom.trim());
            if ((d.exists() || d.mkdirs()) && d.canWrite()) return d;
        }
        if (!fallback.exists()) fallback.mkdirs();
        return fallback;
    }
}
