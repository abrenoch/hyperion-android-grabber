package com.abrenoch.hyperiongrabber.common;

import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;

public class HyperionScreenEncoderBase {
    private static final boolean DEBUG = false;
    private static final String TAG = "ScreenEncoderBase";
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
        if (DEBUG) Log.d(TAG, "Encoder starting");

        mListener = listener;
        mMediaProjection = projection;
        mDensity = density;
        mFrameRate = frameRate;

        if (DEBUG) {
            Log.d(TAG, "Density: " + String.valueOf(mDensity));
            Log.d(TAG, "Frame Rate: " + String.valueOf(mFrameRate));
            Log.d(TAG, "Original Width: " + String.valueOf(width));
            Log.d(TAG, "Original Height: " + String.valueOf(height));
            Log.d(TAG, "Target Bitrate: " + String.valueOf(TARGET_BIT_RATE));
            Log.d(TAG, "Target Width: " + String.valueOf(TARGET_WIDTH));
            Log.d(TAG, "Target Height: " + String.valueOf(TARGET_HEIGHT));
        }

        // enforce we have whole even numbers
        mWidth = (int) Math.floor(width);
        mHeight = (int) Math.floor(height);
        if (mWidth % 2 != 0) mWidth--;
        if (mHeight % 2 != 0) mHeight--;

        if (DEBUG) {
            Log.d(TAG, "Rounded Width: " + String.valueOf(mWidth));
            Log.d(TAG, "Rounded Height: " + String.valueOf(mHeight));
        }

        int divisor = findDivisor();
        mHeightScaled = (mHeight / divisor);
        mWidthScaled = (mWidth / divisor);

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

    public boolean isCapturing() {
        return mIsCapturing;
    }

    public void stopRecording() {
        throw new RuntimeException("Stub!");
    }

    public void resumeRecording() {
        throw new RuntimeException("Stub!");
    }

    private int findDivisor() {
        List<Integer> divisors = getCommonDivisors(mWidth, mHeight);
        if (DEBUG) Log.d(TAG, "Available Divisors: " + divisors.toString());
        for (Integer divisor : divisors) {
            if ((mWidth / divisor) * (mHeight / divisor) * 3 <= TARGET_BIT_RATE) {
                return divisor;
            }
        }
        return 1;
    }

    private static List<Integer> getCommonDivisors(int num1, int num2) {
        List<Integer> list = new ArrayList<>();
        int min = minimum(num1, num2);
        for (int i = 1; i <= min / 2; i++) {
            if (num1 % i == 0 && num2 % i == 0) {
                list.add(i);
            }
        }
        if (num1 % min == 0 && num2 % min == 0) {
            list.add(min);
        }
        return list;
    }

    private static int minimum(int num1, int num2) {
        return num1 <= num2 ? num1 : num2;
    }
}
