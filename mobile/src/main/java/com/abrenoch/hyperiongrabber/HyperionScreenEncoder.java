package com.abrenoch.hyperiongrabber;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import com.abrenoch.hyperiongrabber.screencap.EglTask;
import com.abrenoch.hyperiongrabber.screencap.FullFrameRect;
import com.abrenoch.hyperiongrabber.screencap.Texture2dProgram;
import com.abrenoch.hyperiongrabber.screencap.WindowSurface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Random;

public class HyperionScreenEncoder implements Runnable  {
    private static final String TAG = "HyperionScreenEncoder";

    private static final int TARGET_HEIGHT = 60;
    private static final int TARGET_WIDTH = 60;
    private static final int TARGET_BIT_RATE = TARGET_HEIGHT * TARGET_WIDTH * 3;
    private static int FRAME_RATE;
    private final float SCALE;


    // parameters for recording
    private static final String MIME_TYPE = "video/avc";
    private static final float BPP = 0.01f;

    protected final Object mSync = new Object();

    private int mWidth;
    private int mHeight;
    private MediaProjection mMediaProjection;
    private final int mDensity;

    protected volatile boolean mRequestPause;
    protected volatile boolean mRequestStop;
    private int mRequestDrain;


    private Surface mSurface;
    private final Handler mHandler;
    private boolean mIsCapturing = false;
    private HyperionThread.HyperionThreadListener mListener;
    private MediaCodec mMediaCodec;



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HyperionScreenEncoder(final HyperionThread.HyperionThreadListener listener,
                                 final MediaProjection projection, final int width, final int height,
                                 final int density, int framerate) {

//        super(muxer, listener, width, height);
        mListener = listener;
        mMediaProjection = projection;
        mDensity = density;
        mWidth = (int) Math.floor(width);
        mHeight = (int) Math.floor(height);

        if (mWidth % 2 != 0) mWidth--;
        if (mHeight % 2 != 0) mHeight--;

        SCALE = findScaleFactor();
        FRAME_RATE = framerate;

        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain)
                    mRequestDrain--;
            }
            if (localRequestStop) {
//                drain();
                // request stop recording
//                signalEndOfInputStream();
                // process output data again for EOS signale
//                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
//                drain();
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    //    @Override
    protected void release() {
        mHandler.getLooper().quit();
//        super.release();
    }

    //    @Override
    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void prepare() throws IOException, MediaCodec.CodecException {
        int w = mWidth;
        int h = mHeight;

        Log.w(TAG, "preparing codec: " +
                String.valueOf(w) + "x" + String.valueOf(h) + "@" + String.valueOf(FRAME_RATE));

        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        assert codecInfo != null;

        MediaCodecInfo.CodecCapabilities capabilitiesForType = codecInfo.getCapabilitiesForType(MIME_TYPE);
        Log.d(TAG, "default format = " + capabilitiesForType.getDefaultFormat().toString());

        // get video capabilities
        MediaCodecInfo.VideoCapabilities videoCapabilities = capabilitiesForType.getVideoCapabilities();
        if (videoCapabilities != null) {
            if (!videoCapabilities.isSizeSupported(w,h)) {
                Log.w(TAG, "PROBLEM WITH SUPPLIED RESOLUTION " +
                        String.valueOf(w) + "x" + String.valueOf(h));
                return;
            }

            Range<Double> rates = videoCapabilities.getSupportedFrameRatesFor(w,h);
            if (rates == null) {
                Log.w(TAG, "FAILED TO GET FRAME RATE INFO");
                return;
            } else if (!rates.contains((double) FRAME_RATE)) {
                Log.w(TAG, "FRAME RATE " + String.valueOf(FRAME_RATE) +
                        " NOT SUPPORTED FOR DIMENSIONS " + String.valueOf(w) + "x" + String.valueOf(h));
                return;
            }
        }

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, w, h);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface); // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate(FRAME_RATE));
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_RATE);


        try {

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mMediaCodec.createInputSurface();
        } catch (MediaCodec.CodecException e) {
            int code = e.getErrorCode();
            Log.w("WARN", "ERROR CODE: " + String.valueOf(code));
            Log.w("WARN", e.getDiagnosticInfo());
        }


        mMediaCodec.start();

        mIsCapturing = true;
        new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
        if (mListener != null) {
            try {
//                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType))
                        return codecInfo;
                }
            }
        }
        return null;
    }

    protected int calcBitRate(final int frameRate) {
        final int bitrate = (int)(BPP * frameRate * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    //    @Override
    void stopRecording() {
        synchronized (mSync) {
            mIsCapturing = false;
            mSync.notifyAll();
        }
//        super.stopRecording();
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

        public DrawTask(final EGLContext shared_context, final int flags) {
            super(shared_context, flags);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected void onStart() {
            mDrawer = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTexId = mDrawer.createTextureObject();
            mSourceTexture = new SurfaceTexture(mTexId);
            mSourceTexture.setDefaultBufferSize(mWidth, mHeight);
            mSourceSurface = new Surface(mSourceTexture);
            mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
            mEncoderSurface = new WindowSurface(getEglCore(), mSurface);



            /* might be on to something with this here
             *      seems to have a problem either drawing the new frames or receiving new ones
             * */
//            SurfaceTexture st = new SurfaceTexture(1651);
//            st.setDefaultBufferSize(mWidth, mHeight);
//            mEncoderSurface = new WindowSurface(getEglCore(), st);

            intervals = (long)(1000f / FRAME_RATE);

            display = mMediaProjection.createVirtualDisplay(
                    "Capturing Display",
                    mWidth, mHeight, mDensity,
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
            if (mMediaProjection != null) {
                //We don't release mMediaProjection in there.
                //mMediaProjection.stop();
                //mMediaProjection = null;
            }
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
                }
            }
        };

        public boolean frameAvailableSoon() {
//     if (DEBUG) Log.v(TAG, "frameAvailableSoon");
            synchronized (mSync) {
                if (!mIsCapturing || mRequestStop) {
                    return false;
                }
//                mRequestDrain++;

                //TODO: OR MAYBE SEND FRAME DATA HERE??

                mSync.notifyAll();
            }
            return true;
        }

        private long mLastFrame;
        private final Runnable mDrawTask = new Runnable() {
            @Override
            public void run() {
                boolean local_request_pause;
                boolean local_request_draw;
                double min_nano_time = 1e9 / FRAME_RATE;

                synchronized (mSync) {
                    local_request_pause = mRequestPause;
                    local_request_draw = requestDraw;

                    if (!requestDraw) {
                        try {
                            mSync.wait(intervals);
                            local_request_pause = mRequestPause;
                            local_request_draw = requestDraw;
                            requestDraw = false;
                        } catch (final InterruptedException e) {
                            return;
                        }
                    }
                }
                if (mIsCapturing) {
                    final int dequeueResult = mMediaCodec.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 10000);

                    if (local_request_draw) {

                        long now = System.nanoTime();
                        if (!local_request_pause && now - mLastFrame >= min_nano_time) {

                            mSourceTexture.updateTexImage();
                            mSourceTexture.getTransformMatrix(mTexMatrix);

                            mEncoderSurface.makeCurrent();

                            scaleMatrix(SCALE);
                            mDrawer.drawFrame(mTexId, mTexMatrix);

                            sendImage((int) SCALE);
//                            saveImage(SCALE);
                            
                            mLastFrame = System.nanoTime();

                            mEncoderSurface.swapBuffers();

                            makeCurrent();
                            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                            GLES20.glFlush();

                            frameAvailableSoon();
                        }
                    }

                    if (dequeueResult >= 0) {
                        mMediaCodec.releaseOutputBuffer(dequeueResult, false /* render */);
                    }

                    queueEvent(this);
                } else {
                    releaseSelf();
                }
            }
        };

        private void scaleMatrix (float scale) {
            Matrix.scaleM(mTexMatrix, 0, scale, scale, 1);
        }
    }


    private void sendImage(int scale) {
        if (mListener != null) {
            try {
                mListener.sendFrame(savePixels(scale), mWidth / scale, mHeight / scale);
            } catch (final Exception e) {
                Log.e(TAG, "sendImage exception:", e);
            }
        }
    }

    public byte[] savePixels(int scale){
        int w = mWidth / scale;
        int h = mHeight / scale;
        int b[]= new int[w*h];
        ByteArrayOutputStream bao = new ByteArrayOutputStream();

        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int r = h - 1; r >= 0; r--) {
            for (int c = 0; c < w; c++) {
                int pix = b[r * w + c];
                bao.write((pix) & 0xff); // red
                bao.write((pix >> 8) & 0xff); // green
                bao.write((pix >> 16) & 0xff); // blue
            }
        }

        return bao.toByteArray();
    }

    private void saveImage(int scale) {
        Bitmap bmp = saveBMP(scale);

        File myDir=new File("/sdcard/saved_images");
        myDir.mkdirs();
        String fname = String.valueOf(System.nanoTime()) + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            Log.d("DEBUG", "------------------------- " + fname);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap saveBMP(int scale){
        int w = mWidth / scale;
        int h = mHeight / scale;
        int b[]= new int[w*h];
        int bt[]= new int[w*h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int i = 0, k = 0; i < h; i++, k++) {
            for (int j = 0; j < w; j++) {
                int pix = b[i * w + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(h - k - 1) * w + j] = pix1;
            }
        }

        return Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
    }

    public float findScaleFactor() {
        float step = (float) 0.2;
        for (float i = 1; i < 100; i += step) {
            if ((mWidth / i) * (mHeight / i) * 3 <= TARGET_BIT_RATE) {
                return i;
            }
        }
        return 1;
    }

    public interface HyperionEncoderListener {
        public void onPrepared(HyperionScreenEncoder encoder);
        public void onStopped(HyperionScreenEncoder encoder);
        public void sendFrame(byte[] data, int width, int height);
    }
}
