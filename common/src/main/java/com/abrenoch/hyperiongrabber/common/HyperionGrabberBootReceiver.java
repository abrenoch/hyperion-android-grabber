package com.abrenoch.hyperiongrabber.common;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

public class HyperionGrabberBootReceiver extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

//            if (preferences.getBoolean("start_on_boot", false) && hasScreenshotPermission(context)) {
//                MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager)
//                        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//
//                Intent i = new Intent(context, HyperionScreenService.class);
//                i.setAction(HyperionScreenService.ACTION_START);
//                i.putExtra(HyperionScreenService.EXTRA_RESULT_CODE, 1);
//
//                assert mMediaProjectionManager != null;
//                i.putExtras((Intent) mMediaProjectionManager.createScreenCaptureIntent().clone());
////                i.putExtras((Intent) screenshotPermission.clone());
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    context.startForegroundService(i);
//                } else {
//                    context.startService(i);
//                }
//            }
        }
    }

    public static boolean hasScreenshotPermission(Context context) {
        return (context.checkCallingOrSelfPermission(Manifest.permission.READ_FRAME_BUFFER) ==
                PackageManager.PERMISSION_GRANTED);
    }
}
