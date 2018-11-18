package com.abrenoch.hyperiongrabber.common.network;

import com.abrenoch.hyperiongrabber.common.HyperionScreenService;
import com.abrenoch.hyperiongrabber.common.HyperionProto;

import java.io.IOException;

public class HyperionThread extends Thread {
    private String HOST;
    private int PORT;
    private int PRIORITY;
    private final int FRAME_DURATION = -1;
    private boolean RECONNECT = false;
    private boolean HAS_CONNECTED = false;
    private int RECONNECT_DELAY;
    private Hyperion mHyperion;

    HyperionScreenService.HyperionThreadBroadcaster mSender;
    HyperionThreadListener mReceiver = new HyperionThreadListener() {
        @Override
        public void sendFrame(byte[] data, int width, int height) {
            HyperionProto.HyperionRequest req =
                    Hyperion.setImageRequest(data, width, height, PRIORITY, FRAME_DURATION);
            if (mHyperion != null && mHyperion.isConnected()) {
                try {
                    mHyperion.sendRequest(req);
                } catch (IOException e) {
                    mSender.onConnectionError(e.hashCode(), e.getMessage());
                    e.printStackTrace();
                    if (RECONNECT && HAS_CONNECTED) {
                        reconnectDelay(RECONNECT_DELAY);
                        try {
                            mHyperion = new Hyperion(HOST, PORT);
                        } catch (IOException i) {
                            i.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        public void clear() {
            if (mHyperion != null && mHyperion.isConnected()) {
                try {
                    mHyperion.clear(PRIORITY);
                } catch (IOException e) {
                    mSender.onConnectionError(e.hashCode(), e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void disconnect() {
            if (mHyperion != null) {
                try {
                    mHyperion.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void sendStatus(boolean isGrabbing) {
            if (mSender != null) {
                mSender.onReceiveStatus(isGrabbing);
            }
        }
    };

    public HyperionThread(HyperionScreenService.HyperionThreadBroadcaster listener, final String host,
                          final int port, final int priority, final boolean reconnect, final int delay){
        HOST = host;
        PORT = port;
        PRIORITY = priority;
        RECONNECT = reconnect;
        RECONNECT_DELAY = delay * 1000;
        mSender = listener;
    }

    public HyperionThreadListener getReceiver() {return mReceiver;}

    @Override
    public void run(){
        do {
            try {
                mHyperion = new Hyperion(HOST, PORT);
            } catch (IOException e) {
                mSender.onConnectionError(e.hashCode(), e.getMessage());
                e.printStackTrace();
                if (RECONNECT && HAS_CONNECTED) {
                    reconnectDelay(RECONNECT_DELAY);
                }
            } finally {
                if (mHyperion != null && mSender != null && mHyperion.isConnected()) {
                    HAS_CONNECTED = true;
                    mSender.onConnected();
                    break;
                }
            }
        } while (RECONNECT && HAS_CONNECTED);
    }
    
    public void reconnectDelay(long t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            RECONNECT = false;
            HAS_CONNECTED = false;
            Thread.currentThread().interrupt();
        }
    }

    public interface HyperionThreadListener {
        void sendFrame(byte[] data, int width, int height);
        void clear();
        void disconnect();
        void sendStatus(boolean isGrabbing);
    }
}
