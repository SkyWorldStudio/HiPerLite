package com.matrix.hiper.lite;

import android.content.Context;
import android.net.VpnService;
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
import com.matrix.hiper.lite.utils.ConnectionStateManager;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int START_HIPER_CODE = 1000;
    public static MainActivity instance;


    private ImageFilterButton addNewInstance;
    private ImageFilterButton refresh;
    private ListView instanceListView;

    private String name;

    private void handlePendingConnection() {
        String pendingSite = ConnectionStateManager.getPendingSite(this);
        if (pendingSite != null) {
            // ✅ 清除状态前先验证站点是否存在
            boolean siteExists = false;
            String[] ids = new File(getFilesDir().getAbsolutePath()).list();
            if (ids != null) {
                for (String id : ids) {
                    if (id.equals(pendingSite)) {
                        siteExists = true;
                        break;
                    }
                }
            }

            if (siteExists) {
                setName(pendingSite);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // ✅ 添加额外检查：确保没有其他VPN正在运行
                    boolean anyRunning = false;
                    for (Sites.Site site : Sites.Site.loadAll(this)) {
                        if (HiPerVpnService.isRunning(site.getName())) {
                            anyRunning = true;
                            break;
                        }
                    }

                    if (!anyRunning) {
                        Intent vpnPrepareIntent = VpnService.prepare(this);
                        if (vpnPrepareIntent != null) {
                            startActivityForResult(vpnPrepareIntent, START_HIPER_CODE);
                        } else {
                            onActivityResult(START_HIPER_CODE, Activity.RESULT_OK, null);
                        }
                    }
                    ConnectionStateManager.clearState(this);
                }, 500);
            } else {
                // 站点已被删除，清除状态
                ConnectionStateManager.clearState(this);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        // ✅ 检查待连接状态
        handlePendingConnection();

        addNewInstance = findViewById(R.id.add_new_instance);
        refresh = findViewById(R.id.refresh);
        addNewInstance.setOnClickListener(this);
        refresh.setOnClickListener(this);
        instanceListView = findViewById(R.id.instance_list);

        refreshList();
    }

    public String getName() {
        return name;
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
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private String activeServiceName = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == START_HIPER_CODE && resultCode == Activity.RESULT_OK && name != null) {
            // ✅ 连接成功后清除待连接状态
            ConnectionStateManager.clearState(this);

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
                    // 关键修复：无论是否出错，都刷新列表重置按钮状态
                    refreshList();
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
            // 添加连电保护
            addNewInstance.setEnabled(false);
            AddInstanceDialog dialog = new AddInstanceDialog(this, () -> {
                refreshList();
                // 操作完成后恢复按钮状态
                addNewInstance.post(() -> addNewInstance.setEnabled(true));
            });
            dialog.setOnDismissListener(d ->
                    addNewInstance.post(() -> addNewInstance.setEnabled(true))
            );
            dialog.show();
        }
        if (view == refresh) {
            // 添加连电保护
            refresh.setEnabled(false);
            try {
                refreshList();
            } finally {
                refresh.post(() -> refresh.setEnabled(true));
            }
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

    public static void requestRestartNotification(Context context) {
        Intent intent = new Intent(context, RestartNotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    // ✅ 修复: 将方法改为 public static (关键修复)
//    public static void showRestartNotification() {
//        MainActivity activity = instance;
//        if (activity != null && !activity.isFinishing()) {
//            activity.runOnUiThread(() -> {
//                // ✅ 移除这里的状态保存 - 状态已在SiteListAdapter中保存
//                new AlertDialog.Builder(activity)
//                        .setTitle(R.string.restart_required_title)
//                        .setMessage(R.string.restart_required_message)
//                        .setCancelable(false)
//                        .show();
//
//                // 1.5秒后自动重启
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
////                    Intent intent = activity.getPackageManager()
////                            .getLaunchIntentForPackage(activity.getPackageName());
//                    if (intent != null) {
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
//                                Intent.FLAG_ACTIVITY_NEW_TASK |
//                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        activity.startActivity(intent);
//                        android.os.Process.killProcess(android.os.Process.myPid());
//                    }
//                }, 1500);
//            });
//        }
//    }


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