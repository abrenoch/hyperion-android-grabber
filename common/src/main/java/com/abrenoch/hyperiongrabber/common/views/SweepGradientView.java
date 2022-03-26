package com.abrenoch.hyperiongrabber.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SweepGradient;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by nino on 26-5-18.
 */

public class SweepGradientView extends View{

    Paint paint;
    final int[] colors = {Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED};
    final float[] positions = {0f, 1/6f, 2/6f, 3/6f, 4/6f, 5/6f, 6/6f};

    SweepGradient gradient;

    Rect viewBounds = new Rect();

    Matrix gradientMatrix = new Matrix();

    float rotate = 0f;

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

        gradientMatrix.postRotate(rotate++, viewBounds.width()/2, viewBounds.height()/2);
        gradient.setLocalMatrix(gradientMatrix);

        paint.setShader(gradient);
        canvas.drawRect(viewBounds, paint);
        gradientMatrix.reset();
        invalidate();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewBounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());

        gradient = new SweepGradient(viewBounds.centerX(), viewBounds.centerY(), colors, positions);
    }
}
