package com.mihuashi.paybyfinger.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.mihuashi.paybyfinger.ui.activity.BiometricAuthActivity;

public class FingerprintService extends Service {

    public static final String ACTION_BIOMETRIC_RESULT = "com.mihuashi.paybyfinger.service.BIOMETRIC_RESULT";
    public static final String EXTRA_RESULT = "result";

    private final IBinder binder = new FingerprintBinder();

    public class FingerprintBinder extends Binder {
        public FingerprintService getService() {
            return FingerprintService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public interface AuthenticationCallback {
        void onAuthenticationSuccess();
        void onAuthenticationFailure(String error);
    }

    private AuthenticationCallback callback;

    public void startBiometricAuthentication(AuthenticationCallback callback) {
        this.callback = callback;
        Intent intent = new Intent(this, BiometricAuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Log.i("FingerprintService", "开始生物识别认证");
        startActivity(intent); // 启动透明活动
    }

    private final BroadcastReceiver biometricResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("FingerprintService", "服务已启动");
            boolean result = intent.getBooleanExtra(EXTRA_RESULT, false);
            if (callback != null) {
                if (result) {
                    callback.onAuthenticationSuccess();
                } else {
                    callback.onAuthenticationFailure("Authentication failed or was canceled");
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("FingerprintService", "服务已创建");
        registerReceiver(biometricResultReceiver, new IntentFilter(ACTION_BIOMETRIC_RESULT), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("FingerprintService", "服务已销毁");
        unregisterReceiver(biometricResultReceiver);
    }
}
