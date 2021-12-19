package com.abrenoch.hyperiongrabber.common.audio;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.abrenoch.hyperiongrabber.common.HyperionNotification;
import com.abrenoch.hyperiongrabber.common.R;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;
import com.abrenoch.hyperiongrabber.common.util.Preferences;

import java.util.Arrays;
import java.util.Objects;

public class HyperionAudioService extends Service {
    public static final String BROADCAST_ERROR = "SERVICE_ERROR";
    public static final String BROADCAST_TAG = "SERVICE_STATUS";
    public static final String BROADCAST_FILTER = "SERVICE_FILTER";
    public static final String BROADCAST_COLORS = "SERVICE_COLORS";
    public static final String BROADCAST_X = "SERVICE_X";
    private static final boolean DEBUG = false;
    private static final String TAG = "HyperionAudioService";

    private static final String BASE = "com.abrenoch.hyperiongrabber.service.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_EXIT = BASE + "ACTION_EXIT";
    public static final String GET_STATUS = BASE + "ACTION_STATUS";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_EXIT_INTENT_ID = 2;

    private boolean RECONNECT = false;
    private boolean hasConnected = false;
    private int mHorizontalLEDCount;
    private int mVerticalLEDCount;
    private NotificationManager mNotificationManager;
    private String mStartError = null;

    private String mSamplingMethod;
    private int mSamplingRate;
    private String mStartEffect;
    private float mTurnAroundRate;
    private int mOffsetEffect;

    private HyperionAudioEncoder mHyperionAudioEncoder;
    private HyperionAudioThread mHyperionAudioThread;

    HyperionAudioThreadBroadcaster mReceiver = new HyperionAudioThreadBroadcaster() {
        @Override
        public void onConnected() {
            Log.d(TAG, "CONNECTED TO HYPERION INSTANCE");
            hasConnected = true;
            notifyActivity();
        }

        @Override
        public void onConnectionError(int errorID, String error) {
            Log.e(TAG, "COULD NOT CONNECT TO HYPERION INSTANCE");
            if (error != null) Log.e(TAG, error);
            if (!hasConnected) {
                mStartError = getResources().getString(R.string.error_server_unreachable);
                haltStartup();
            }
            if (RECONNECT && hasConnected) {
                Log.e(TAG, "AUTOMATIC RECONNECT ENABLED. CONNECTING ...");
            } else if (!RECONNECT && hasConnected) {
                mStartError = getResources().getString(R.string.error_connection_lost);
                stopSelf();
            }
        }

        @Override
        public void onReceiveStatus(boolean isCapturing) {
            if (DEBUG) Log.v(TAG, "Received grabber status, notifying activity. Status: " +
                    String.valueOf(isCapturing));
            notifyActivity();
        }

    };

    HyperionAudioEncoderBroadcaster mAudioReceiver = () -> {
        if (DEBUG) Log.d(TAG, "SEND COLORS");
        sendColors();
    };

    BroadcastReceiver mEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (Objects.requireNonNull(intent.getAction())) {
                case Intent.ACTION_SCREEN_ON:
                    if (DEBUG) Log.v(TAG, "ACTION_SCREEN_ON intent received");
                    if (mHyperionAudioEncoder != null && !isCapturing()) {
                        if (DEBUG) Log.v(TAG, "Encoder not grabbing, attempting to restart");
                        mHyperionAudioEncoder.resumeRecording();
                    }
                    notifyActivity();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (DEBUG) Log.v(TAG, "ACTION_SCREEN_OFF intent received");
                    if (mHyperionAudioEncoder != null) {
                        if (DEBUG) Log.v(TAG, "Clearing current light data");
                        mHyperionAudioEncoder.clearLights();
                    }
                    break;
                case Intent.ACTION_CONFIGURATION_CHANGED:
                    if (DEBUG) Log.v(TAG, "ACTION_CONFIGURATION_CHANGED intent received");
                    if (mHyperionAudioEncoder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (DEBUG) Log.v(TAG, "Configuration changed, checking orientation");
                        mHyperionAudioEncoder.setOrientation(getResources().getConfiguration().orientation);
                    }
                    break;
                case Intent.ACTION_SHUTDOWN:
                case Intent.ACTION_REBOOT:
                    if (DEBUG) Log.v(TAG, "ACTION_SHUTDOWN|ACTION_REBOOT intent received");
                    stopAudioRecord();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {

        // #todo: edit HyperionNotiifications for Audio
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean prepared() {
        Preferences prefs = new Preferences(getBaseContext());
        String host = prefs.getString(R.string.pref_key_host, null);
        int port = prefs.getInt(R.string.pref_key_port, -1);
        String priority = prefs.getString(R.string.pref_key_priority, "50");

        mHorizontalLEDCount = prefs.getInt(R.string.pref_key_x_led);
        mVerticalLEDCount = prefs.getInt(R.string.pref_key_y_led);
        // #todo: add to SettingsActivity: samplingrate, tournaroundrate, ledcount
        mSamplingMethod = prefs.getString(R.string.pref_key_sampling_method, "Waveform");
        mSamplingRate = prefs.getInt(R.string.pref_key_sampling_rate);
        mStartEffect = prefs.getString(R.string.pref_key_effect, "Plasma");
        mTurnAroundRate = (float)prefs.getInt(R.string.pref_key_turnaround_rate)/100.0f;
        mOffsetEffect = prefs.getInt(R.string.pref_key_turnaround_offset);
//        Log.d("new settings", mSamplingMethod + ", " +
//                Integer.toString(mSamplingRate) + ", " +
//                mStartEffect + ", " +
//                Float.toString(mTurnAroundRate) + ", " +
//                Integer.toString(mOffsetEffect));

        RECONNECT = prefs.getBoolean(R.string.pref_key_reconnect);
        int delay = prefs.getInt(R.string.pref_key_reconnect_delay);
        if (host == null || Objects.equals(host, "0.0.0.0") || Objects.equals(host, "")) {
            mStartError = getResources().getString(R.string.error_empty_host);
            return false;
        }
        if (port == -1) {
            mStartError = getResources().getString(R.string.error_empty_port);
            return false;
        }
        if (mHorizontalLEDCount <= 0 || mVerticalLEDCount <= 0) {
            mStartError = getResources().getString(R.string.error_invalid_led_counts);
            return false;
        }

        mHyperionAudioThread = new HyperionAudioThread(mReceiver, host, port, Integer.parseInt(priority), RECONNECT, delay);
        mHyperionAudioThread.start();
        mStartError = null;
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.v(TAG, "Start command received");
        super.onStartCommand(intent, flags, startId);
        if (intent == null || intent.getAction() == null) {
            String nullItem = (intent == null ? "intent" : "action");
            if (DEBUG) Log.v(TAG, "Null " + nullItem + " provided to start command");
        } else  {
            final String action = intent.getAction();
            if (DEBUG) Log.v(TAG, "Start command action: " + String.valueOf(action));
            switch (action) {
                case ACTION_START:
                    if (mHyperionAudioThread == null) {
                        boolean isPrepared = prepared();
                        if (isPrepared) {
                            startAudioRecord(intent);
                            startForeground(NOTIFICATION_ID, getNotification());

                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
                            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
                            intentFilter.addAction(Intent.ACTION_REBOOT);
                            intentFilter.addAction(Intent.ACTION_SHUTDOWN);

                            registerReceiver(mEventReceiver, intentFilter);
                        } else {
                            haltStartup();
                        }
                    }
                    break;
                case ACTION_STOP:
                    stopAudioRecord();
                    break;
                case GET_STATUS:
                    notifyActivity();
                    break;
                case ACTION_EXIT:
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

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "Ending service");

        try {
            unregisterReceiver(mEventReceiver);
        } catch (Exception e) {
            if (DEBUG) Log.v(TAG, "Wake receiver not registered");
        }

        stopAudioRecord();
        stopForeground(true);
        notifyActivity();

        super.onDestroy();
    }

    private void haltStartup() {
        startForeground(NOTIFICATION_ID, getNotification());
        stopSelf();
    }

    private Intent buildExitButton() {
        Intent notificationIntent = new Intent(this, this.getClass());
        notificationIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        notificationIntent.setAction(ACTION_EXIT);
        return notificationIntent;
    }

    public Notification getNotification() {
        HyperionNotification notification = new HyperionNotification(this, mNotificationManager);
        String label = getString(R.string.notification_exit_button);
        notification.setAction(NOTIFICATION_EXIT_INTENT_ID, label, buildExitButton());
        return notification.buildNotification();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startAudioRecord(final Intent intent) {
        if (DEBUG) Log.v(TAG, "Start audio recorder");
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (window != null) {
            final DisplayMetrics metrics = new DisplayMetrics();
            window.getDefaultDisplay().getRealMetrics(metrics);
            HyperionGrabberOptions options = new HyperionGrabberOptions(mHorizontalLEDCount,
                    mVerticalLEDCount, mSamplingMethod, mSamplingRate, mStartEffect, mTurnAroundRate, mOffsetEffect);
            if (DEBUG) Log.v(TAG, "Starting the recorder");
            mHyperionAudioEncoder = new HyperionAudioEncoder(mHyperionAudioThread.getReceiver(), mAudioReceiver,
                    metrics.widthPixels, metrics.heightPixels, options);
            mHyperionAudioEncoder.sendStatus();
        }
    }

    private void stopAudioRecord() {

        if (DEBUG) Log.v(TAG, "Stop audio recorder");
        RECONNECT = false;
        mNotificationManager.cancel(NOTIFICATION_ID);
        if (mHyperionAudioEncoder != null) {
            if (DEBUG) Log.v(TAG, "Stopping the current encoder");
            mHyperionAudioEncoder.stopRecording();
        }
        releaseResource();
        if (mHyperionAudioThread != null) {
            mHyperionAudioThread.interrupt();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResource() {
        // #todo: close get AudioCapture from Encoder and stop
//        if (_mediaProjection != null) {
//            _mediaProjection.stop();
//            _mediaProjection = null;
//        }
    }

    boolean isCapturing() {

        return mHyperionAudioEncoder != null && mHyperionAudioEncoder.isCapturing();
    }

    boolean isCommunicating() {

        return isCapturing() && hasConnected;
    }

    int[] getColors() {
        return mHyperionAudioEncoder.getColors();
    }

    private void notifyActivity() {
        Intent intent = new Intent(BROADCAST_FILTER);
        intent.putExtra(BROADCAST_TAG, isCommunicating());
        intent.putExtra(BROADCAST_ERROR, mStartError);
        if (DEBUG) {
            Log.v(TAG, "Sending status broadcast - communicating: " +
                    String.valueOf(isCommunicating()));
            if (mStartError != null) {
                Log.v(TAG, "Startup error: " + mStartError);
            }
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void sendColors() {
        Intent intent = new Intent(BROADCAST_X);
        intent.putExtra(BROADCAST_COLORS, getColors());
        if (DEBUG) {
            Log.v(TAG, "Sending colors broadcast - capturing: " +
                    Arrays.toString( getColors() )  );

        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public interface HyperionAudioThreadBroadcaster {
        //        void onResponse(String response);
        void onConnected();
        void onConnectionError(int errorHash, String errorString);
        void onReceiveStatus(boolean isCapturing);

    }

    public interface HyperionAudioEncoderBroadcaster {
        void onSendColors();
    }
}
