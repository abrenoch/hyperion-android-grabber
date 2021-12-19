package com.abrenoch.hyperiongrabber.common.audio;

import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

public class HyperionAudioEncoderBase {

    static final boolean DEBUG = false;
    private static final String TAG = "AudioEncoderBase";
    private final int INIT_ORIENTATION;

    public static VisualisationMethod VISUALIZATION_METHOD = VisualisationMethod.NONE;

    final int mWidthScaled;
    final int mHeightScaled;
    private final int CLEAR_COMMAND_DELAY_MS = 100;
    int mCurrentOrientation;
    boolean mIsCapturing = false;

    int[] mColors;
    final String mSamplingMethod;
    final int mSamplingRate;
    final String mStartEffect;
    final float mTurnAroundRate;
    final int mTurnAroundOffeset;

    HyperionAudioThread.HyperionThreadListener mListener;
    HyperionAudioService.HyperionAudioEncoderBroadcaster mSender;
    Handler mHandler;

    HyperionAudioEncoderBase(final HyperionAudioThread.HyperionThreadListener listener, HyperionAudioService.HyperionAudioEncoderBroadcaster sender,
                             int width, int height, HyperionGrabberOptions options) {
        if (DEBUG) Log.d(TAG, "Encoder starting");

        mListener = listener;
        mSender = sender;
        mSamplingMethod = options.getSamplingMethod();
        mSamplingRate = options.getSamplingRate();
        mStartEffect = options.getStartEffect();
        mTurnAroundRate = options.getTurnaroundRate();
        mTurnAroundOffeset = options.getTurnaroundOffset();
        mCurrentOrientation = INIT_ORIENTATION = width > height ? Configuration.ORIENTATION_LANDSCAPE :
                Configuration.ORIENTATION_PORTRAIT;

        if (DEBUG) {
            Log.d(TAG, "Sampling Rate: " + String.valueOf(mSamplingRate));
            Log.d(TAG, "Turnaround Rate: " + String.valueOf(mTurnAroundRate));
            Log.d(TAG, "Original Width: " + String.valueOf(width));
            Log.d(TAG, "Original Height: " + String.valueOf(height));

        }

        // find the common divisor for width & height best fit for the LED count (defined in options)
        int divisor = options.findDivisor(width, height);

        // set the scaled width & height based upon the found divisor
        mHeightScaled = (height / divisor);
        mWidthScaled = (width / divisor);

        if (DEBUG) {
            Log.d(TAG, "Common Divisor: " + String.valueOf(divisor));
            Log.d(TAG, "Scaled Width: " + String.valueOf(mWidthScaled));
            Log.d(TAG, "Scaled Height: " + String.valueOf(mHeightScaled));
        }

        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());

        if (DEBUG) Log.d(TAG, "Encoder ready");
    }

    private Runnable clearAndDisconnectRunner = new Runnable() {
        public void run() {
            if (DEBUG) Log.d(TAG, "Clearing LEDs and disconnecting");
            try {
                Thread.sleep(CLEAR_COMMAND_DELAY_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mListener.clear();
            mListener.disconnect();
        }
    };

    private Runnable clearLightsRunner = new Runnable() {
        public void run() {
            if (DEBUG) Log.d(TAG, "Clearing LEDs");
            try {
                Thread.sleep(CLEAR_COMMAND_DELAY_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mListener.clear();
        }
    };

    public void clearLights() {
        new Thread(clearLightsRunner).start();
    }

    void clearAndDisconnect() {
        new Thread(clearAndDisconnectRunner).start();
    }

    public boolean isCapturing() {
        return mIsCapturing;
    }

    public void setCapturing(boolean isCapturing) {
        mIsCapturing = isCapturing;
    }

    public void sendStatus() {
        if (mListener != null) {
            mListener.sendStatus(isCapturing());
        }
    }

    public void stopRecording() {
        throw new RuntimeException("Stub!");
    }

    public void resumeRecording() {
        throw new RuntimeException("Stub!");
    }

    public void setOrientation(int orientation) {
        mCurrentOrientation = orientation;
    }

    public int[] getColors(){
        return mColors;
    }

    public int[] getScaledDimension () { return new int[]{mWidthScaled, mHeightScaled}; }

    public enum VisualisationMethod {
        NONE,
        RAINBOW_SWIRL,
        RAINBOW_MOD,
        ICEBLUE,
        RGBWHITE,
        PLASMA
    }

}
