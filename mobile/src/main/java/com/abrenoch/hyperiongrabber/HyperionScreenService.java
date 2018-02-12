package com.abrenoch.hyperiongrabber;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;

public class HyperionScreenService extends IntentService {
    private static final boolean DEBUG = false;
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
    private static final String NOTIFICATION_CHANNEL_ID = BASE + "NOTIFICATION";

    public static final String ACTION_RELEASE_RESOURCE = BASE + "ACTION_RELEASE_RESOURCE";
    private static final int FOREGROUND_ID = 4568;

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
        public void onConnectionError(String error) {
            Log.e("ERROR", "COULD NOT CONNECT TO HYPERION INSTANCE");
            if (error != null) Log.e("ERROR", error);
        }

        @Override
        public void onResponse(String response) {

        }
    };



    public HyperionScreenService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
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

        super.onCreate();

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mHyperionThread = new HyperionThread(mReceiver, host, Integer.parseInt(port), Integer.parseInt(priority));
        mHyperionThread.start();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (DEBUG) Log.v(TAG, "onHandleIntent:intent=" + intent);
        final String action = intent.getAction();


        if (ACTION_START.equals(action)) {

            startForeground(FOREGROUND_ID, getNotification());

            startScreenRecord(intent);
            updateStatus();
        } else if (ACTION_STOP.equals(action)) {
            stopScreenRecord();
            updateStatus();
        } else if (ACTION_QUERY_STATUS.equals(action)) {
            updateStatus();
        } else if (ACTION_PAUSE.equals(action)) {
            pauseScreenRecord();
            updateStatus();
        } else if (ACTION_RESUME.equals(action)) {
            resumeScreenRecord();
            updateStatus();
        } else if (ACTION_RELEASE_RESOURCE.equals(action)) {
            releaseResource();
        }
    }

    private void updateStatus() {
        final boolean isRecording, isPausing;
        synchronized (sSync) {
            isPausing = false;
        }
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
        super.onStartCommand(intent, flags, startId);
        return START_STICKY ;
    }

    public Notification getNotification() {

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Channel description");
//            notificationChannel.enableLights(true);
//            notificationChannel.setLightColor(null);
//            notificationChannel.setVibrationPattern(null);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null,null);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent notificationIntent = new Intent(this, HyperionScreenService.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setVibrate(null)
                .setSound(null)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Content Title")
                .setContentText("Content Text");

        Notification n = builder.build();

        notificationManager.notify(NOTIFICATION_ID, n);

        return n;
    }


    /**
     * start screen recording as .mp4 file
     *
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {
        synchronized (sSync) {
            final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            // get MediaProjection
            final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
            if (projection != null) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                final int density = metrics.densityDpi;

                _mediaProjection = projection;

                if (DEBUG) Log.v(TAG, "startRecording:");

                new HyperionScreenEncoder(mHyperionThread.getReceiver(),
                        projection, metrics.widthPixels, metrics.heightPixels,
                        density, mFrameRate);
            }
        }
    }

    /**
     * stop screen recording
     */
    private void stopScreenRecord() {
//        if (DEBUG) Log.v(TAG, "stopScreenRecord:sMuxer=" + sMuxer);
        synchronized (sSync) {
//            if (sMuxer != null) {
//                sMuxer.stopRecording();
//                sMuxer = null;
//                // you should not wait here
//            }
        }
    }

    private void pauseScreenRecord() {
        synchronized (sSync) {
//            if (sMuxer != null) {
//                sMuxer.pauseRecording();
//            }
        }
    }

    private void resumeScreenRecord() {
        synchronized (sSync) {
//            if (sMuxer != null) {
//                sMuxer.resumeRecording();
//            }
        }
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
        void onConnectionError(String error);
    }
}