package com.milanac007.demo.imserver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BootReceiver", "recv: " + intent.getAction());
        openApp(context);
    }

    private void openApp(Context context) {
        Intent intent = new Intent("com.milanac007.demo.imserver.WebSocketService");
        intent.setPackage(context.getPackageName());
        ComponentName componentName = context.startService(intent);
        Log.i("BootReceiver", "startService = " + componentName);
    }
}
