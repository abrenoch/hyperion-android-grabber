package com.abrenoch.hyperiongrabber.common.util;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class HyperionGrabberOptions {
    private static final boolean DEBUG = false;
    private static final String TAG = "HyperionGrabberOptions";

    private final int MINIMUM_IMAGE_PACKET_SIZE; // how many bytes the minimal acceptable image quality is
    private final int FRAME_RATE;
    private final boolean USE_AVERAGE_COLOR;
    private final int BLACK_THRESHOLD = 5; // The limit each RGB value must be under to be considered a black pixel [0-255]

    private final String SAMPLING_METHOD;
    private final int SAMPLING_RATE;
    private final String START_EFFECT;
    private final float TURNAROUND_RATE;
    private final int TURNAROUND_OFFSET;

    public HyperionGrabberOptions(int horizontalLED, int verticalLED, int frameRate, boolean useAvgColor) {

        /*
        * To determine the minimal acceptable image packet size we take the count of the width & height
        * of the LED pixels (that the user is driving via their hyperion server) and multiply them
        * together and then by 3 (1 for each color in RGB). This will give us the count of the bytes
        * that the minimal acceptable quality should be equal to or greater than.
        **/
        MINIMUM_IMAGE_PACKET_SIZE = horizontalLED * verticalLED * 3;
        FRAME_RATE = frameRate;
        USE_AVERAGE_COLOR = useAvgColor;

        /*
         * no input for videoMode
        */
        SAMPLING_METHOD = "NONE";
        SAMPLING_RATE = 0;
        START_EFFECT = "NONE";
        TURNAROUND_RATE = 0.0f;
        TURNAROUND_OFFSET = 0;

        if (DEBUG) {
            Log.d(TAG, "Video Mode Options ");
            Log.d(TAG, "Horizontal LED Count: " + String.valueOf(horizontalLED));
            Log.d(TAG, "Vertical LED Count: " + String.valueOf(verticalLED));
            Log.d(TAG, "Minimum Image Packet: " + String.valueOf(MINIMUM_IMAGE_PACKET_SIZE));
        }
    }

    //AudioMode
    public HyperionGrabberOptions(int horizontalLED, int verticalLED, String samplingMethod, int samplingRate, String startEffect, float turnAroundRate, int turnaroundOffset) {
        MINIMUM_IMAGE_PACKET_SIZE = horizontalLED * verticalLED * 3;
        SAMPLING_METHOD = samplingMethod;
        SAMPLING_RATE = samplingRate;
        START_EFFECT = startEffect;
        TURNAROUND_RATE = turnAroundRate;
        TURNAROUND_OFFSET = turnaroundOffset;

        /*
         * no input for audioMode
        */
        FRAME_RATE = 0;
        USE_AVERAGE_COLOR = false;

        if (DEBUG) {
            Log.d(TAG, "Audio Mode Options ");
            Log.d(TAG, "Horizontal LED Count: " + String.valueOf(horizontalLED));
            Log.d(TAG, "Vertical LED Count: " + String.valueOf(verticalLED));
            Log.d(TAG, "Minimum Image Packet: " + String.valueOf(MINIMUM_IMAGE_PACKET_SIZE));
            Log.d(TAG, "Sampling Rate: " + String.valueOf(SAMPLING_RATE));
            Log.d(TAG, "Turnaround Rate: " + String.valueOf(TURNAROUND_RATE));
        }
    }

    // pixelCount for AudioGradientView = scaledDimension
    public HyperionGrabberOptions(int horizontalLED, int verticalLED) {
        MINIMUM_IMAGE_PACKET_SIZE = horizontalLED * verticalLED * 3;

        // no need here
        FRAME_RATE = 0;
        USE_AVERAGE_COLOR = false;
        SAMPLING_METHOD = "NONE";
        SAMPLING_RATE = 0;
        START_EFFECT = "NONE";
        TURNAROUND_RATE = 0.0f;
        TURNAROUND_OFFSET = 0;
    }

    public int getFrameRate() { return FRAME_RATE; }

    public boolean useAverageColor() { return USE_AVERAGE_COLOR; }

    public String getSamplingMethod() { return SAMPLING_METHOD; }

    public int getSamplingRate() { return SAMPLING_RATE; }

    public String getStartEffect() { return START_EFFECT; }

    public float getTurnaroundRate() { return TURNAROUND_RATE; }

    public int getTurnaroundOffset() { return TURNAROUND_OFFSET; }

    /**
    * returns the divisor best suited to be used to meet the minimum image packet size
    *     Since we only want to scale using whole numbers, we need to find what common divisors
    *     are available for the given width & height. We will check those divisors to find the smallest
    *     number (that we can divide our screen dimensions by) that would meet the minimum image
    *     packet size required to match the count of the LEDs on the destination hyperion server.
    * @param width The original width of the device screen
    * @param height  The original height of the device screen
    * @return int The divisor bes suited to scale the screen dimensions by
    **/
    public int findDivisor(int width, int height) {
        List<Integer> divisors = getCommonDivisors(width, height);
        if (DEBUG) Log.d(TAG, "Available Divisors: " + divisors.toString());
        ListIterator it = divisors.listIterator(divisors.size());

        // iterate backwards since the divisors are listed largest to smallest
        while (it.hasPrevious()) {
            int i = (int) it.previous();

            // check if the image packet size for this divisor is >= the minimum image packet size
            // like above we multiply the dimensions together and then by 3 for each byte in RGB
            if ((width / i) * (height / i) * 3 >= MINIMUM_IMAGE_PACKET_SIZE)
                return i;
        }
        return 1;
    }

    /**
     * gets a list of all the common divisors [large to small] for the given integers.
     * @param num1 The first integer to find a whole number divisor for
     * @param num2  The second integer to find a whole number divisor for
     * @return List A list of the common divisors [large to small] that match the provided integers
     **/
    private static List<Integer> getCommonDivisors(int num1, int num2) {
        List<Integer> list = new ArrayList<>();
        int min = Math.min(num1, num2);
        for (int i = 1; i <= min / 2; i++)
            if (num1 % i == 0 && num2 % i == 0)
                list.add(i);
        if (num1 % min == 0 && num2 % min == 0) list.add(min);
        return list;
    }

    public int getBlackThreshold() { return BLACK_THRESHOLD; }
}
