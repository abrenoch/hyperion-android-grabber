package com.abrenoch.hyperiongrabber.screencap;

import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.FloatBuffer;

public class FullFrameRect {
    private static final boolean DEBUG = false;
    private static final String TAG = "FullFrameRect";

    public static final int FILTER_NONE = 0;
    public static final int FILTER_BLACK_WHITE = 1;
    public static final int FILTER_NIGHT = 2;
    public static final int FILTER_CHROMA_KEY = 3;
    public static final int FILTER_BLUR = 4;
    public static final int FILTER_SHARPEN = 5;
    public static final int FILTER_EDGE_DETECT = 6;
    public static final int FILTER_EMBOSS = 7;
    public static final int FILTER_SQUEEZE = 8;
    public static final int FILTER_TWIRL = 9;
    public static final int FILTER_TUNNEL = 10;
    public static final int FILTER_BULGE = 11;
    public static final int FILTER_DENT = 12;
    public static final int FILTER_FISHEYE = 13;
    public static final int FILTER_STRETCH = 14;
    public static final int FILTER_MIRROR = 15;

    public static enum SCREEN_ROTATION {LANDSCAPE, VERTICAL, UPSIDEDOWN_LANDSCAPE, UPSIDEDOWN_VERTICAL}

    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;
    private final Object mDrawLock = new Object();

    private static final int SIZEOF_FLOAT = 4;

    private final float[] mMvpMatrix = new float[16];

    private static final float TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer TEX_COORDS_BUF = GlUtil.createFloatBuffer(TEX_COORDS);
    private static final int TEX_COORDS_STRIDE = 2 * SIZEOF_FLOAT;

    private boolean mCorrectVerticalVideo = false;
    private boolean mScaleToFit;
    private SCREEN_ROTATION requestedOrientation = SCREEN_ROTATION.LANDSCAPE;

    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *                the program when no longer needed.
     */
    public FullFrameRect(final Texture2dProgram program) {
        mProgram = program;
        Matrix.setIdentityM(mMvpMatrix, 0);
    }

    /**
     * Adjust the MVP Matrix to rotate and crop the texture
     * to make vertical video appear upright
     */
    public void adjustForVerticalVideo(final SCREEN_ROTATION orientation, final boolean scaleToFit) {
        synchronized (mDrawLock) {
            mCorrectVerticalVideo = true;
            mScaleToFit = scaleToFit;
            requestedOrientation = orientation;
            Matrix.setIdentityM(mMvpMatrix, 0);
            switch (orientation) {
                case VERTICAL:
                    if (scaleToFit) {
                        Matrix.rotateM(mMvpMatrix, 0, -90, 0f, 0f, 1f);
                        Matrix.scaleM(mMvpMatrix, 0, 3.16f, 1.0f, 1f);
                    } else {
                        Matrix.scaleM(mMvpMatrix, 0, 0.316f, 1f, 1f);
                    }
                    break;
                case UPSIDEDOWN_LANDSCAPE:
                    if (scaleToFit) {
                        Matrix.rotateM(mMvpMatrix, 0, -180, 0f, 0f, 1f);
                    }
                    break;
                case UPSIDEDOWN_VERTICAL:
                    if (scaleToFit) {
                        Matrix.rotateM(mMvpMatrix, 0, 90, 0f, 0f, 1f);
                        Matrix.scaleM(mMvpMatrix, 0, 3.16f, 1.0f, 1f);
                    } else {
                        Matrix.scaleM(mMvpMatrix, 0, 0.316f, 1f, 1f);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void resetMatrix() {
        Matrix.setIdentityM(mMvpMatrix, 0);
    }

    public float[] setMatrix(final float[] mvp_matrix, final int offset) {
        System.arraycopy(mvp_matrix, offset, mMvpMatrix, 0, 16);
        return mMvpMatrix;
    }

    public void setScale(final float scaleX, final float scaleY) {
        mMvpMatrix[0] = scaleX;
        mMvpMatrix[5] = scaleY;
    }

    public void flipMatrix(final boolean verticalFlip) {
        final float[] mat = new float[32];
        System.arraycopy(mMvpMatrix, 0, mat, 16, 16);
        Matrix.setIdentityM(mat, 0);
        if (verticalFlip) {
            Matrix.scaleM(mat, 0, 1f, -1f, 1f);
        } else {
            Matrix.scaleM(mat, 0, -1f, 1f, 1f);
        }
        Matrix.multiplyMM(mMvpMatrix, 0, mat, 0, mat, 16);
    }

    /**
     * Releases resources.
     */
    public void release() {
        if (mProgram != null) {
            mProgram.release();
            mProgram = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     */
    public void changeProgram(final Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    public void drawFrame(final int textureId, final float[] texMatrix) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        synchronized (mDrawLock) {
            if (mCorrectVerticalVideo && !mScaleToFit && (requestedOrientation == SCREEN_ROTATION.VERTICAL || requestedOrientation == SCREEN_ROTATION.UPSIDEDOWN_VERTICAL)) {
                Matrix.scaleM(texMatrix, 0, 0.316f, 1.0f, 1f);
            }

            mProgram.draw(mMvpMatrix, mRectDrawable.getVertexArray(), 0,
                    mRectDrawable.getVertexCount(), mRectDrawable.getCoordsPerVertex(),
                    mRectDrawable.getVertexStride(),
                    texMatrix, TEX_COORDS_BUF, textureId, TEX_COORDS_STRIDE);
        }
    }

    /**
     * Pass touch event down to the
     * texture's shader program
     *
     * @param ev
     */
    public void handleTouchEvent(final MotionEvent ev) {
        mProgram.handleTouchEvent(ev);
    }

    /**
     * Updates the filter
     * @return the int code of the new filter
     */
    public void updateFilter(final int newFilter) {
        Texture2dProgram.ProgramType programType;
        float[] kernel = null;
        float colorAdj = 0.0f;

        if (DEBUG) Log.d(TAG, "Updating filter to " + newFilter);
        switch (newFilter) {
            case FILTER_NONE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
                break;
            case FILTER_BLACK_WHITE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case FILTER_NIGHT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_NIGHT;
                break;
            case FILTER_CHROMA_KEY:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_CHROMA_KEY;
                break;
            case FILTER_SQUEEZE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_SQUEEZE;
                break;
            case FILTER_TWIRL:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_TWIRL;
                break;
            case FILTER_TUNNEL:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_TUNNEL;
                break;
            case FILTER_BULGE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BULGE;
                break;
            case FILTER_DENT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_DENT;
                break;
            case FILTER_FISHEYE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FISHEYE;
                break;
            case FILTER_STRETCH:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_STRETCH;
                break;
            case FILTER_MIRROR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_MIRROR;
                break;
            case FILTER_BLUR:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT3x3;
                kernel = new float[] {
                        1f/16f, 2f/16f, 1f/16f,
                        2f/16f, 4f/16f, 2f/16f,
                        1f/16f, 2f/16f, 1f/16f };
                break;
            case FILTER_SHARPEN:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT3x3;
                kernel = new float[] {
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f };
                break;
            case FILTER_EDGE_DETECT:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT3x3;
                kernel = new float[] {
                        -1f, -1f, -1f,
                        -1f, 8f, -1f,
                        -1f, -1f, -1f };
                break;
            case FILTER_EMBOSS:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_FILT3x3;
                kernel = new float[] {
                        2f, 0f, 0f,
                        0f, -1f, 0f,
                        0f, 0f, -1f };
                colorAdj = 0.5f;
                break;
            default:
                throw new RuntimeException("Unknown filter mode " + newFilter);
        }

        // Do we need a whole new program?  (We want to avoid doing this if we don't have
        // too -- compiling a program could be expensive.)
        if (programType != getProgram().getProgramType()) {
            changeProgram(new Texture2dProgram(programType));
        }

        // Update the filter kernel (if any).
        if (kernel != null) {
            getProgram().setKernel(kernel, colorAdj);
        }
    }

}