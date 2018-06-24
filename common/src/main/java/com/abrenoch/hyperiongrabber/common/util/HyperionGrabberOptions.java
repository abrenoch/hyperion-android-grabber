package com.abrenoch.hyperiongrabber.common.util;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class HyperionGrabberOptions {
    private static final boolean DEBUG = false;
    private static final String TAG = "HyperionGrabberOptions";

    private final int MINIMUM_BIT_RATE;
    private final int FRAME_RATE;

    public HyperionGrabberOptions(int horizontalLED, int verticalLED, int frameRate) {
        MINIMUM_BIT_RATE = horizontalLED * verticalLED * 3;
        FRAME_RATE = frameRate;

        if (DEBUG) {
            Log.d(TAG, "Horizontal LED Count: " + String.valueOf(horizontalLED));
            Log.d(TAG, "Vertical LED Count: " + String.valueOf(verticalLED));
            Log.d(TAG, "Minimum Bitrate: " + String.valueOf(MINIMUM_BIT_RATE));
        }
    }

    public int getFrameRate() { return FRAME_RATE; }

    public int findDivisor(int width, int height) {
        List<Integer> divisors = getCommonDivisors(width, height);
        if (DEBUG) Log.d(TAG, "Available Divisors: " + divisors.toString());
        ListIterator it = divisors.listIterator(divisors.size());
        while (it.hasPrevious()) {
            int i = (int) it.previous();
            if ((width / i) * (height / i) * 3 >= MINIMUM_BIT_RATE) {
                return i;
            }
        }
        return 1;
    }

    private static List<Integer> getCommonDivisors(int num1, int num2) {
        List<Integer> list = new ArrayList<>();
        int min = Math.min(num1, num2);
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
}
