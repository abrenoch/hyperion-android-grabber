package com.abrenoch.hyperiongrabber.common;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.abrenoch.hyperiongrabber.common.network.HyperionThread;
import com.abrenoch.hyperiongrabber.common.screencap.EglTask;
import com.abrenoch.hyperiongrabber.common.screencap.FullFrameRect;
import com.abrenoch.hyperiongrabber.common.screencap.Texture2dProgram;
import com.abrenoch.hyperiongrabber.common.screencap.WindowSurface;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;

public class HyperionScreenEncoderOGL extends HyperionScreenEncoderBase implements Runnable  {
    private static final String TAG = "HyperionScreenEOGL";

    private final Object mSync = new Object();
    private volatile boolean mRequestStop;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    HyperionScreenEncoderOGL(final HyperionThread.HyperionThreadListener listener,
                          final MediaProjection projection, final int width, final int height,
                          final int density, HyperionGrabberOptions options) {
        super(listener, projection, width, height, density, options);

        try {
            prepare();
        } catch (MediaCodec.CodecException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        synchronized (mSync) {
            mRequestStop = false;
            mSync.notify();
        }
        boolean localRequestStop;
        while (true) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
            }
            if (localRequestStop) {
                release();
                break;
            }
            synchronized (mSync) {
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    break;
                }
            }
        }
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    protected void release() {
        mHandler.getLooper().quit();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() throws MediaCodec.CodecException {

        /*
        *   SCALING THIS SURFACE TEXTURE DOWN SEEMS TO CAUSE PROBLEMS?
        *
        *   will produce black frames if setDefaultBufferSize is not called
        * */
        mSurfaceTexture = new SurfaceTexture(1651);
        mSurfaceTexture.setDefaultBufferSize(mWidthScaled, mHeightScaled);
        mSurface = new Surface(mSurfaceTexture);

        mIsCapturing = true;

        new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
    }

    @Override
    public void stopRecording() {
        synchronized (mSync) {
            mIsCapturing = false;
            mSync.notifyAll();
        }
    }

    private boolean requestDraw;
    private final DrawTask mScreenCaptureTask = new DrawTask(null, 0);

    private final class DrawTask extends EglTask {
        private VirtualDisplay display;
        private long intervals;
        private int mTexId;
        private SurfaceTexture mSourceTexture;
        private Surface mSourceSurface;
        private WindowSurface mEncoderSurface;
        private FullFrameRect mDrawer;
        private final float[] mTexMatrix = new float[16];

        DrawTask(final EGLContext shared_context, final int flags) {
            super(shared_context, flags);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onStart() {
            mDrawer = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTexId = mDrawer.createTextureObject();

            mSourceTexture = new SurfaceTexture(mTexId);
            mSourceTexture.setDefaultBufferSize(mWidthScaled, mHeightScaled);
            mSourceSurface = new Surface(mSourceTexture);
            mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
            mEncoderSurface = new WindowSurface(getEglCore(), mSurface);

            intervals = (long)(1000f / mFrameRate);

            display = mMediaProjection.createVirtualDisplay(
                    "Capturing Display",
                    mWidthScaled, mHeightScaled, mDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSourceSurface, null, null);

            queueEvent(mDrawTask);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        protected void onStop() {
            if (mDrawer != null) {
                mDrawer.release();
                mDrawer = null;
            }
            if (mSourceSurface != null) {
                mSourceSurface.release();
                mSourceSurface = null;
            }
            if (mSourceTexture != null) {
                mSourceTexture.release();
                mSourceTexture = null;
            }
            if (mEncoderSurface != null) {
                mEncoderSurface.release();
                mEncoderSurface = null;
            }
            makeCurrent();
            if (display != null) {
                display.release();
            }
            mListener.clear();
            mListener.disconnect();
        }

        @Override
        protected boolean onError(final Exception e) {
            return false;
        }

        @Override
        protected boolean processRequest(final int request, final int arg1, final Object arg2) {
            return false;
        }

        private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                if (mIsCapturing) {
                    synchronized (mSync) {
                        requestDraw = true;
                        mSync.notifyAll();
                    }
                } else {
                    requestDraw = false;
                }
            }
        };

        void frameAvailableSoon() {
            synchronized (mSync) {
                if (!mIsCapturing || mRequestStop) {
                    return;
                }
                mSync.notifyAll();
            }
        }

        private long mLastFrame;
        private final Runnable mDrawTask = new Runnable() {
            @Override
            public void run() {
                boolean local_request_draw;
                double min_nano_time = 1e9 / mFrameRate;
                synchronized (mSync) {
                    local_request_draw = requestDraw;
                    if (!requestDraw) {
                        try {
                            mSync.wait(intervals);
                        } catch (final InterruptedException e) {
                            return;
                        }
                    }
                }
                if (mIsCapturing) {
                    if (local_request_draw && System.nanoTime() - mLastFrame >= min_nano_time) {
                        mSourceTexture.updateTexImage();
                        mSourceTexture.getTransformMatrix(mTexMatrix);
                        mSurfaceTexture.updateTexImage();
                        mEncoderSurface.makeCurrent();
                        mDrawer.drawFrame(mTexId, mTexMatrix);
                        sendImage();
                        mLastFrame = System.nanoTime();
                        mEncoderSurface.swapBuffers();
                        makeCurrent();
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        GLES20.glFlush();
                        frameAvailableSoon();
                    }
                    queueEvent(this);
                } else {
                    releaseSelf();
                    onStop();
                }
            }
        };
    }

    private void sendImage() {
        if (mListener != null) {
            try {
                mListener.sendFrame(savePixels(), mWidthScaled, mHeightScaled);
            } catch (final Exception e) {
                Log.e(TAG, "sendImage exception:", e);
            }
        }
    }

    private byte[] savePixels(){
        int w = mWidthScaled;
        int h = mHeightScaled;
        int b[]= new int[w*h];
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int r = h - 1; r >= 0; r--) {
            for (int c = 0; c < w; c++) {
                int pix = b[r * w + c];
                bao.write((pix) & 0xff); // R
                bao.write((pix >> 8) & 0xff); // G
                bao.write((pix >> 16) & 0xff); // B
            }
        }

        return bao.toByteArray();
    }
}