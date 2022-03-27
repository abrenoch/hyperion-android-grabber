package com.abrenoch.hyperiongrabber.common;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.PixelFormat;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;
import com.abrenoch.hyperiongrabber.common.util.BorderProcessor;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class HyperionScreenEncoder extends HyperionScreenEncoderBase {
    private static final int MAX_IMAGE_READER_IMAGES = 5;
    private static final String TAG = "HyperionScreenEncoder";
    private static final boolean DEBUG = false;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    HyperionScreenEncoder(final HyperionThread.HyperionThreadListener listener,
                           final MediaProjection projection, final int width, final int height,
                           final int density, HyperionGrabberOptions options) {
        super(listener, projection, width, height, density, options);

        try {
            prepare();
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() throws MediaCodec.CodecException {
        if (DEBUG) Log.d(TAG, "Preparing encoder");

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                TAG,
                getGrabberWidth(), getGrabberHeight(), mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, mDisplayCallback, null);

        setImageReader();
    }

    @Override
    public void stopRecording() {
        if (DEBUG) Log.i(TAG, "stopRecording Called");
        setCapturing(false);
        mVirtualDisplay.release();
        mHandler.getLooper().quit();
        clearAndDisconnect();
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    public void resumeRecording() {
        if (DEBUG) Log.i(TAG, "resumeRecording Called");
        if (!isCapturing() && mImageReader != null) {
            if (DEBUG) Log.i(TAG, "Resuming reading images");
            Image img = mImageReader.acquireNextImage();
            setCapturing(true);
            if (img != null) {
                img.close();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setOrientation(int orientation) {
        if (mVirtualDisplay != null && orientation != mCurrentOrientation) {
            mCurrentOrientation = orientation;
            mIsCapturing = false;
            mVirtualDisplay.resize(getGrabberWidth(), getGrabberHeight(), mDensity);
            mImageReader.close();
            setImageReader();
        }
    }

    private VirtualDisplay.Callback mDisplayCallback = new VirtualDisplay.Callback() {
        @Override
        public void onPaused() {
            if (DEBUG) Log.d("DEBUG", "HyperionScreenEncoder.displayCallback.onPaused triggered");
            super.onPaused();
            setCapturing(false);
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
            setCapturing(false);
        }
    };

    @SuppressLint("WrongConstant") // incorrectly reports PixelFormat.RGBA_8888 as incompatible
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private void setImageReader() {
        if (DEBUG) Log.d(TAG, "Setting image reader  " + String.valueOf(isCapturing()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mImageReader = ImageReader.newInstance(getGrabberWidth(), getGrabberHeight(),
                    PixelFormat.RGBA_8888, MAX_IMAGE_READER_IMAGES, HardwareBuffer.USAGE_CPU_READ_OFTEN);
        } else {
            mImageReader = ImageReader.newInstance(getGrabberWidth(), getGrabberHeight(),
                    PixelFormat.RGBA_8888, MAX_IMAGE_READER_IMAGES);
        }
        mImageReader.setOnImageAvailableListener(imageAvailableListener, mHandler);
        mVirtualDisplay.setSurface(mImageReader.getSurface());
        setCapturing(true);
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
                    if (DEBUG) Log.w(TAG, "sendImage exception:", e);
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
            if (y < firstY || y > compareHeight) {
                offset += width * pixelStride;
                continue;
            }
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
            if (y < firstY || y > compareHeight) {
                offset += width * pixelStride;
                continue;
            }
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

        if (mRemoveBorders || mAvgColor) {
            mBorderProcessor.parseBorder(buffer, width, height, rowStride, pixelStride);
            BorderProcessor.BorderObject border = mBorderProcessor.getCurrentBorder();
            if (border != null && border.isKnown()) {
                firstX = border.getHorizontalBorderIndex();
                firstY = border.getVerticalBorderIndex();
            }
        }

        if (mAvgColor) {
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