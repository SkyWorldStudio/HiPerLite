package com.matrix.hiper.lite;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.utils.widget.ImageFilterButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.matrix.hiper.lite.hiper.HiPerCallback;
import com.matrix.hiper.lite.hiper.HiPerVpnService;
import com.matrix.hiper.lite.hiper.Sites;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int START_HIPER_CODE = 1000;
    public static MainActivity instance;


    private ImageFilterButton addNewInstance;
    private ImageFilterButton refresh;
    private ListView instanceListView;

    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        addNewInstance = findViewById(R.id.add_new_instance);
        refresh = findViewById(R.id.refresh);
        addNewInstance.setOnClickListener(this);
        refresh.setOnClickListener(this);
        instanceListView = findViewById(R.id.instance_list);

        refreshList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ✅ 新增: 清理引用防止内存泄漏
        if (instance == this) {
            instance = null;
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private String activeServiceName = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_HIPER_CODE && resultCode == Activity.RESULT_OK && name != null) {
            activeServiceName = name;
            Intent intent = new Intent(this, HiPerVpnService.class);
            HiPerVpnService.setHiPerCallback(new HiPerCallback() {
                @Override
                public void run(int code) {
                    System.out.println(code == 1 ? "success" : "failed");
                    refreshList();
                }

                @Override
                public void onExit(String error) {
                    if (error != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage(error);
                        builder.setPositiveButton("OK", null);
                        builder.create().show();
                    }
                }
            });
            Bundle bundle = new Bundle();
            bundle.putString("name", name);
            intent.putExtras(bundle);
            startService(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean isServiceRunning(String siteName) {
        return activeServiceName != null && activeServiceName.equals(siteName);
    }

    @Override
    public void onClick(View view) {
        if (view == addNewInstance) {
            AddInstanceDialog dialog = new AddInstanceDialog(this, this::refreshList);
            dialog.show();
        }
        if (view == refresh) {
            refreshList();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void refreshList() {
        ArrayList<Sites.Site> list = new ArrayList<>();
        String[] ids = new File(getFilesDir().getAbsolutePath()).list();
        if (ids != null) {
            for (String id : ids) {
                String configPath = getFilesDir().getAbsolutePath() + "/" + id + "/hiper_config.json";
                if (new File(configPath).exists()) {
                    Sites.Site site = Sites.Site.fromFile(this, id);
                    // 重要：过滤掉空站点
                    if (site != null) {
                        list.add(site);
                    }
                }
            }
        }
        SiteListAdapter adapter = new SiteListAdapter(this, this, list);
        instanceListView.setAdapter(adapter);
    }


    // ✅ 修复: 将方法改为 public static (关键修复)
    public static void showRestartNotification() {
        // 使用静态实例引用
        MainActivity activity = instance;
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.restart_required_title)
                        .setMessage(R.string.restart_required_message)
                        .setCancelable(false)
                        .show();
                // 1.5秒后自动重启
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        activity.startActivity(intent);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }, 1500);
            });
        }
    }
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}