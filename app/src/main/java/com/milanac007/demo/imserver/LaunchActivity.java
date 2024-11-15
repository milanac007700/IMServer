package com.milanac007.demo.imserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.runtime.permission.PermissionUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {
    private static final String TAG = LaunchActivity.class.getSimpleName();
    private TextView logView;
    private TextView logView2;
    private Button btn_clearlog;
    private BroadcastReceiver receiver, receiver2;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_launch);
        logView = findViewById(R.id.logView);
        logView2 = findViewById(R.id.logView2);
        btn_clearlog = findViewById(R.id.btn_clearlog);
        logView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logView2.setMovementMethod(ScrollingMovementMethod.getInstance());
        btn_clearlog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logView2.setText("");
                logView2.scrollTo(0, 0); //清空内容后需要用.scrollTo(0,0) 恢复滚轮位置
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String log = intent.getStringExtra("log");
                logView.append(log);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.milanac007.demo.imserver.server.log");
        registerReceiver(receiver, filter);


        receiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String log = intent.getStringExtra("log");
                logView2.append(log);
                //更新内容后，使用View的scrollTo(int x,int y)方法使其自动滚动到最后一行。
                int offset = logView2.getLineCount() * logView2.getLineHeight();
                if(offset > logView2.getHeight()){
                    logView2.scrollTo(0,offset- logView2.getHeight());
                }
            }
        };
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.milanac007.demo.imserver.client.log");
        registerReceiver(receiver2, filter2);

        String permission = PermissionUtils.PERMISSION_WRITE_EXTERNAL_STORAGE;
        if(PermissionUtils.lacksPermission(this, permission)) {
            PermissionUtils.requestPermission(this, PermissionUtils.CODE_PERMISSION_WRITE_EXTERNAL_STORAGE, permission, mCallback);
        }else {
            onPermissionCallback();
        }
    }

    private void onPermissionCallback() {
        Intent intent = new Intent("com.milanac007.demo.imserver.WebSocketService");
        intent.setPackage(getPackageName());
        startService(intent);
    }

    PermissionUtils.PermissionGrantCallback mCallback = new PermissionUtils.PermissionGrantCallback() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode) {
                case PermissionUtils.CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:{
                    onPermissionCallback();
                }break;
            }
        }

        @Override
        public void onPermissionDenied(int requestCode, String err) {
            Log.i(TAG, "PermissionGrantCallback onPermissionDenied: " + err);
            switch (requestCode) {
                case PermissionUtils.CODE_PERMISSION_WRITE_EXTERNAL_STORAGE:{
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("提示");
                    builder.setPositiveButton(err, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            startActivity(intent);
                            finish();
                        }
                    });
                    builder.create().show();
                }break;
            }
        }

        @Override
        public void onError(String error) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults, mCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}