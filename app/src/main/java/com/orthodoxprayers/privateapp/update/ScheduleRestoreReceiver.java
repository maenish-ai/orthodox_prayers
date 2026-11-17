package com.orthodoxprayers.privateapp.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.orthodoxprayers.privateapp.OrthodoxPrayersApp;

/** Restores the network-free 00:03 Amman local refresh after system changes. */
public final class ScheduleRestoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) return;
        Context applicationContext = context.getApplicationContext();
        if (!(applicationContext instanceof OrthodoxPrayersApp)) return;
        OrthodoxPrayersApp app = (OrthodoxPrayersApp) applicationContext;
        app.updateCoordinator().scheduleDailyRefresh();
        app.updateCoordinator().scheduleChurchDirectoryWeeklySync();
        app.appUpdateManager().schedulePeriodicChecks();
    }
}
