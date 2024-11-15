package com.milanac007.demo.imserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.milanac007.demo.fileserver.HttpService;
import com.milanac007.demo.util.Utils;

import org.nanohttpd.protocols.http.NanoHTTPD;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.RequiresApi;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    public static final String CHANNEL_ID_STRING = "IMWebSocketService";
    private WebSocketServerImpl mWebsocketServer;
    private HttpService fileServer;
//    private SimpleWebServer simpleWebServer;
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.i(TAG, "onCreate()");
        startForegroundService();
        startWebsocketServer();
        testNIOServer();
        startFileServer();
        Intent intent = new Intent(this, KeepAliveService.class);
        startService(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForegroundService();
    }

    //启动WebSocket服务
    public void startWebsocketServer() {
        Log.i(TAG, "startWebsocketServer()");
        //192.168.1.101为安卓服务端，需要连接wifi后，高级选项ip设置为静态，输入自定义地址
        //方便客户端找服务端，不需要用getHostddress等，可能连接不上

//        InetSocketAddress myHost = new InetSocketAddress("127.0.0.1", 9999);
        InetSocketAddress myHost = new InetSocketAddress("0.0.0.0", 8081);
        mWebsocketServer = new WebSocketServerImpl(this, myHost);
        mWebsocketServer.setCallback(new WebSocketServerImpl.Callback() {
            @Override
            public void onStart() {
               String log = String.format("消息服务启动完成, 地址: %s:%d\n", Utils.getLocalIpAddress(mContext), 8081);
                refreshUI(log);
            }

            @Override
            public void onError(String error) {
                String log = String.format("消息服务启动失败: %s\n", error);
                refreshUI(log);
            }
        });
        Log.i(TAG, "startWebsocketServer: " + mWebsocketServer);
        mWebsocketServer.start();
    }

    public void startFileServer() {
        String log = "";
        try {
            if(fileServer == null || !fileServer.isAlive()) {
                String rootDir = "";
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //TODO Android10 作用域存储
                        rootDir = getExternalFilesDir(null).getAbsolutePath();
                    } else {
                        rootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                    }
                }

                File rootFile = new File(rootDir + "/IMServer/files");
                if(!rootFile.exists()) {
                    boolean res = rootFile.mkdirs();
                    if(!res) {
                        log += String.format("文件服务启动失败: %s\n", "mkdirs()执行失败");
                        return;
                    }
                }

                System.out.println("KeyStore.getDefaultType(): " + KeyStore.getDefaultType());
                fileServer = new HttpService(mContext, "0.0.0.0", 8080, rootFile);
//                enableClientCertCheck();
//                disableClientCertCheck();

            }
            fileServer.start();
            log += String.format("文件服务启动完成, 地址: %s:%d\n", Utils.getLocalIpAddress(mContext), 8080);

        } catch (IOException e) {
            e.printStackTrace();
            log += String.format("文件服务启动失败: %s\n", e.getMessage());
        }finally {
            refreshUI(log);
        }

//        try {
//            if(simpleWebServer == null || !simpleWebServer.isAlive()) {
//                File rootFile = new File(Environment.getExternalStorageDirectory() + File.separator + "IMServer" + File.separator  + "files");
//                if(!rootFile.exists()) {
//                    boolean res = rootFile.mkdirs();
//                    if(!res) {
//                        log += String.format("文件服务启动失败: %s\n", "mkdirs()执行失败");
//                        return;
//                    }
//                }
//                simpleWebServer = new SimpleWebServer("0.0.0.0", 8080, rootFile, false);
//            }
//            simpleWebServer.start();
//            log += String.format("文件服务启动完成, 地址: %s:%s\n", Utils.getLocalIpAddress(mContext), 8080);
//        } catch (IOException e) {
//            e.printStackTrace();
//            log += "文件服务启动失败\n";
//        }finally {
//            refreshUI(log);
//        }
    }

    private void enableClientCertCheck() {
        try {
            InputStream keystoreStream = getResources().openRawResource(R.raw.client);
//                    InputStream keystoreStream = getResources().openRawResource(R.raw.client_sm);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(keystoreStream, "123456".toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "123456".toCharArray());
            fileServer.makeSecure(NanoHTTPD.makeSSLSocketFactory(keyStore, keyManagerFactory), null);//HTTPS
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disableClientCertCheck() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // Don't check
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // Don't check
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            fileServer.makeSecure(serverSocketFactory, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            // Handle exception
        }
    }

    private void startForegroundService() {
        Notification notification = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_STRING, "imserver", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
            notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
        }else {
            notification = new Notification.Builder(getApplicationContext()).build();
        }
        startForeground(1, notification);
    }

    private void stopForegroundService() {
        stopForeground(true);
    }


    private void testNIOServer(){

        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                Selector selector = null;
                try {
                    ServerSocketChannel listenChannel = ServerSocketChannel.open();
                    ServerSocket listen = listenChannel.socket();
                    listen.bind(new InetSocketAddress(10001));
                    listen.setReuseAddress(true);
                    listenChannel.configureBlocking(false);
                    listen.setSoTimeout(0); //无超时限制
                    boolean isRunning = true;

                    selector = Selector.open();
                    listenChannel.register(selector, SelectionKey.OP_ACCEPT);
                    while (isRunning) {
                        int size = selector.selectNow();
                        if(size == 0) {
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> it = selectionKeys.iterator();
                        while (it.hasNext()) {
                            SelectionKey key = it.next();
                            it.remove();

                            if(key.isAcceptable()) {
                                SocketChannel localSocketChannel = ((ServerSocketChannel)key.channel()).accept();
                                localSocketChannel.configureBlocking(false); //非阻塞，以使用Selector
                                localSocketChannel.register(selector, SelectionKey.OP_READ);
                            }else if(key.isReadable()) {
                                zeroCopy((SocketChannel)key.channel(), localBuffer);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        selector.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        localBuffer.clear();
                    }
                }
            }
        }).start();
    }

    ByteBuffer localBuffer = ByteBuffer.allocate(100);
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected void zeroCopy(SocketChannel in, ByteBuffer buffer)  {
        try {
            int count;
            while ((count = in.read(buffer)) > 0) {
                buffer.flip();

                byte[] arr = buffer.array();
                String log = "recv from " + in.getRemoteAddress() + ": " +  Uri.decode(new String(arr,0, buffer.limit()))  + "\n";

                Log.i(TAG, log);
                refreshUI(log);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                buffer.clear();
                log = "send to " + in.getRemoteAddress()  + ": " + "welocome!" + "\n";
                buffer.put(log.getBytes());
                buffer.flip();
                in.write(buffer);

                refreshUI(log);
                buffer.clear(); //不同于Stream，缓冲区的内容一直都在，所以读完后需要清空，才能进行向里面写
            }

            if(count == -1) { //远端正常关闭了连接时，read反-1； 非正常断开连接时，会抛出IOException
                throw new IOException(in.getRemoteAddress() + "正常关闭了连接");
            }
        }catch (IOException e) {
            refreshUI(e.getMessage() + "\n");
            try {
                in.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private void refreshUI(String log) {
        Intent intent = new Intent("com.milanac007.demo.imserver.server.log");
        intent.putExtra("log", log);
        sendBroadcast(intent);
    }
}