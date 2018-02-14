package com.abrenoch.hyperiongrabber;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;

public class HyperionScreenService extends Service {
    private static final boolean DEBUG = true;
    private static final String TAG = "HyperionScreenService";

    private static final String BASE = "com.abrenoch.hyperiongrabber.service.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_PAUSE = BASE + "ACTION_PAUSE";
    public static final String ACTION_RESUME = BASE + "ACTION_RESUME";
    public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";

    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_RELEASE_RESOURCE = BASE + "ACTION_RELEASE_RESOURCE";

    private static Object sSync = new Object();

    private MediaProjectionManager mMediaProjectionManager;

    private HyperionThread mHyperionThread;

    private static MediaProjection _mediaProjection;

    private int mFrameRate;



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

        @Override
        public void onResponse(String response) {

        }
    };
    private HyperionScreenEncoder mHyperionEncoder;
    private NotificationManager mNotificationManager;


    public HyperionScreenService() {
//        super(TAG);
    }

    @Override
    public void onCreate() {

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        super.onCreate();


    }


    private void prepare() {
        if (DEBUG)
            Log.v(TAG, "prepare::");

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
//
//    @Override
//    protected void onHandleIntent(final Intent intent) {
//        if (DEBUG) Log.v(TAG, "onHandleIntent:intent=" + intent);
//        final String action = intent.getAction();
//
//
//        if (ACTION_START.equals(action)) {
//
//            prepare();
//
//
//            startForeground(NOTIFICATION_ID, getNotification());
//
//            startScreenRecord(intent);
//            updateStatus();
//        } else if (ACTION_STOP.equals(action)) {
//            stopScreenRecord();
//            updateStatus();
//        } else if (ACTION_QUERY_STATUS.equals(action)) {
//            updateStatus();
//        } else if (ACTION_PAUSE.equals(action)) {
//            pauseScreenRecord();
//            updateStatus();
//        } else if (ACTION_RESUME.equals(action)) {
//            resumeScreenRecord();
//            updateStatus();
//        } else if (ACTION_RELEASE_RESOURCE.equals(action)) {
//            releaseResource();
//        }
//    }

    private void updateStatus() {
        final boolean isRecording, isPausing;
//        synchronized (sSync) {
            isPausing = false;
//        }
        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
//        result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, isPausing);
        if (DEBUG)
            Log.v(TAG, "sendBroadcast:isPausing=" + isPausing);
        sendBroadcast(result);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG)
            Log.v(TAG, "onStartCommand::");

        super.onStartCommand(intent, flags, startId);

//        synchronized (sSync) {
//        }
        final String action = intent.getAction();

            switch (action) {
                case ACTION_START:
                    if (mHyperionThread == null) {
                        prepare();
                        startScreenRecord(intent);
                        updateStatus();
                        startForeground(NOTIFICATION_ID, getNotification());
                    }
                    break;
                case ACTION_STOP:
                    stopScreenRecord();
                    break;
            }


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Notification getNotification() {
        Intent notificationIntent = new Intent(this, this.getClass());
        notificationIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        String label = "START GRABBER";
        if (mHyperionEncoder != null && mHyperionEncoder.isCapturing()) {
            label = "STOP GRABBER";
            notificationIntent.setAction(ACTION_STOP);
        } else {
            notificationIntent.setAction(ACTION_START);
        }

        HyperionNotification noti = new HyperionNotification(this, mNotificationManager);
        noti.setAction(label, notificationIntent);
        return noti.buildNotification();
    }


    /**
     * start screen recording as .mp4 file
     *
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {
//        synchronized (sSync) {
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
//        }
    }

    /**
     * stop screen recording
     */
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

    public interface HyperionThreadBroadcaster {
        void onResponse(String response);
        void onConnected();
        void onConnectionError(int errorHash, String errorString);
    }
}