package com.abrenoch.hyperiongrabber.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

public class HyperionGrabberBootReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean("start_on_boot", false)) {
                final Intent i = new Intent(context, BootActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION
                        |Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        |Intent.FLAG_ACTIVITY_NO_HISTORY);

                context.startActivity(i);
            }
        }
    }
}
