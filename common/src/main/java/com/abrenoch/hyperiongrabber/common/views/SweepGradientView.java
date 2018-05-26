package com.abrenoch.hyperiongrabber.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by nino on 26-5-18.
 */

public class SweepGradientView extends View{

    Paint paint;
    final int[] colors = {Color.RED, Color.GREEN};
    final float[] positions = {0f, 1f};

    SweepGradient gradient;

    Rect viewBounds = new Rect();

    Matrix gradientMatrix = new Matrix();

    float rotate = 100f;


    public SweepGradientView(Context context) {
        this(context, null);
    }

    public SweepGradientView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SweepGradientView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();

        paint.setShader(gradient);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        gradientMatrix.setRotate(rotate +=1f);
        gradient.setLocalMatrix(gradientMatrix);

        paint.setShader(gradient);
        canvas.drawRect(viewBounds,paint);
        invalidate();




    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewBounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        gradient = new SweepGradient(viewBounds.centerX(), viewBounds.centerY(), colors, positions);
        gradient.setLocalMatrix(gradientMatrix);
        paint.setShader(gradient);

    }
}
