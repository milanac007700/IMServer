package com.milanac007.demo.imserver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.milanac007.demo.im.db.callback.Task;
import com.milanac007.demo.im.db.callback.TaskQueue;
import com.milanac007.demo.im.db.entity.UserEntity;
import com.milanac007.demo.im.db.helper.SequenceNumberMaker;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketServerImpl extends WebSocketServer {
    private static final String TAG = "WebSocketServerImpl";

    private Context context;
    private HashMap<WebSocket, WebSocketClient> clients = new HashMap<>();
    private HashMap<Integer, WebSocketClient> loginClients = new HashMap<>();
    private ConcurrentHashMap<Integer, Integer> tnMap = new ConcurrentHashMap<>();

    public WebSocketServerImpl(Context context, InetSocketAddress myHost) {
        super(myHost);
        this.context = context;
        setReuseAddr(true);
        TaskQueue.getInstance().onStart();
        initData();
    }

    private Callback mCallback = null;
    public void setCallback(Callback cb) {
        mCallback =  cb;
    }

    public interface Callback {
        void onStart();
        void onError(String error);
    }

    public HashMap<WebSocket, WebSocketClient> getClients() {
        return clients;
    }

    public HashMap<Integer, WebSocketClient> getLoginClients() {
        return loginClients;
    }

    public ConcurrentHashMap<Integer, Integer> getTnMap() {
        return tnMap;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        appendLog(String.format("onOpen getLocalSocketAddress = %s, getRemoteSocketAddress = %s", conn.getLocalSocketAddress(), conn.getRemoteSocketAddress()));
        appendLog(String.format("onOpen conn = %s, message = %s", conn.toString(), handshake.toString()));
        WebSocketClient webSocketClient = new WebSocketClient(context, conn, handshake);
        clients.put(conn, webSocketClient);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        appendLog(String.format("onClose conn = %s, code = %d, reason = %s, remote = %b", conn.toString(), code, reason, reason));
        WebSocketClient client = clients.remove(conn);
        if(client != null) {
            client.onClose(code, reason, remote);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        appendLog("recv message: " + message);
        if(!message.contains("{")) { //j简单判断，不符合json格式直接返回原报文
            conn.send(message);
            return;
        }

        try {
            TaskQueue.getInstance().push(new Task(conn, message, WebSocketServerImpl.this));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if(conn == null){
            return;
        }
        clients.get(conn).onError(ex);
        if(mCallback != null) {
            mCallback.onError(ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        appendLog("onStart()");
        if(mCallback != null) {
            mCallback.onStart();
        }
    }

    private void appendLog(String message) {
        Log.i(TAG, message);
        Intent intent = new Intent("com.milanac007.demo.imserver.client.log");
        intent.putExtra("log", message + "\n");
        context.sendBroadcast(intent);
    }

    private void initData() {
        if (UserEntity.getAllContacts() == null || UserEntity.getAllContacts().isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject1.put("mainName", "test0001");
            jsonObject1.put("created", System.currentTimeMillis());
            jsonObject1.put("userCode", "test0001");
            jsonObject1.put("nickName", "test0001");
            jsonObject1.put("gender", 0);
            jsonArray.add(jsonObject1);

            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject2.put("mainName", "test0002");
            jsonObject2.put("created", System.currentTimeMillis());
            jsonObject2.put("userCode", "test0002");
            jsonObject2.put("nickName", "test0002");
            jsonObject2.put("gender", 1);
            jsonArray.add(jsonObject2);

            JSONObject jsonObject3 = new JSONObject();
            jsonObject3.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject3.put("mainName", "test0003");
            jsonObject3.put("created", System.currentTimeMillis());
            jsonObject3.put("userCode", "test0003");
            jsonObject3.put("nickName", "test0003");
            jsonObject3.put("gender", 0);
            jsonArray.add(jsonObject3);

            JSONObject jsonObject4 = new JSONObject();
            jsonObject4.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject4.put("mainName", "A_test0004");
            jsonObject4.put("created", System.currentTimeMillis());
            jsonObject4.put("userCode", "test0004");
            jsonObject4.put("nickName", "004");
            jsonObject4.put("gender", 0);
            jsonArray.add(jsonObject4);


            JSONObject jsonObject5 = new JSONObject();
            jsonObject5.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject5.put("mainName", "# Test0005");
            jsonObject5.put("created", System.currentTimeMillis());
            jsonObject5.put("userCode", "test0005");
            jsonObject5.put("nickName", "小5");
            jsonObject5.put("gender", 1);
            jsonArray.add(jsonObject5);


            JSONObject jsonObject6 = new JSONObject();
            jsonObject6.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject6.put("mainName", "test0006");
            jsonObject6.put("created", System.currentTimeMillis());
            jsonObject6.put("userCode", "test0006");
            jsonObject6.put("nickName", "6号");
            jsonObject6.put("gender", 1);
            jsonArray.add(jsonObject6);

            JSONObject jsonObject7 = new JSONObject();
            jsonObject7.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject7.put("mainName", "007");
            jsonObject7.put("created", System.currentTimeMillis());
            jsonObject7.put("userCode", "test0007");
            jsonObject7.put("nickName", "007");
            jsonObject7.put("gender", 1);
            jsonArray.add(jsonObject7);

            JSONObject jsonObject8 = new JSONObject();
            jsonObject8.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject8.put("mainName", "test0008");
            jsonObject8.put("created", System.currentTimeMillis());
            jsonObject8.put("userCode", "test0008");
            jsonObject8.put("nickName", "8号");
            jsonObject8.put("gender", 0);
            jsonArray.add(jsonObject8);

            JSONObject jsonObject9 = new JSONObject();
            jsonObject9.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject9.put("mainName", "九号电动车");
            jsonObject9.put("created", System.currentTimeMillis());
            jsonObject9.put("userCode", "test0009");
            jsonObject9.put("nickName", "九号电动车");
            jsonObject9.put("gender", 0);
            jsonArray.add(jsonObject9);

            JSONObject jsonObject10 = new JSONObject();
            jsonObject10.put("peerId", SequenceNumberMaker.getInstance().makeUserId());
            jsonObject10.put("mainName", "十全十美");
            jsonObject10.put("created", System.currentTimeMillis());
            jsonObject10.put("userCode", "test0010");
            jsonObject10.put("nickName", "十全十美");
            jsonObject10.put("gender", 1);
            jsonArray.add(jsonObject10);

            UserEntity.insertOrUpdateMultiData(jsonArray);
        }
    }

}
