package com.abrenoch.hyperiongrabber.common.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.abrenoch.hyperiongrabber.common.R;
import com.abrenoch.hyperiongrabber.common.util.HyperionGrabberOptions;
import com.abrenoch.hyperiongrabber.common.util.Preferences;

import java.util.Arrays;

public class AudioGradientView extends View {

    private static final boolean DEBUG = true;
    private static final String TAG = "AudioGradientView";

    public int[] colors;
    float[] positions;
    int pixelCount;

    Paint paint;
    SweepGradient gradient;
    Rect viewBounds = new Rect();

    public AudioGradientView(Context context) {
        this(context, null);
    }

    public AudioGradientView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AudioGradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Preferences prefs = new Preferences(getContext());
        int mHorizontalLEDCount = prefs.getInt(R.string.pref_key_x_led);
        int mVerticalLEDCount = prefs.getInt(R.string.pref_key_y_led);
        HyperionGrabberOptions options = new HyperionGrabberOptions(mHorizontalLEDCount, mVerticalLEDCount);
        // find the common divisor for width & height best fit for the LED count (defined in options)
        WindowManager window = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        window.getDefaultDisplay().getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        Log.d("leds/display: ", Integer.toString(mHorizontalLEDCount) + " " + Integer.toString(mVerticalLEDCount) + " " + Integer.toString(width) + " " + Integer.toString(height));
        int divisor = options.findDivisor(width, height);
        // set the scaled width & height based upon the found divisor
        int mWidthScaled = (width / divisor);
        int mHeightScaled = (height / divisor);
        pixelCount = 2*mWidthScaled + 2*mHeightScaled;
        if(DEBUG){ Log.d("pixelCount: ", Integer.toString(pixelCount)); }

        // overwrite dims with smallest even pixelnumber for debug only
//        if (DEBUG){
//            mWidthScaled = 32;
//            mHeightScaled = 18;
//            pixelCount = 2*mWidthScaled + 2*mHeightScaled;
//            positions2 = new float[pixelCount];
//        }

        colors = swirl();
//        if(!DEBUG){
//            positions = new float[pixelCount];
//            positions[0] = 0f;
//            for(int i=1; i < pixelCount; i++){
//                positions[i] = i*1.0f/LED_COUNT;
//            }
//        }
//        else
            positions = new float[pixelCount];
        double[] alpha = new double[pixelCount];
        int i;
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


        paint = new Paint();
        paint.setShader(gradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if(DEBUG){
            Log.d(TAG, "onDraw: " + Arrays.toString(colors));
        }
        paint.setShader(gradient);
        canvas.drawRect(viewBounds, paint);
        //invalidate();                         // ---> ??? call himself ??? ---> setColors invalidate()
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if(DEBUG){
            Log.d(TAG, "onMeasure: " + Arrays.toString(colors));
        }
        if(colors==null) colors = swirl();

        viewBounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        gradient = new SweepGradient(viewBounds.centerX(), viewBounds.centerY(), colors, positions);
    }

    // update Layout with new colors
    public void setColors(int[] _colors){
        if (DEBUG) Log.d(TAG,"setColors" + Arrays.toString(_colors));
        colors = _colors;
        requestLayout();    //call onMeasure
        invalidate();       //call onDraw
    }

    public int[] swirl(){
        int[] hues = new int[pixelCount];
        int[] colors = new int[pixelCount];
        float vs = 0.5f;                                      // vs == hues from OnDataCaptureListener
        for(int i = 0; i < pixelCount; i++){
            hues[i] = (int) (((float) i / pixelCount) * 360);
        }
        for(int i = 0; i < pixelCount; i++){
            float[] hsv = new float[]{(float) hues[i], 1, vs};
            int color = Color.HSVToColor(hsv);
            colors[i] = color;
        }
        return colors;

    }

    // debug
    private int[] rgbWhite() {
        int[] res = new int[pixelCount];
        for(int i = 0; i < pixelCount; i=i+3){
            res[i] = 0;
            if (i+1 < pixelCount) res[i+1] = 120;
            if (i+2 < pixelCount) res[i+2] = 240;

        }


        int[] colors = new int[pixelCount];
        float vs = 1.0f;

        for(int i = 0; i < pixelCount; i++){
            float[] hsv = new float[]{(float) res[i], 1, vs};
            int color = Color.HSVToColor(hsv);
            colors[i] = color;
        }

        return colors;
    }

}
