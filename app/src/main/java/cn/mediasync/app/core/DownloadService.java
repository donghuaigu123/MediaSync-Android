package cn.mediasync.app.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** 前台服务：保证后台下载与通知持续可用。 */
public class DownloadService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, Notify.service(this, "下载服务已就绪"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, Notify.service(this, "下载队列运行中"));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
