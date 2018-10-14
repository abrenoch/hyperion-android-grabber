package com.abrenoch.hyperiongrabber.common.network;

import com.abrenoch.hyperiongrabber.common.HyperionProto.ClearRequest;
import com.abrenoch.hyperiongrabber.common.HyperionProto.ColorRequest;
import com.abrenoch.hyperiongrabber.common.HyperionProto.HyperionReply;
import com.abrenoch.hyperiongrabber.common.HyperionProto.HyperionRequest;
import com.abrenoch.hyperiongrabber.common.HyperionProto.ImageRequest;
import com.google.protobuf.ByteString;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Hyperion {
    private final int TIMEOUT = 1000;
    private final Socket mSocket;


    public Hyperion(String address, int port) throws IOException {
        mSocket = new Socket();
        mSocket.connect(new InetSocketAddress(address, port), TIMEOUT);
    }


    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }


    public boolean isConnected() {
        //noinspection ConstantConditions for some reason mSocket CAN be null sometimes
        return mSocket != null && mSocket.isConnected();
    }

    public void disconnect() throws IOException {
        if (isConnected()) {
            mSocket.close();
        }
    }

    public void clear(int priority) throws IOException {
        sendRequest(clearRequest(priority));
    }
    public static HyperionRequest clearRequest(int priority) {
        ClearRequest clearRequest = ClearRequest.newBuilder()
                .setPriority(priority)
                .build();

        return HyperionRequest.newBuilder()
                .setCommand(HyperionRequest.Command.CLEAR)
                .setExtension(ClearRequest.clearRequest, clearRequest)
                .build();
    }


    public void clearAll() throws IOException {
        sendRequest(clearAllRequest());
    }
    public static HyperionRequest clearAllRequest() {
        return HyperionRequest.newBuilder()
                .setCommand(HyperionRequest.Command.CLEARALL)
                .build();
    }


    public void setColor(int color, int priority) throws IOException {
        setColor(color, priority, -1);
    }
    public void setColor(int color, int priority, int duration_ms) throws IOException {
        sendRequest(setColorRequest(color, priority, duration_ms));
    }
    public static HyperionRequest setColorRequest(int color, int priority, int duration_ms) {
        ColorRequest colorRequest = ColorRequest.newBuilder()
                .setRgbColor(color)
                .setPriority(priority)
                .setDuration(duration_ms)
                .build();

        return HyperionRequest.newBuilder()
                .setCommand(HyperionRequest.Command.COLOR)
                .setExtension(ColorRequest.colorRequest, colorRequest)
                .build();
    }


    public void setImage(byte[] data, int width, int height, int priority) throws IOException {
        setImage(data, width, height, priority, -1);
    }
    public void setImage(byte[] data, int width, int height, int priority, int duration_ms) throws IOException {
        sendRequest(setImageRequest(data, width, height, priority, duration_ms));
    }
    public static HyperionRequest setImageRequest(byte[] data, int width, int height, int priority, int duration_ms) {
        ImageRequest imageRequest = ImageRequest.newBuilder()
                .setImagedata(ByteString.copyFrom(data))
                .setImagewidth(width)
                .setImageheight(height)
                .setPriority(priority)
                .setDuration(duration_ms)
                .build();

        return HyperionRequest.newBuilder()
                .setCommand(HyperionRequest.Command.IMAGE)
                .setExtension(ImageRequest.imageRequest, imageRequest)
                .build();
    }


    public void sendRequest(HyperionRequest request) throws IOException {
        if (isConnected()) {
            int size = request.getSerializedSize();

            // create the header
            byte[] header = new byte[4];
            header[0] = (byte)((size >> 24) & 0xFF);
            header[1] = (byte)((size >> 16) & 0xFF);
            header[2] = (byte)((size >>  8) & 0xFF);
            header[3] = (byte)((size  ) & 0xFF);

            // write the data to the socket
            OutputStream output = mSocket.getOutputStream();
            output.write(header);
            request.writeTo(output);
            output.flush();

            HyperionReply reply = receiveReply();
            if (reply != null && !reply.getSuccess()) {
                // response had a problem?
            }
        }
    }

    private HyperionReply receiveReply() throws IOException {
        BufferedInputStream input = new BufferedInputStream(mSocket.getInputStream());

        byte[] header = new byte[4];
        input.read(header, 0, 4);
        int size = (header[0]<<24) | (header[1]<<16) | (header[2]<<8) | (header[3]);

        HyperionReply reply = null;
        if (size > 0 && size == input.available()) {
            byte[] data = new byte[size];
            input.read(data, 0, size);
            reply = HyperionReply.parseFrom(data);
        }

        return reply;
    }
}