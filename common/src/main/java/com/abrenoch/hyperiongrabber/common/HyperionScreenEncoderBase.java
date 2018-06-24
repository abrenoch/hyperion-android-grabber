package com.abrenoch.hyperiongrabber.common;

import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import java.util.ArrayList;
import java.util.List;

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

        // enforce we have whole even numbers (though it seems unlikely we wouldn't already)
        mWidth = (int) Math.floor(width);
        mHeight = (int) Math.floor(height);
        if (mWidth % 2 != 0) mWidth--;
        if (mHeight % 2 != 0) mHeight--;

        int divisor = findDivisor();
        mHeightScaled = (mHeight / divisor);
        mWidthScaled = (mWidth / divisor);

        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
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
