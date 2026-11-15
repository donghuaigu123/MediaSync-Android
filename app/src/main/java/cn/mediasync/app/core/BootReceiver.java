package cn.mediasync.app.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.mediasync.app.App;

/** 开机后恢复定时扫描与未完成的下载队列。 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        ScanReceiver.schedule(context);
        try { App.I.engine.resumePending(); } catch (Exception ignored) { }
    }
}
