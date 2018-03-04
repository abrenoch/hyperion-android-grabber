package com.abrenoch.hyperiongrabber;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;

public class HyperionScreenService extends Service {
    private static final boolean DEBUG = false;
    private static final String TAG = "HyperionScreenService";

    private static final String BASE = "com.abrenoch.hyperiongrabber.service.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_EXIT = BASE + "ACTION_EXIT";
    public static final String GET_STATUS = BASE + "ACTION_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_STAT_STOP_INTENT_ID = 2;
    private static final int NOTIFICATION_EXIT_INTENT_ID = 3;

    private MediaProjectionManager mMediaProjectionManager;
    private HyperionThread mHyperionThread;
    private static MediaProjection _mediaProjection;
    private int mFrameRate;
    private HyperionScreenEncoder mHyperionEncoder;
    private NotificationManager mNotificationManager;

    HyperionThreadBroadcaster mReceiver = new HyperionThreadBroadcaster() {
        @Override
        public void onConnected() {
            Log.d("DEBUG", "CONNECTED TO HYPERION INSTANCE");
        }

        @Override
        public void onConnectionError(int errorID, String error) {
            Log.e("ERROR", "COULD NOT CONNECT TO HYPERION INSTANCE");
            if (error != null) Log.e("ERROR", error);
        }

//        @Override
//        public void onResponse(String response) {
//
//        }
    };

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() {
        if (DEBUG) Log.v(TAG, "prepare::");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String host = preferences.getString("hyperion_host", null);
        String port = preferences.getString("hyperion_port", null);
        String priority = preferences.getString("hyperion_priority", null);
        String rate = preferences.getString("hyperion_framerate", null);
        if (host == null || port == null) {
            Log.e(TAG, "HOST AND PORT SHOULD NOT BE EMPTY");
            return;
        }
        if (rate == null) rate = "30";
        if (priority == null) priority = "50";
        mFrameRate = Integer.parseInt(rate);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mHyperionThread = new HyperionThread(mReceiver, host, Integer.parseInt(port), Integer.parseInt(priority));
        mHyperionThread.start();
    }

    private void updateStatus() {
        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, false);
        sendBroadcast(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.v(TAG, "onStartCommand::");
        super.onStartCommand(intent, flags, startId);
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_START:
                    if (mHyperionThread == null) {
                        prepare();
                        startScreenRecord(intent);
                        notifyActivity();
                        startForeground(NOTIFICATION_ID, getNotification());
                    }
                    break;
                case ACTION_STOP:
                    stopScreenRecord();
                    break;
                case GET_STATUS:
                    notifyActivity();
                    break;
                case ACTION_EXIT:
                    stopScreenRecord();
                    stopForeground(true);
                    notifyActivity();
                    stopSelf();
                    break;
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Intent buildStopStartButtons() {
        Intent notificationIntent = new Intent(this, this.getClass());
        notificationIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        if (mHyperionEncoder != null && mHyperionEncoder.isCapturing()) {
            notificationIntent.setAction(ACTION_EXIT);
        } else {
            notificationIntent.setAction(ACTION_START);
        }
        return notificationIntent;
    }

    private Intent buildExitButton() {
        Intent notificationIntent = new Intent(this, this.getClass());
        notificationIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        notificationIntent.setAction(ACTION_EXIT);
        return notificationIntent;
    }

    public Notification getNotification() {
        HyperionNotification notification = new HyperionNotification(this, mNotificationManager);
        String label = "START GRABBER";
        String label2 = "EXIT";
        if (mHyperionEncoder != null && mHyperionEncoder.isCapturing()) {
            label = "STOP GRABBER";
        }
        notification.setAction(NOTIFICATION_STAT_STOP_INTENT_ID, label, buildStopStartButtons());
        notification.setAction(NOTIFICATION_EXIT_INTENT_ID, label2, buildExitButton());
        return notification.buildNotification();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {
        final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        // get MediaProjection
        final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
        if (projection != null) {
            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            final int density = metrics.densityDpi;
            _mediaProjection = projection;
            if (DEBUG) Log.v(TAG, "startRecording:");
            mHyperionEncoder = new HyperionScreenEncoder(mHyperionThread.getReceiver(),
                    projection, metrics.widthPixels, metrics.heightPixels,
                    density, mFrameRate);
        }
    }

    private void stopScreenRecord() {
        if (DEBUG) Log.v(TAG, "stopScreenRecord");
        mNotificationManager.cancel(NOTIFICATION_ID);
        if (mHyperionEncoder != null) {
            if (DEBUG) Log.v(TAG, "stopScreenRecord:stopping encoder");
            mHyperionEncoder.stopRecording();
        }
        releaseResource();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResource() {
        if (_mediaProjection != null) {
            _mediaProjection.stop();
            _mediaProjection = null;
        }
    }

    private void notifyActivity() {
        Intent intent = new Intent(MainActivity.BROADCAST_FILTER);
        intent.putExtra(MainActivity.BROADCAST_TAG,
                mHyperionEncoder != null && mHyperionEncoder.isCapturing());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public interface HyperionThreadBroadcaster {
//        void onResponse(String response);
        void onConnected();
        void onConnectionError(int errorHash, String errorString);
    }
}