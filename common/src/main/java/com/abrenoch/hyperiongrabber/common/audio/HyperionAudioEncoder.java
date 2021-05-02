package com.abrenoch.hyperiongrabber.common.audio;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HyperionAudioEncoder extends HyperionAudioEncoderBase {

    private static final String TAG = "HyperionAudioEncoder";
    private static final boolean DEBUG = false;

    private Visualizer mAudioCapture = null;

    long eventTimeStart=0;
    long eventTimeNew=0;
    long eventTimeOld=0;
    long eventTimeBetween=0;
    boolean newTime=false;
    float turnaround=0;
    int blues = 188;
    boolean direction = true;

    int red = 0;
    int green = 120;
    int blue = 240;

    float colorRange = 360.0f;          // Farbraum
    int[] plasmaColorWidthTop;
    int[] plasmaColorHeightRight;
    int[] plasmaColorWidthBottom;
    int[] plasmaColorHeightLeft;
    long mod;

    int mScaledPixelCount;
    float[] positions;
    int turnaroundRGB = 0;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    HyperionAudioEncoder(final HyperionAudioThread.HyperionThreadListener listener, HyperionAudioService.HyperionAudioEncoderBroadcaster sender,
                         final int width, final int height, HyperionGrabberOptions options) {
        super(listener, sender, width, height, options);

        prepare();
    }

    public HyperionAudioService.HyperionAudioEncoderBroadcaster getReceiver() {return mSender;}

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepare() {
        if (DEBUG) Log.d(TAG, "Preparing encoder");

        // todo: change _METHOD to String
        VISUALIZATION_METHOD = VisualisationMethod.RGBWHITE;

        setAudioReader();

        mScaledPixelCount = 2*(mWidthScaled+mHeightScaled);
        plasmaColorWidthTop = new int[mWidthScaled];
        plasmaColorHeightRight = new int[mHeightScaled];
        plasmaColorWidthBottom = new int[mWidthScaled];
        plasmaColorHeightLeft = new int [mHeightScaled];
        prepareEffects();
        setGradientPosition();
    }

    @Override
    public void stopRecording() {
        if (DEBUG) Log.i(TAG, "stopRecording Called");
        setCapturing(false);
        mHandler.getLooper().quit();
        clearAndDisconnect();

        mAudioCapture.setDataCaptureListener(null, 0, false, false);
        mAudioCapture.setEnabled(false);
        mAudioCapture.release();
        audioListener = null;

    }

    @Override
    public void resumeRecording() {
        if (DEBUG) Log.i(TAG, "resumeRecording Called");
        // #todo:
//        if (!isCapturing() && mAudioCapture != null) {
//            if (DEBUG) Log.i(TAG, "Resuming capture audio");
//            int res = mAudioCapture.setEnabled(true);
//            if(res != Visualizer.SUCCESS){
//                Log.d(TAG, "Error starting audiocaputre: " + res);
//            }
//            setCapturing(true);
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setOrientation(int orientation) {
        if (orientation != mCurrentOrientation) {
            mCurrentOrientation = orientation;
            mIsCapturing = false;
            setAudioReader();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private void setAudioReader() {
        if (DEBUG) Log.d(TAG, "Setting audio reader  " + String.valueOf(isCapturing()));

        mAudioCapture = new Visualizer(0);    // 0: intern, 1: microphone
        mAudioCapture.setDataCaptureListener(audioListener, mSamplingRate, true, false);
        int res = mAudioCapture.setEnabled(true);
        if(res != Visualizer.SUCCESS){
            Log.d(TAG, "Error starting audiocaputre: " + res);
        }
        setCapturing(true);

        eventTimeNew = SystemClock.uptimeMillis();
    }

    private Visualizer.OnDataCaptureListener audioListener = new Visualizer.OnDataCaptureListener() {

        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {

            setFrame(waveform);
            if (DEBUG) {
                Log.d(TAG, "onWaveFormDataCapture bytes: " + Arrays.toString(waveform));
                Log.d(TAG, "onWaveFormDataCapture i: " + Integer.toString(samplingRate));
            }
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
            // #todo: fft to hues -> test
        }
    };

    private void setFrame(byte[] waveform){

        int[] colors = new int[mScaledPixelCount];
        int steps = waveform.length / mScaledPixelCount;
        float[] hues = new float[mScaledPixelCount];
        byte[] data = new byte[0];
        ByteBuffer byteBuffer;

        for(int j = 0; j < mScaledPixelCount; j++){
            int tmp_avg = 0;
            for(int k = 0; k < steps; k++){
                tmp_avg += (int) waveform[steps*j+k] & 0xFF;
            }
            float intres = tmp_avg / (float) steps;
            hues[j] = intres/(float) 255;
        }

        colors = getColorArray(hues);
        mColors = colors;
        mSender.onSendColors();

        byteBuffer = renderImageBuffer(colors);
        data = getPixels(byteBuffer, mWidthScaled, mHeightScaled, 0, 0, 0, 0);
        if (DEBUG) Log.d(TAG, "PixelArray" + Arrays.toString(data));

        byte[] finalData = data;
        new Thread(() -> mListener.sendFrame(finalData, mWidthScaled, mHeightScaled)).start();

    }

    private ByteBuffer renderImageBuffer(int[] colors){

        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        bitmap = Bitmap.createBitmap(mWidthScaled, mHeightScaled, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        SweepGradient gradient;
        Rect viewBounds = new Rect();
        viewBounds.set(0, 0, mWidthScaled, mHeightScaled);
        gradient = new SweepGradient(viewBounds.centerX(), viewBounds.centerY(), colors, positions);
        Paint paint = new Paint();
        paint.setShader(gradient);
        canvas.drawRect(viewBounds, paint);

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);

        return byteBuffer;

    }

    private byte[] getPixels(ByteBuffer byteBuffer, int width, int height, int rowStride, int pixelStride, int firstX, int firstY){

        // todo: hardcoded!!! maybe possible to get from bitmap
        rowStride = 512;    //plane.Image
        pixelStride = 4;
        firstX = 0;         // not zero with black borders
        firstY = 0;

        //
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
                bao.write(byteBuffer.get(offset) & 0xff); // R
                bao.write(byteBuffer.get(offset + 1) & 0xff); // G
                bao.write(byteBuffer.get(offset + 2) & 0xff); // B
            }
        }

        return bao.toByteArray();

    }

    private int[] getColorArray(float[] vs){
        int[] hues = new int[mScaledPixelCount];
        int[] colors = new int[mScaledPixelCount];

        if (VISUALIZATION_METHOD == VisualisationMethod.RAINBOW_SWIRL){
            hues = rainBowSwirl();
        } else if (VISUALIZATION_METHOD == VisualisationMethod.RAINBOW_MOD){
            hues = single_color();
        } else if (VISUALIZATION_METHOD == VisualisationMethod.ICEBLUE){
            hues = iceblue();
        } else if (VISUALIZATION_METHOD == VisualisationMethod.RGBWHITE){
            hues = rgbWhite();
        } else if (VISUALIZATION_METHOD == VisualisationMethod.PLASMA){
            hues = plasma();
        }
        else {
            for (int i = 0; i < mScaledPixelCount; i++){
                hues[i] = 0;
            }
        }

        // plasma effect using mod with timeBetween
        if (VISUALIZATION_METHOD != VisualisationMethod.PLASMA) {
            turnaround += mTurnAroundRate % 360;

        }

        for (int i = 0; i < mScaledPixelCount; i++){
            float[] hsv = new float[]{(float) hues[i], 1, vs[i]};
            int color = Color.HSVToColor(hsv);
            colors[i] = color;
        }

        return colors;
    }

    // #todo: create class Effects and export from encoder
    // Effects
    private int[] rainBowSwirl(){
        int[] res = new int[mScaledPixelCount];
        for(int i = 0; i < mScaledPixelCount; i++){
            res[i] = ((int) (((float) i / mScaledPixelCount) * 360) + (int) turnaround) % 360;
        }
        return res;
    }

    private int[] single_color(){
        int[] res = new int[mScaledPixelCount];
        for(int i = 0; i < mScaledPixelCount; i++){
            res[i] = (int) turnaround % 360;
        }
        return res;
    }

    private int[] iceblue(){
        // todo: adapt frequencies to color spectrum
        // hochton: helbblau bass: dunkelblau
        // 8bit pcm: jedes bit ein frequenzbereich?
        // 8 blaustufen für die töne
        int[] res = new int[mScaledPixelCount];
        for(int i = 0; i < mScaledPixelCount; i++){
            res[i] = blues;
        }
        if (blues == 225) direction = false;
        if (blues == 188) direction = true;
        if (direction) blues++;
        if (!direction) blues--;
        return res;
    }

    private int[] rgbWhite() {
        int[] res = new int[mScaledPixelCount];
        for(int i = 0; i < mScaledPixelCount; i=i+3){
            res[i] = red;
            if (i+1 < mScaledPixelCount) res[i+1] = green;
            if (i+2 < mScaledPixelCount) res[i+2] = blue;

        }
        if ( turnaroundRGB == 3 ){
            if (red==240) red = 0;
            else red = red+120;
            if (green==240) green = 0;
            else green = green+120;
            if (blue==240) blue = 0;
            else blue = blue+120;
            turnaroundRGB = 0;
        }
        turnaroundRGB++;
       return res;
    }

    private int[] plasma() {

        int[] res = new int[mScaledPixelCount];

        if(newTime){
            eventTimeNew = SystemClock.uptimeMillis();
            newTime =! newTime;
        } else {
            eventTimeOld = SystemClock.uptimeMillis();
        }
        eventTimeBetween = Math.abs(eventTimeNew-eventTimeOld);
        if (eventTimeBetween > 222) {
            mod = eventTimeStart/11;
            eventTimeStart = eventTimeStart+eventTimeBetween;
            eventTimeBetween = 0;
            newTime =! newTime;
        }

        for(int i=0; i < mScaledPixelCount; i++){

            if (i < plasmaColorWidthTop.length) {
                res[i] = (int)(plasmaColorWidthTop[i] + mod) % (int)colorRange;
            }
            else if (i >= plasmaColorWidthTop.length & i < plasmaColorWidthTop.length+plasmaColorHeightRight.length) {
                res[i] = (int)(plasmaColorHeightRight[i-plasmaColorWidthTop.length] + mod) % (int)colorRange;
            }
            else if (i >= plasmaColorWidthTop.length+plasmaColorHeightRight.length & i < plasmaColorWidthTop.length+plasmaColorHeightRight.length+plasmaColorWidthBottom.length) {
                res[i] = (int)(plasmaColorWidthBottom[i-plasmaColorWidthTop.length-plasmaColorHeightRight.length] + mod) % (int)colorRange;
            }
            else if (i >= plasmaColorWidthTop.length+plasmaColorHeightRight.length+plasmaColorWidthBottom.length) {
                res[i] = (int)(plasmaColorHeightLeft[i-plasmaColorWidthTop.length-plasmaColorHeightRight.length-plasmaColorWidthBottom.length] + mod) % (int)colorRange;
            }

        }

        return res;
    }

    private void prepareEffects() {

        int x;
        int y;

        y = 0;
        for (x = 0; x < mWidthScaled; x++) {    // LEDcountHeight = 101
            plasmaColorWidthTop[x] = colorPlasmaFrameFactor(x, y);
        }
        x = mWidthScaled-1;
        for (y = 0; y < mHeightScaled; y++) {   // LEDcountHeight = 66
            plasmaColorHeightRight[y] = colorPlasmaFrameFactor(x, y);
        }
        y = mHeightScaled-1;
        for (x = mWidthScaled-1; x >= 0; x--) {    // LEDcountHeight = 101
            plasmaColorWidthBottom[x] = colorPlasmaFrameFactor(x, y);
        }
        x = 0;
        for (y = mHeightScaled-1; y >= 0; y--) {   // LEDcountHeight = 66
            plasmaColorHeightLeft[y] = colorPlasmaFrameFactor(x, y);
        }

    }

    private int colorPlasmaFrameFactor (int x, int y) {
        return (int) (
                colorRange/2.0f + (colorRange/2.0f * Math.sin(x / 16.0f))
                        + colorRange/2.0f + (colorRange/2.0f * Math.sin(y / 8.0f))
                        + colorRange/2.0f + (colorRange/2.0f * Math.sin((x+y)) / 16.0f)
                        + colorRange/2.0f + (colorRange/2.0f * Math.sin(Math.sqrt(Math.pow(x, 2.0f) + Math.pow(y, 2.0f)) / 8.0f))
        ) / 4;
    }

    private void setGradientPosition () {

         positions = new float[mScaledPixelCount];
        double[] alpha = new double[mScaledPixelCount];
        int i;
        // # Test dimensions: 32 x 18 px
        // start from 3 o'clock to diagonal left/top corner (alpha[0-8])
        for(i=1; i<=mHeightScaled/2; i++){
            alpha[i-1] = Math.toDegrees(   Math.atan(  (double)i / ( (double)mWidthScaled/2.0f  ) )  );
        }
        if (DEBUG){Log.d("alphas", Arrays.toString(alpha));}
        int j;
        // start diagonal left/top corner to 12 o'clock (alpha[9-24])
        for(j=(mWidthScaled/2)-1; j >= 0; j--,i++){
            alpha[i-1] = 90 - Math.toDegrees(   Math.atan(  (double)j / ( (double)mHeightScaled/2.0f  ) )  );
        }
        if (DEBUG){Log.d("alphas", Arrays.toString(alpha));}
        // mirror 1.quarter(alpha[0-24]) at y-axis to 2.quarter(alpha[25-49])
        for(j=(mHeightScaled/2+mWidthScaled/2)-1; j > 0; j--,i++){
            alpha[i-1] = 90 + ( 90 - alpha[j-1]);
        }
        alpha[i-1] = alpha[i-2] + alpha[j];
        if (DEBUG){Log.d("alphas", Arrays.toString(alpha));}
        // mirror top half(alpha[0-49]) at x-axis to bottom half(alpha[50-99])
        for (j=0; j<(2*(mHeightScaled/2+mWidthScaled/2)) ; j++,i++){
            alpha[i] = 180 + alpha[j];
        }
        if (DEBUG){Log.d("alphas", Arrays.toString(alpha));}
        // norm alpha[degree] to positions[norm]
        for(i=0; i<alpha.length; i++){

//            if(DEBUG) positions2[i] = (float)alpha[i]/360.f;
//            else
            positions[i] = (float)alpha[i]/360.f;
        }
        if (DEBUG){
            Log.d("positions", Arrays.toString(positions));
            Log.d("last alpha&position", String.valueOf(alpha[alpha.length-1]) +" & "+ String.valueOf(positions[positions.length-1]));
        }

    }

}
