package com.abrenoch.hyperiongrabber.common;

import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;
import com.abrenoch.hyperiongrabber.common.util.BorderProcessor;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HyperionScreenEncoder extends HyperionScreenEncoderBase {
    private static final int MAX_IMAGE_READER_IMAGES = 5;
    private static final String TAG = "HyperionScreenEncoder";
    private static final boolean DEBUG = false;
    private static boolean BORDER_DETECTION_ENABLED = true; // enables detecting borders for standard grabbing
    private boolean USE_AVERAGE_COLOR = false; // if true will send only average color of screen
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private BorderProcessor mBorderProcessor;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    HyperionScreenEncoder(final HyperionThread.HyperionThreadListener listener,
                           final MediaProjection projection, final int width, final int height,
                           final int density, HyperionGrabberOptions options) {
        super(listener, projection, width, height, density, options);

        mBorderProcessor = new BorderProcessor(5);

        try {
            prepare();
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() throws MediaCodec.CodecException {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "Capturing Display",
                mWidthScaled, mHeightScaled, mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, mDisplayCallback, null);

        setImageReader();
    }

    @Override
    public void stopRecording() {
        if (DEBUG) Log.i(TAG, "stopRecording Called");
        mIsCapturing = false;
        mVirtualDisplay.release();
        mHandler.getLooper().quit();
        clearAndDisconnect();
        mImageReader.close();
        mImageReader = null;
    }

    @Override
    public void resumeRecording() {
        if (!isCapturing() && mImageReader != null) {
            Image img = mImageReader.acquireNextImage();
            mIsCapturing = true;
            if (img != null) {
                img.close();
            }
        }
    }

    private VirtualDisplay.Callback mDisplayCallback = new VirtualDisplay.Callback() {
        @Override
        public void onPaused() {
            if (DEBUG) Log.d("DEBUG", "HyperionScreenEncoder.displayCallback.onPaused triggered");
            super.onPaused();
            mIsCapturing = false;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
        @Override
        public void onResumed() {
            if (DEBUG) Log.d("DEBUG", "HyperionScreenEncoder.displayCallback.onResumed triggered");
            super.onResumed();
            resumeRecording();
        }

        @Override
        public void onStopped() {
            if (DEBUG) Log.d("DEBUG", "HyperionScreenEncoder.displayCallback.onStopped triggered");
            super.onStopped();
            mIsCapturing = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private void setImageReader() {
        mImageReader = ImageReader.newInstance(mWidthScaled, mHeightScaled,
                PixelFormat.RGBA_8888, MAX_IMAGE_READER_IMAGES);
        mImageReader.setOnImageAvailableListener(imageAvailableListener, mHandler);
        mVirtualDisplay.setSurface(mImageReader.getSurface());
        mIsCapturing = true;
    }

    private OnImageAvailableListener imageAvailableListener = new OnImageAvailableListener() {
        double min_nano_time = 1e9 / mFrameRate;
        long lastFrame;

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (mListener != null && isCapturing()) {
                try {
                    long now = System.nanoTime();
                    Image img = reader.acquireLatestImage();
                    if (img != null && now - lastFrame >= min_nano_time) {
                        sendImage(img);
                        img.close();
                        lastFrame = now;
                    } else if (img != null) {
                        img.close();
                    }
                } catch (final Exception e) {
                    Log.e(TAG, "sendImage exception:", e);
                }
            }
        }
    };

    private byte[] getPixels(ByteBuffer buffer, int width, int height, int rowStride,
                             int pixelStride, int firstX, int firstY){
        int rowPadding = rowStride - width * pixelStride;
        int offset = 0;

        ByteArrayOutputStream bao = new ByteArrayOutputStream(
                (width - firstX * 2) * (height - firstY * 2) * 3
        );

        for (int y = 0, compareHeight = height - firstY - 1; y < height; y++, offset += rowPadding) {
            if (y < firstY || y > compareHeight) continue;
            for (int x = 0, compareWidth = width - firstX - 1; x < width; x++, offset += pixelStride) {
                if (x < firstX || x > compareWidth) continue;
                bao.write(buffer.get(offset) & 0xff); // R
                bao.write(buffer.get(offset + 1) & 0xff); // G
                bao.write(buffer.get(offset + 2) & 0xff); // B
            }
        }

        return bao.toByteArray();
    }

    private byte[] getAverageColor(ByteBuffer buffer, int width, int height, int rowStride,
                                   int pixelStride, int firstX, int firstY) {
        long totalRed = 0, totalGreen = 0, totalBlue = 0;
        int rowPadding = rowStride - width * pixelStride;
        int pixelCount = 0;
        int offset = 0;

        ByteArrayOutputStream bao = new ByteArrayOutputStream(3);

        for (int y = 0, compareHeight = height - firstY - 1; y < height; y++, offset += rowPadding) {
            if (y < firstY || y > compareHeight) continue;
            for (int x = 0, compareWidth = width - firstX - 1; x < width; x++, offset += pixelStride) {
                if (x < firstX || x > compareWidth) continue;
                totalRed += buffer.get(offset) & 0xff; // R
                totalGreen += buffer.get(offset + 1) & 0xff; // G
                totalBlue += buffer.get(offset + 2) & 0xff; // B
                pixelCount++;
            }
        }

        bao.write((int) totalRed / pixelCount);
        bao.write((int) totalGreen / pixelCount);
        bao.write((int) totalBlue / pixelCount);

        return bao.toByteArray();
    }

    private void sendImage(Image img) {
        Image.Plane plane = img.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();

        int width = img.getWidth();
        int height = img.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int firstX = 0;
        int firstY = 0;

        if (BORDER_DETECTION_ENABLED || USE_AVERAGE_COLOR) {
            mBorderProcessor.parseBorder(buffer, width, height, rowStride, pixelStride);
            BorderProcessor.BorderObject border = mBorderProcessor.getCurrentBorder();
            if (border != null && border.isKnown()) {
                firstX = border.getHorizontalBorderIndex();
                firstY = border.getVerticalBorderIndex();
            }
        }

        if (USE_AVERAGE_COLOR) {
            mListener.sendFrame(
                    getAverageColor(buffer, width, height, rowStride, pixelStride, firstX, firstY),
                    1,
                    1
            );
        } else {
            mListener.sendFrame(
                    getPixels(buffer, width, height, rowStride, pixelStride, firstX, firstY),
                    width - firstX * 2,
                    height - firstY * 2
            );
        }
    }
}