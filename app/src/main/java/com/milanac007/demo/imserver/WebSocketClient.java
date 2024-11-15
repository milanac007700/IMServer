package com.milanac007.demo.imserver;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;

import androidx.annotation.NonNull;

public class WebSocketClient {
    private static final String TAG = "WebSocketClient";

    private WebSocket mConn;
    private Context mContext;
    public WebSocketClient(Context context, WebSocket conn, ClientHandshake handshake) {
        mContext = context;
        mConn = conn;
    }

    public void onClose(int code, String reason, boolean remote) {

    }

    public void onMessage(String message) {

    }

    public void onError(Exception ex) {

    }


    public void send(int toId, String cmd) {
        appendLog("toId: " + toId  + ", send : " +  cmd);
        try {
            mConn.send(cmd);
            appendLog("send ok.");
        } catch (WebsocketNotConnectedException e) {
            appendLog("send fail WebsocketNotConnectedException: " + e.getMessage());
        }
    }


    @NonNull
    @Override
    public String toString() {
        return String.format("%s[remote = %s", TAG, mConn.getRemoteSocketAddress());
    }

    private void appendLog(String message) {
        Log.i(TAG, message);
        Intent intent = new Intent("com.milanac007.demo.imserver.client.log");
        intent.putExtra("log", message + "\n");
        mContext.sendBroadcast(intent);
    }
}
