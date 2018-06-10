package com.abrenoch.hyperiongrabber.common;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.network.Hyperion;
import com.abrenoch.hyperiongrabber.common.network.HyperionThread;
import com.abrenoch.hyperiongrabber.common.util.Preferences;

import java.io.IOException;
import java.util.Objects;

public class HyperionScreenService extends Service {
    public static final String BROADCAST_ERROR = "SERVICE_ERROR";
    public static final String BROADCAST_TAG = "SERVICE_STATUS";
    public static final String BROADCAST_FILTER = "SERVICE_FILTER";
    private static final boolean DEBUG = true;
    private static final String TAG = "HyperionScreenService";

    private static final String BASE = "com.abrenoch.hyperiongrabber.service.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_EXIT = BASE + "ACTION_EXIT";
    public static final String GET_STATUS = BASE + "ACTION_STATUS";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_STAT_STOP_INTENT_ID = 2;
    private static final int NOTIFICATION_EXIT_INTENT_ID = 3;

    private boolean OGL_GRABBER = false;
    private boolean RECONNECT = false;
    private MediaProjectionManager mMediaProjectionManager;
    private HyperionThread mHyperionThread;
    private static MediaProjection _mediaProjection;
    private int mFrameRate;
    private HyperionScreenEncoder mHyperionEncoder;
    private HyperionScreenEncoderOGL mHyperionEncoderOGL;
    private NotificationManager mNotificationManager;
    private String mStartError = null;

    HyperionThreadBroadcaster mReceiver = new HyperionThreadBroadcaster() {
        @Override
        public void onConnected() {
            Log.d("DEBUG", "CONNECTED TO HYPERION INSTANCE");
        }

        @Override
        public void onConnectionError(int errorID, String error) {
            stopScreenRecord();
            mStartError = getResources().getString(R.string.error_server_unreachable);
            notifyActivity();
            Log.e("ERROR", "COULD NOT CONNECT TO HYPERION INSTANCE");
            if (error != null) Log.e("ERROR", error);
            if (RECONNECT) Log.e("DEBUG", "AUTOMATIC RECONNECT ENABLED. CONNECTING ...");
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
    private boolean prepared() {
        Preferences prefs = new Preferences(getBaseContext());
        String host = prefs.getString(R.string.pref_key_hyperion_host, null);
        String portString = prefs.getString(R.string.pref_key_hyperion_port, null);
        String priority = prefs.getString(R.string.pref_key_hyperion_priority, "50");
        String rate = prefs.getString(R.string.pref_key_hyperion_framerate, "30");
        OGL_GRABBER = prefs.getBoolean(R.string.pref_key_ogl_grabber, false);
        RECONNECT = prefs.getBoolean(R.string.pref_key_reconnect, false);
        String delay = prefs.getString(R.string.pref_key_reconnect_delay, "5");
        if (TextUtils.isEmpty(host) || Objects.equals(host, "0.0.0.0")) {
            mStartError = getResources().getString(R.string.error_empty_host);
            return false;
        }
        if (TextUtils.isEmpty(portString)) {
            mStartError = getResources().getString(R.string.error_empty_or_invalid_port);
            return false;
        }

        int portInt;
        try {
            portInt = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            mStartError = getResources().getString(R.string.error_empty_or_invalid_port);
            return false;
        }

        if (!assertHostReachable(host, portInt)){
            mStartError = getResources().getString(R.string.error_server_unreachable);
            return false;
        }

        mFrameRate = Integer.parseInt(rate);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mHyperionThread = new HyperionThread(mReceiver, host, portInt, Integer.parseInt(priority), RECONNECT, Integer.parseInt(delay));
        mHyperionThread.start();
        mStartError = null;
        return true;
    }

//    private void updateStatus() {
//        final Intent result = new Intent();
//        result.setAction(ACTION_QUERY_STATUS_RESULT);
//        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, false);
//        sendBroadcast(result);
//    }

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
                        tryStart(intent);
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
        if (isCapturing()) {
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
        if (isCapturing()) {
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
            if (OGL_GRABBER) {
                mHyperionEncoderOGL = new HyperionScreenEncoderOGL(mHyperionThread.getReceiver(),
                        projection, metrics.widthPixels, metrics.heightPixels,
                        density, mFrameRate);
                mHyperionEncoder = null;
            } else {
                mHyperionEncoder = new HyperionScreenEncoder(mHyperionThread.getReceiver(),
                        projection, metrics.widthPixels, metrics.heightPixels,
                        density, mFrameRate);
                mHyperionEncoderOGL = null;
            }
        }
    }

    private void stopScreenRecord() {
        if (DEBUG) Log.v(TAG, "stopScreenRecord");
        mNotificationManager.cancel(NOTIFICATION_ID);
        if (currentEncoder() != null) {
            if (DEBUG) Log.v(TAG, "stopScreenRecord:stopping encoder");
            currentEncoder().stopRecording();
        }
        releaseResource();
        if (mHyperionThread != null) {
            mHyperionThread.interrupt();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResource() {
        if (_mediaProjection != null) {
            _mediaProjection.stop();
            _mediaProjection = null;
        }
    }

    HyperionScreenEncoderBase currentEncoder() {
        if (mHyperionEncoder != null) {
            return mHyperionEncoder;
        } else if (mHyperionEncoderOGL != null) {
            return mHyperionEncoderOGL;
        }
        return null;
    }

    boolean isCapturing() {
        return currentEncoder() != null && currentEncoder().isCapturing();
    }

    private void notifyActivity() {
        Intent intent = new Intent(BROADCAST_FILTER);
        intent.putExtra(BROADCAST_TAG, isCapturing());
        intent.putExtra(BROADCAST_ERROR, mStartError);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void tryStart(Intent intent){
        new Thread(() -> { // need to be on a worker thread for network access
            boolean prepd = prepared();
            if (prepd) {
                new Handler(Looper.getMainLooper()).post(() -> { // need to be on Main thread for starting the recorder
                    startScreenRecord(intent);
                    notifyActivity();
                });
                startForeground(NOTIFICATION_ID, getNotification());
            } else {
                notifyActivity();
            }

        }).start();

    }

    private boolean assertHostReachable(String host, int port){
        try {
            Hyperion hyperion = new Hyperion(host, port);
            if (hyperion.isConnected()){
                hyperion.disconnect();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }


        public interface HyperionThreadBroadcaster {
//        void onResponse(String response);
        void onConnected();
        void onConnectionError(int errorHash, String errorString);
    }
}
