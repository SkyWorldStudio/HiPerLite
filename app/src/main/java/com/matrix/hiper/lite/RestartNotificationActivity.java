package com.matrix.hiper.lite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

public class RestartNotificationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 确保只有单个实例
        if (isTaskRoot()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.restart_required_title)
                    .setMessage(R.string.restart_required_message)
                    .setCancelable(false)
                    .show();

            // 1.5秒后重启
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finishAndRemoveTask(); // 先关闭当前Activity
                android.os.Process.killProcess(android.os.Process.myPid());
            }, 1500);
        } else {
            finish(); // 避免重复创建
        }
    }
}
