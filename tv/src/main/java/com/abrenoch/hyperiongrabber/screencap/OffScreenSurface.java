package com.abrenoch.hyperiongrabber.screencap;

public class OffScreenSurface extends EglSurfaceBase {

    public OffScreenSurface(final EglCore eglBase, final int width, final int height) {
        super(eglBase);
        createOffscreenSurface(width, height);
        makeCurrent();
    }

    public void release() {
        releaseEglSurface();
    }

}