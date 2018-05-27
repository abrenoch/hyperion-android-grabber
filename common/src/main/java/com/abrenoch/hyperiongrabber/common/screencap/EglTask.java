package com.abrenoch.hyperiongrabber.common.screencap;

import android.opengl.EGLContext;
import android.util.Log;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class EglTask implements Runnable {
    private static final String TAG = "EglTask";

    protected static final class Request {
        int request;
        int arg1;
        Object arg2;

        public Request(final int _request, final int _arg1, final Object _arg2) {
            request = _request;
            arg1 = _arg1;
            arg2 = _arg2;
        }

        @Override
        public boolean equals(final Object o) {
            return (o instanceof Request)
                    ? (request == ((Request) o).request) && (arg1 == ((Request) o).arg1) && (arg2 == ((Request) o).arg2)
                    : super.equals(o);
        }
    }

    // minus value is reserved for internal use
    private static final int REQUEST_EGL_TASK_NON = 0;
    private static final int REQUEST_EGL_TASK_RUN = -1;
    private static final int REQUEST_EGL_TASK_START = -8;
    private static final int REQUEST_EGL_TASK_QUIT = -9;

    private final Object mSync = new Object();
    private final LinkedBlockingQueue<Request> mRequestPool = new LinkedBlockingQueue<Request>();
    private final LinkedBlockingDeque<Request> mRequestQueue = new LinkedBlockingDeque<Request>();
    private boolean mIsRunning = true;
    private EglCore mEglCore = null;
    private OffScreenSurface mEglHolder;

    public EglTask(final EGLContext shared_context, final int flags) {
        Log.i(TAG, "shared_context=" + shared_context);
        offer(REQUEST_EGL_TASK_START, flags, shared_context);
    }

    protected abstract void onStart();
    protected abstract void onStop();
    protected abstract boolean onError(Exception e);
    protected abstract boolean processRequest(int request, int arg1, Object arg2);

    @Override
    public void run() {
        Request request = null;
        try {
            request = mRequestQueue.take();
        } catch (final InterruptedException e) {
            // ignore
        }
        synchronized (mSync) {
            if ((request.arg2 == null) || (request.arg2 instanceof EGLContext))
                mEglCore = new EglCore(((EGLContext)request.arg2), request.arg1);
            mSync.notifyAll();
            if (mEglCore == null) {
                callOnError(new RuntimeException("failed to create EglCore"));
                return;
            }
        }
        mEglHolder = new OffScreenSurface(mEglCore, 1, 1);
        mEglHolder.makeCurrent();
        try {
            onStart();
        } catch (final Exception e) {
            if (callOnError(e))
                mIsRunning = false;
        }
        LOOP: while (mIsRunning) {
            try {
                request = mRequestQueue.take();
                mEglHolder.makeCurrent();
                switch (request.request) {
                    case REQUEST_EGL_TASK_NON:
                        break;
                    case REQUEST_EGL_TASK_RUN:
                        if (request.arg2 instanceof Runnable)
                            try {
                                ((Runnable)request.arg2).run();
                            } catch (final Exception e) {
                                if (callOnError(e))
                                    break LOOP;
                            }
                        break;
                    case REQUEST_EGL_TASK_QUIT:
                        break LOOP;
                    default:
                        boolean result = false;
                        try {
                            result = processRequest(request.request, request.arg1, request.arg2);
                        } catch (final Exception e) {
                            if (callOnError(e))
                                break LOOP;
                        }
                        if (result)
                            break LOOP;
                        break;
                }
                request.request = REQUEST_EGL_TASK_NON;
                mRequestPool.offer(request);
            } catch (final InterruptedException e) {
                break;
            }
        }
        mEglHolder.makeCurrent();
        try {
            onStop();
        } catch (final Exception e) {
            callOnError(e);
        }
        mEglHolder.release();
        mEglCore.release();
        synchronized (mSync) {
            mIsRunning = false;
            mSync.notifyAll();
        }
    }

    private boolean callOnError(final Exception e) {
        try {
            return onError(e);
        } catch (final Exception e2) {
            Log.e(TAG, "exception occurred in callOnError", e);
        }
        return true;
    }

    protected Request obtain(final int request, final int arg1, final Object arg2) {
        Request req = mRequestPool.poll();
        if (req != null) {
            req.request = request;
            req.arg1 = arg1;
            req.arg2 = arg2;
        } else {
            req = new Request(request, arg1, arg2);
        }
        return req;
    }

    /**
     * offer request to run on worker thread
     * @param request minus values and zero are reserved
     * @param arg1
     * @param arg2
     */
    public void offer(final int request, final int arg1, final Object arg2) {
        mRequestQueue.offer(obtain(request, arg1, arg2));
    }

    /**
     * offer request to run on worker thread on top of the request queue
     * @param request minus values and zero are reserved
     * @param arg1
     * @param arg2
     */
    public void offerFirst(final int request, final int arg1, final Object arg2) {
        mRequestQueue.offerFirst(obtain(request, arg1, arg2));
    }

    /**
     * request to run on worker thread
     * @param task
     */
    public void queueEvent(final Runnable task) {
        if (task != null)
            mRequestQueue.offer(obtain(REQUEST_EGL_TASK_RUN, 0, task));
    }

    public void removeRequest(final Request request) {
        for (; mRequestQueue.remove(request) ;) {};
    }

    public EglCore getEglCore() {
        return mEglCore;
    }

    protected void makeCurrent() {
        mEglHolder.makeCurrent();
    }

    /**
     * request terminate worker thread and release all related resources
     */
    public void release() {
        mRequestQueue.clear();
        synchronized (mSync) {
            if (mIsRunning) {
                offerFirst(REQUEST_EGL_TASK_QUIT, 0, null);
                mIsRunning = false;
                try {
                    mSync.wait();
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public void releaseSelf() {
        mRequestQueue.clear();
        synchronized (mSync) {
            if (mIsRunning) {
                offerFirst(REQUEST_EGL_TASK_QUIT, 0, null);
                mIsRunning = false;
            }
        }
    }

}