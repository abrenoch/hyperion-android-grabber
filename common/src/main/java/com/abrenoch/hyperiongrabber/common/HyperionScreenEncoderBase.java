package com.abrenoch.hyperiongrabber.common;

import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;

public class HyperionScreenEncoderBase {
    private static final String TAG = "HyperionScreenEncoderBase";
    private static final int TARGET_HEIGHT = 60;
    private static final int TARGET_WIDTH = 60;
    private static final int TARGET_BIT_RATE = TARGET_HEIGHT * TARGET_WIDTH * 3;
    private int mHeight;
    private int mWidth;
    int mWidthScaled;
    int mFrameRate;
    int mHeightScaled;
    int mDensity;

    Handler mHandler;

    boolean mIsCapturing = false;
    MediaProjection mMediaProjection;
    HyperionThread.HyperionThreadListener mListener;

    HyperionScreenEncoderBase(final HyperionThread.HyperionThreadListener listener,
                              final MediaProjection projection, final int width, final int height,
                              final int density, int frameRate) {
        mListener = listener;
        mMediaProjection = projection;
        mDensity = density;
        mFrameRate = frameRate;

        mWidth = (int) Math.floor(width);
        mHeight = (int) Math.floor(height);
        if (mWidth % 2 != 0) mWidth--;
        if (mHeight % 2 != 0) mHeight--;

        float scale = findScaleFactor();
        mWidthScaled = (int) (mWidth / scale);
        mHeightScaled = (int) (mHeight / scale);

        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
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

    public void stopRecording() {
        throw new RuntimeException("Stub!");
    }

    public void resumeRecording() {
        throw new RuntimeException("Stub!");
    }

    private float findScaleFactor() {
        float step = (float) 0.2;
        for (float i = 1; i < 100; i += step) {
            if ((mWidth / i) * (mHeight / i) * 3 <= TARGET_BIT_RATE) {
                return i;
            }
        }
        return 1;
    }
}
