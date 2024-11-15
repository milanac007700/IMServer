package com.milanac007.demo.imserver;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class App extends Application {
    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.i("App", "onCreate()");
    }
}
