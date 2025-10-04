package com.matrix.hiper.lite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.matrix.hiper.lite.utils.ConnectionStateManager;

public class RestartNotificationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isTaskRoot()) {
            ConnectionStateManager.clearState(this);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.restart_required_title)
                    .setMessage(R.string.restart_required_message)
                    .setCancelable(false)
                    .show();

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
