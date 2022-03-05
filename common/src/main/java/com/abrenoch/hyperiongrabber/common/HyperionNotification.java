package com.abrenoch.hyperiongrabber.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class HyperionNotification {
    private final String NOTIFICATION_CHANNEL_ID = "com.abrenoch.hyperiongrabber.notification";
    private final String NOTIFICATION_CHANNEL_LABEL;
    private final String NOTIFICATION_TITLE;
    private final String NOTIFICATION_DESCRIPTION;
    private final int PENDING_INTENT_REQUEST_CODE = 0;
    private final NotificationManager mNotificationManager;
    private final Context mContext;
    private Notification.Action mAction = null;

    HyperionNotification (Context ctx, NotificationManager manager) {
        mNotificationManager = manager;
        mContext = ctx;
        NOTIFICATION_TITLE = mContext.getString(R.string.app_name);
        NOTIFICATION_DESCRIPTION = mContext.getString(R.string.notification_description);
        NOTIFICATION_CHANNEL_LABEL = mContext.getString(R.string.notification_channel_label);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(makeChannel());
        }
    }

    public void setAction(int code, String label, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getService(mContext, code,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAction = new Notification.Action.Builder(
                    Icon.createWithResource(mContext, R.drawable.ic_notification_icon),
                    label,
                    pendingIntent
            ).build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            mAction = new Notification.Action.Builder(
                    R.drawable.ic_notification_icon,
                    label,
                    pendingIntent
            ).build();
        }
    }

    public Notification buildNotification() {
        PendingIntent pIntent = null;
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(mContext.getPackageName());
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pIntent = PendingIntent.getActivity(mContext, PENDING_INTENT_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentTitle(NOTIFICATION_TITLE)
                    .setContentText(NOTIFICATION_DESCRIPTION);
            if (mAction != null) {
                builder.addAction(mAction);
            }
            if (pIntent != null) {
                builder.setContentIntent(pIntent);
            }
            return builder.build();
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                    .setVibrate(null)
                    .setSound(null)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentTitle(NOTIFICATION_TITLE)
                    .setContentText(NOTIFICATION_DESCRIPTION);
            if (pIntent != null) {
                builder.setContentIntent(pIntent);
            }
            return builder.build();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel makeChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_LABEL, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(NOTIFICATION_CHANNEL_LABEL);
        notificationChannel.enableVibration(false);
        notificationChannel.setSound(null,null);
        return notificationChannel;
    }
}
