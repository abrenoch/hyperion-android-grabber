package com.abrenoch.hyperiongrabber.common;

import android.content.res.Configuration;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

public class HyperionScreenEncoderBase {
    private static final boolean DEBUG = false;
    private static final String TAG = "ScreenEncoderBase";
    private final int INIT_ORIENTATION;
    int mWidthScaled;
    int mFrameRate;
    int mHeightScaled;
    int mDensity;
    int mCurrentOrientation;
    Handler mHandler;

    boolean mIsCapturing = false;
    MediaProjection mMediaProjection;
    HyperionThread.HyperionThreadListener mListener;

    HyperionScreenEncoderBase(final HyperionThread.HyperionThreadListener listener,
                              final MediaProjection projection, int width, int height,
                              final int density, HyperionGrabberOptions options) {
        if (DEBUG) Log.d(TAG, "Encoder starting");

        mListener = listener;
        mMediaProjection = projection;
        mDensity = density;
        mFrameRate = options.getFrameRate();

        mCurrentOrientation = INIT_ORIENTATION = width > height ? Configuration.ORIENTATION_LANDSCAPE :
                Configuration.ORIENTATION_PORTRAIT;

        if (DEBUG) {
            Log.d(TAG, "Density: " + String.valueOf(mDensity));
            Log.d(TAG, "Frame Rate: " + String.valueOf(mFrameRate));
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
            mListener.clear();
            mListener.disconnect();
        }
    };

    private Runnable clearLightsRunner = new Runnable() {
        public void run() {
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

    int getGrabberWidth() {
        return INIT_ORIENTATION != mCurrentOrientation ? mHeightScaled : mWidthScaled;
    }

    int getGrabberHeight() {
        return INIT_ORIENTATION != mCurrentOrientation ? mWidthScaled : mHeightScaled;
    }

    public void setOrientation(int orientation) {
        mCurrentOrientation = orientation;
    }
}