package com.abrenoch.hyperiongrabber.common.screencap;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.SurfaceHolder;

public class WindowSurface extends EglSurfaceBase {
    private Surface mSurface;

    /**
     * Associates an EGL surface with the native window surface.  The Surface will be
     * owned by WindowSurface, and released when release() is called.
     */
    public WindowSurface(final EglCore eglCore, final SurfaceHolder surface) {
        super(eglCore);
        createWindowSurface(surface.getSurface());
        mSurface = surface.getSurface();
    }

    /**
     * Associates an EGL surface with the native window surface.  The Surface will be
     * owned by WindowSurface, and released when release() is called.
     */
    public WindowSurface(final EglCore eglCore, final Surface surface) {
        super(eglCore);
        createWindowSurface(surface);
        mSurface = surface;
    }

    /**
     * Associates an EGL surface with the SurfaceTexture.
     */
    public WindowSurface(final EglCore eglCore, final SurfaceTexture surfaceTexture) {
        super(eglCore);
        createWindowSurface(surfaceTexture);
    }

    /**
     * Releases any resources associated with the Surface and the EGL surface.
     */
    public void release() {
        releaseEglSurface();
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    /**
     * Recreate the EGLSurface, using the new EglCore.  The caller should have already
     * freed the old EGLSurface with releaseEglSurface().
     * <p>
     * This is useful when we want to update the EGLSurface associated with a Surface.
     * For example, if we want to share with a different EGLContext, which can only
     * be done by tearing down and recreating the context.  (That's handled by the caller;
     * this just creates a new EGLSurface for the Surface we were handed earlier.)
     * <p>
     * If the previous EGLSurface isn't fully destroyed, e.g. it's still current on a
     * context somewhere, the create call will fail with complaints from the Surface
     * about already being connected.
     */
    public void recreate(final EglCore newEglCore) {
        if (mSurface == null) {
            throw new RuntimeException("not yet implemented for SurfaceTexture");
        }
        mEglCore = newEglCore;          // switch to new context
        createWindowSurface(mSurface);  // create new surface
    }

}