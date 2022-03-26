package com.abrenoch.hyperiongrabber.mobile;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;

public class FadingImageView extends androidx.appcompat.widget.AppCompatImageView {
    private Context c;

    public FadingImageView(Context c, AttributeSet attrs, int defStyle) {
        super(c, attrs, defStyle);
        this.c = c;
        init();
    }

    public FadingImageView(Context c, AttributeSet attrs) {
        super(c, attrs);
        this.c = c;
        init();
    }

    public FadingImageView(Context c) {
        super(c);
        this.c = c;
        init();
    }

    private void init() {
        this.setHorizontalFadingEdgeEnabled(true);
        this.setVerticalFadingEdgeEnabled(true);
        this.setEdgeLength(80);
    }

    public void setEdgeLength(int length) {
        this.setFadingEdgeLength(getPixels(length));
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return 1.0f;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return 1.0f;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return true;
    }

    @Override
    public boolean onSetAlpha(int alpha) {
        return false;
    }

    private int getPixels(int dipValue) {
        Resources r = c.getResources();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dipValue, r.getDisplayMetrics());
    }
}