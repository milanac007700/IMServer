package com.milanac007.demo.imserver;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;

public class KeepAliveService extends Service {
    private static final String TAG = "KeepAliveService";

    private ConcurrentHashMap<String, KeepAliveAppInfo> mkeepAlives = new ConcurrentHashMap<>();
    private SharedPreferences sp;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        sp = getSharedPreferences("keepAlive", Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();
        for(String key: all.keySet()) {
            String o = (String)all.get(key);
            Gson gson = new Gson();
            KeepAliveAppBean keepAliveAppBean = gson.fromJson(o, KeepAliveAppBean.class);
            KeepAliveAppInfo keepAliveAppInfo = new KeepAliveAppInfo(this, keepAliveAppBean.pkgName, keepAliveAppBean.action, keepAliveAppBean.type, keepAliveAppBean.interval);
            mkeepAlives.put(keepAliveAppBean.pkgName, keepAliveAppInfo);
            keepAliveAppInfo.start();
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if(intent != null && intent.getExtras() != null) {
            String pkgName = intent.getStringExtra("pkgName");
            boolean isEnable = intent.getBooleanExtra("isEnable", true);
            if(isEnable) {
                String action = intent.getStringExtra("action");
                String type = intent.getStringExtra("type");
                int interval = intent.getIntExtra("interval", 5000);
                Log.i(TAG, String.format("addKeepAlive : pkgName = %s, action = %s, type = %s, interval = %d", pkgName, action, type, interval));
                addKeepAlive(this, pkgName, action, type, interval);
            }else {
                Log.i(TAG, String.format("removeKeepAlive : pkgName = %s", pkgName));
                removeKeepAlive(pkgName);
            }
        }
    }

    public void addKeepAlive(Context context, String pkgName, String action, String type, int interval) {
        if(mkeepAlives.containsKey(pkgName)) {
            KeepAliveAppInfo keepAliveAppInfo = mkeepAlives.get(pkgName);
            keepAliveAppInfo.type = type;
            keepAliveAppInfo.interval = interval;
        }else {
            KeepAliveAppInfo keepAliveAppInfo = new KeepAliveAppInfo(context, pkgName, action, type, interval);
            mkeepAlives.put(pkgName, keepAliveAppInfo);
            keepAliveAppInfo.start();

            KeepAliveAppBean keepAliveAppBean = new KeepAliveAppBean(pkgName, action, type, interval);
            Gson gson = new Gson();
            String s = gson.toJson(keepAliveAppBean);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(pkgName, s);
            editor.commit();
        }
    }

    public void removeKeepAlive(String pkgName) {
        if(mkeepAlives.containsKey(pkgName)) {
            KeepAliveAppInfo keepAliveAppInfo = mkeepAlives.get(pkgName);
            keepAliveAppInfo.stop();
            SharedPreferences.Editor editor = sp.edit();
            editor.remove(pkgName);
            editor.commit();
        }
    }

    public static class KeepAliveAppBean {
        private String pkgName;
        private String action;
        private String type; //activity、service
        private int interval = 5000;

        public KeepAliveAppBean(String pkgName, String action, String type, int interval) {
            this.pkgName = pkgName;
            this.action = action;
            this.type = type;
            this.interval = interval;
        }
    }

    public static class KeepAliveAppInfo {
        private Handler mHandler;
        private Context context;
        private String pkgName;
        private String action;
        private String type; //activity、service
        private int interval = 5000;

        public KeepAliveAppInfo(Context context, String pkgName, String action, String type, int interval) {
            this.context = context;
            this.pkgName = pkgName;
            this.action = action;
            this.type = type;
            this.interval = interval;
            mHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case 0: {
                            boolean b = checkAppRunning(context, pkgName);
                            if(!b) {
                                startNewProcess(context, pkgName, action, type);
                            }
                            mHandler.sendEmptyMessageDelayed(0, interval);
                        }break;
                    }
                    return false;
                }
            });
        }


        private void startNewProcess(Context context, String pkgName, String action, String type) {
            Intent intent = new Intent(action);
            intent.setPackage(pkgName);
            if("activity".equalsIgnoreCase(type)) {
                context.startActivity(intent);
            }else {
                ComponentName componentName = context.startService(intent);
                Log.i(TAG, "addKeepAlive startNewProcess: componentName= " + componentName);
            }
            Log.i(TAG, String.format("addKeepAlive startNewProcess : pkgName = %s, action = %s, type = %s", pkgName, action, type));
        }

        public void start() {
            mHandler.sendEmptyMessageDelayed(0, interval);
        }

        public void stop() {
            mHandler.removeMessages(0);
        }

        private static boolean checkAppRunning(Context context, String pkgName) {
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
            for(ActivityManager.RunningAppProcessInfo amProcess: runningAppProcesses) {
                if(pkgName.equalsIgnoreCase(amProcess.processName)) {
                    return true;
                }
            }
            return false;
        }
    }
}