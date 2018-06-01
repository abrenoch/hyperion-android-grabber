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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HyperionScreenEncoder extends HyperionScreenEncoderBase {
    private static final int MAX_IMAGE_READER_IMAGES = 5;
    private static final String TAG = "HyperionScreenEncoder";
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    HyperionScreenEncoder(final HyperionThread.HyperionThreadListener listener,
                           final MediaProjection projection, final int width, final int height,
                           final int density, int frameRate) {
        super(listener, projection, width, height, density, frameRate);

        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() throws IOException, MediaCodec.CodecException {
        mIsCapturing = true;

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "Capturing Display",
                mWidthScaled, mHeightScaled, mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, displayCallback, null);

        setImageReader();
    }

    @Override
    public void stopRecording() {
        Log.i(TAG, "stopRecording Called");
        mIsCapturing = false;
        mVirtualDisplay.release();
        mHandler.getLooper().quit();
        new Thread(clearAndDisconnect).start();
    }

    private Runnable clearAndDisconnect  = new Runnable() {
        public void run() {
            mListener.clear();
            mListener.disconnect();
        }
    };

    private VirtualDisplay.Callback displayCallback = new VirtualDisplay.Callback() {
        @Override
        public void onPaused() {
            super.onPaused();
            mIsCapturing = false;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
        @Override
        public void onResumed() {
            super.onResumed();
            if (!isCapturing()) {
                mIsCapturing = true;
                setImageReader();
            }
        }

        @Override
        public void onStopped() {
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
                        mListener.sendFrame(savePixels(img), mWidthScaled, mHeightScaled);
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

    private byte[] savePixels(Image image){
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

        image.close();

        return bao.toByteArray();
    }
}
