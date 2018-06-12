package com.abrenoch.hyperiongrabber.common;

import android.media.Image;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import java.nio.ByteBuffer;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;

public class HyperionScreenEncoderBase {
    private static final String TAG = "HyperionScreenEncoderBase";
    private static final int TARGET_HEIGHT = 60;
    private static final int TARGET_WIDTH = 60;
    private static final int TARGET_BIT_RATE = TARGET_HEIGHT * TARGET_WIDTH * 3;
    int mHeight;
    int mWidth;
    int mWidthScaled;
    int mFrameRate;
    int mHeightScaled;
    int mDensity;
    int mBlackThreshold = 5; // unit to detect how black a pixel is [0..255]

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

    public BorderObject findBorder(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        return this.findBorder(buffer, width, height, rowStride, pixelStride);
    }

    public BorderObject findBorder(ByteBuffer buffer, int width, int height, int rowStride,
                                   int pixelStride) {

//        int rowPadding = rowStride - width * pixelStride;

        int width33percent = width / 3;
        int width66percent = width33percent * 2;
        int height33percent = height / 3;
        int height66percent = height33percent * 2;
        int yCenter = height / 2;
        int xCenter = width / 2;
        int firstNonBlackYPixelIndex = -1;
        int firstNonBlackXPixelIndex = -1;

        int c1R, c1G, c1B;
        int c2R, c2G, c2B;
        int c3R, c3G, c3B;
        int pos33p, pos66p;

        buffer.position(0).mark();

        int offset = 0;
        for (int x = 0, posYCLp; x < width33percent; x++) {

            // RGB values at 33% height - to left of image
            pos33p = (height33percent * rowStride) + (x * pixelStride);
//            pos33p = offset + height33percent * rowStride;
            c1R = buffer.get(pos33p) & 0xff;
            c1G = buffer.get(pos33p + 1) & 0xff;
            c1B = buffer.get(pos33p + 2) & 0xff;

            // RGB values at 66% height - to left of image
            pos66p = (height66percent * rowStride) + (x * pixelStride);
//            pos66p = offset + height66percent * rowStride;
            c2R = buffer.get(pos66p) & 0xff;
            c2G = buffer.get(pos66p + 1) & 0xff;
            c2B = buffer.get(pos66p + 2) & 0xff;

            // RGB values at center Y - to right of image
//            posYCLp = (offset + yCenter * rowStride) + (rowStride - rowPadding - (pixelStride * 2) * (x + 1));
            posYCLp = (yCenter * rowStride) + ((width - x - 1) * pixelStride);
            c3R = buffer.get(posYCLp) & 0xff;
            c3G = buffer.get(posYCLp + 1) & 0xff;
            c3B = buffer.get(posYCLp + 2) & 0xff;

            // check if any of our RGB values DO NOT evaluate as black
            if (!isBlack(c1R,c1G,c1B) || !isBlack(c2R,c2G,c2B) || !isBlack(c3R,c3G,c3B)) {
                firstNonBlackXPixelIndex = x;
                break;
            }

            // increase the offset by the pixel stride
//            offset += pixelStride;
        }

        buffer.reset();
        offset = 0;
        for (int y = 0, posBCLp; y < height33percent; y++) {

            // RGB values at 33% width - top of image
//            pos33p = offset + width33percent * pixelStride;
            pos33p = (width33percent * pixelStride) + (y * rowStride);
            c1R = buffer.get(pos33p) & 0xff;
            c1G = buffer.get(pos33p + 1) & 0xff;
            c1B = buffer.get(pos33p + 2) & 0xff;

            // RGB values at 66% width - top of image
//            pos66p = offset + width66percent * pixelStride;
            pos66p = (width66percent * pixelStride) + (y * rowStride);
            c2R = buffer.get(pos66p) & 0xff;
            c2G = buffer.get(pos66p + 1) & 0xff;
            c2B = buffer.get(pos66p + 2) & 0xff;

            // RGB values at center X - bottom of image
//            posBCLp = offset + ((height - y - 1) * (rowStride * 2)) + xCenter * pixelStride;
            posBCLp = (xCenter * pixelStride) + ((height - y - 1) * rowStride);
            c3R = buffer.get(posBCLp) & 0xff;
            c3G = buffer.get(posBCLp + 1) & 0xff;
            c3B = buffer.get(posBCLp + 2) & 0xff;

            // check if any of our RGB values DO NOT evaluate as black
            if (!isBlack(c1R,c1G,c1B) || !isBlack(c2R,c2G,c2B) || !isBlack(c3R,c3G,c3B)) {
                firstNonBlackYPixelIndex = y;
                break;
            }

            // increase the offset by the row stride
//            offset += rowStride;
        }

        return new BorderObject(firstNonBlackXPixelIndex, firstNonBlackYPixelIndex);
    }

    private boolean isBlack(int red, int green, int blue) {
        return red < mBlackThreshold && green < mBlackThreshold && blue < mBlackThreshold;
    }

    public class BorderObject {
        public boolean isKnown;
        public int horizontalBorderIndex;
        public int verticalBorderIndex;
        public BorderObject(int horizontalBorderIndex, int verticalBorderIndex) {
            this.horizontalBorderIndex = horizontalBorderIndex;
            this.verticalBorderIndex = verticalBorderIndex;
            this.isKnown = this.horizontalBorderIndex != -1 && this.verticalBorderIndex != -1;
        }
        public String toString() {
            if (this.isKnown) {
                return "Border information: firstX:" + String.valueOf(this.horizontalBorderIndex) +
                       " firstY: " + String.valueOf(this.verticalBorderIndex);
            }
            return "Border information: unknown";
        }
    }
}
