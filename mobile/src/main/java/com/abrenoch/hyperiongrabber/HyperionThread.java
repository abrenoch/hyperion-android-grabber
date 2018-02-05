package com.abrenoch.hyperiongrabber;

import com.example.android.screencapture.HyperionProto;
import java.io.IOException;

class HyperionThread extends Thread {
    private String HOST;
    private int PORT;
    private Hyperion mHyperion;

    HyperionScreenService.HyperionThreadBroadcaster mSender;
    HyperionThreadListener mReceiver = new HyperionThreadListener() {
        @Override
        public void sendFrame(byte[] data, int width, int height) {
            HyperionProto.HyperionRequest req =
                    Hyperion.setImageRequest(data, width, height, 50, -1);
            if (mHyperion != null && mHyperion.isConnected()) {
                try {
                    mHyperion.sendRequest(req);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    HyperionThread(HyperionScreenService.HyperionThreadBroadcaster listener, final String host,
                   final int port){
        HOST = host;
        PORT = port;
        mSender = listener;
    }

    public HyperionThreadListener getReceiver() {return mReceiver;}

    @Override
    public void run(){
        try {
            mHyperion = new Hyperion(HOST, PORT);
        } catch (IOException e) {
            mSender.onConnectionError(null);
            e.printStackTrace();
        } finally {
            if (mHyperion != null && mSender != null && mHyperion.isConnected()) {
                mSender.onConnected();
            }
        }
    }

    public interface HyperionThreadListener {
        void sendFrame(byte[] data, int width, int height);
    }
}