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
        mImageReader.close();
        mImageReader = null;
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private void setImageReader() {
        mImageReader = ImageReader.newInstance(getGrabberWidth(), getGrabberHeight(),
                PixelFormat.RGBA_8888, MAX_IMAGE_READER_IMAGES);
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
                        mListener.sendFrame(savePixels(img), getGrabberWidth(), getGrabberHeight());
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

    private byte[] savePixels(Image image) throws IllegalStateException {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = plane.getPixelStride();
        int rowPadding = plane.getRowStride() - width * pixelStride;

        ByteArrayOutputStream bao = new ByteArrayOutputStream(width * height * 3);

        int offset = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                bao.write(buffer.get(offset)); // R
                bao.write(buffer.get(offset + 1)); // G
                bao.write(buffer.get(offset + 2)); // B
                offset += pixelStride;
            }
            offset += rowPadding;
        }

        return bao.toByteArray();
    }
}