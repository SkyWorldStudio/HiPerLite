package com.matrix.hiper.lite;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.utils.widget.ImageFilterButton;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.gson.Gson;
import com.matrix.hiper.lite.hiper.HiPerVpnService;
import com.matrix.hiper.lite.hiper.Setting;
import com.matrix.hiper.lite.hiper.Sites;
import com.matrix.hiper.lite.utils.ConnectionStateManager;
import com.matrix.hiper.lite.utils.StringUtils;

public class LogActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private Sites.Site site;
    private Setting setting;

    private String logPath;

    private TextView title;
    private SwitchCompat switchAutoUpdate;
    private ProgressBar progressBar;
    private ImageFilterButton refresh;
    private ImageFilterButton copy;
    private ProgressBar copyProgress;
    private AppCompatTextView log;

    private Sites.IncomingSite incomingSite;
    private Spinner logLevelSpinner;
    private boolean isRestarting = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        site = Sites.Site.fromFile(getApplicationContext(), getIntent().getExtras().getString("name"));
        logPath = site.getLogFile();

        // 加载IncomingSite配置
        String name = getIntent().getExtras().getString("name");
        String configPath = getFilesDir().getAbsolutePath() + "/" + name + "/hiper_config.json";
        String configJson = StringUtils.getStringFromFile(configPath);
        if (configJson != null) {
            incomingSite = new Gson().fromJson(configJson, Sites.IncomingSite.class);
        } else {
            incomingSite = new Sites.IncomingSite();
        }

        setting = Setting.getSetting(this, site.getName());

        title = findViewById(R.id.title);
        title.setText(site.getName());

        switchAutoUpdate = findViewById(R.id.auto_update);
        switchAutoUpdate.setChecked(setting.isAutoUpdate());
        switchAutoUpdate.setOnCheckedChangeListener(this);

        copy = findViewById(R.id.copy);
        refresh = findViewById(R.id.refresh);
        copy.setOnClickListener(this);
        refresh.setOnClickListener(this);

        progressBar = findViewById(R.id.progress);

        log = findViewById(R.id.log);
        refreshLog();

        copy = findViewById(R.id.copy);
        copyProgress = findViewById(R.id.copy_progress); // 绑定进度条

        // 初始化状态
        copyProgress.setVisibility(View.GONE);
        copy.setOnClickListener(this);

        isRestarting = false;

        logLevelSpinner = findViewById(R.id.log_level_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.log_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        logLevelSpinner.setAdapter(adapter);

        // 先移除监听器，设置值后再添加监听器
        logLevelSpinner.setOnItemSelectedListener(null);

        // 设置当前日志级别
        String currentLogLevel = incomingSite.getLoggingLevel();
        int position = adapter.getPosition(currentLogLevel);
        if (position >= 0) {
            logLevelSpinner.setSelection(position);
        }

        // 如果选择的日志级别与当前配置相同，则不触发更新
        logLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLevel = parent.getItemAtPosition(position).toString();
                // 如果选择的日志级别与当前配置相同，则跳过
                if (selectedLevel.equals(incomingSite.getLoggingLevel())) {
                    return;
                }
                updateLogLevel(selectedLevel);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }


    private BroadcastReceiver stopReceiver = null;
    private void updateLogLevel(String level) {
        if (level.equals(incomingSite.getLoggingLevel())) {
            runOnUiThread(() -> {
                logLevelSpinner.setEnabled(true);
                copy.setEnabled(true);
                refresh.setEnabled(true);
                isRestarting = false;
            });
            return;
        }

        // 防止重复操作
        if (isRestarting) {
            Toast.makeText(this, "Please wait for current operation to complete", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用UI控件防止重复点击
        runOnUiThread(() -> {
            logLevelSpinner.setEnabled(false);
            copy.setEnabled(false);
            refresh.setEnabled(false);
        });
        isRestarting = true;
        String name = site.getName();

//        debug
//        Toast.makeText(this, site.getName(), Toast.LENGTH_SHORT).show();


        // 更新配置并保存
        incomingSite.setLoggingLevel(level);
        String path = getFilesDir().getAbsolutePath() + "/" + name + "/hiper_config.json";
        StringUtils.writeFile(path, new Gson().toJson(incomingSite));

        Toast.makeText(this, getString(R.string.toast_log_update) + level + getString(R.string.toast_log_update_2), Toast.LENGTH_SHORT).show();

        if (HiPerVpnService.isRunning(name)) {
            // 保存待连接状态，确保重启后能自动重连
            ConnectionStateManager.savePendingConnection(this, name);

            IntentFilter filter = new IntentFilter("com.matrix.hiper.lite.SERVICE_STOPPED");
            stopReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    unregisterReceiver(this);
                    stopReceiver = null;
                    startServiceWithDelay(name);
                }
            };
            registerReceiver(stopReceiver, filter);

            // 发送停止命令（带服务停止广播）
            Intent stopIntent = new Intent(this, HiPerVpnService.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("stop", true);
            bundle.putBoolean("restart_app", true);
            bundle.putBoolean("send_stop_broadcast", true);
            stopIntent.putExtras(bundle);
            startService(stopIntent);
        } else {
            // 6. 服务未运行时直接恢复UI
            runOnUiThread(() -> {
                logLevelSpinner.setEnabled(true);
                copy.setEnabled(true);
                refresh.setEnabled(true);
                isRestarting = false;
            });
        }
    }

    private void startServiceWithDelay(String name) {
        Toast.makeText(this, R.string.restart_required_message_ing, Toast.LENGTH_LONG).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 3. 确保Toast有足够时间显示（LENGTH_LONG约3500ms）
            MainActivity.requestRestartNotification(this);

            // 4. 立即禁用UI防止用户交互
            runOnUiThread(() -> {
                logLevelSpinner.setEnabled(false);
                copy.setEnabled(false);
                refresh.setEnabled(false);
                isRestarting = true;
            });
        }, 1500); // 1.5秒延迟 - 让Toast先显示
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (stopReceiver != null) {
            try {
                unregisterReceiver(stopReceiver);
            } catch (Exception e) {
                // 忽略
            }
            stopReceiver = null;
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

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == switchAutoUpdate) {
            setting.update(this, site.getName(), b);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == refresh) {
            refreshLog();
        }
        if (view == copy) {
            // 启用连电保护：禁用按钮 + 显示进度动画
            copy.setEnabled(false);
            copyProgress.setVisibility(View.VISIBLE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    ClipboardManager clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData data = ClipData.newPlainText(null, log.getText().toString());
                    clip.setPrimaryClip(data);
                    Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
                } finally {
                    // 操作完成后恢复按钮状态
                    copy.setEnabled(true);
                    copyProgress.setVisibility(View.GONE);
                }
            }, 100); // 短暂延迟确保UI更新
        }
    }

    private void refreshLog() {
        refresh.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        log.setVisibility(View.GONE);
        new Thread(() -> {
            String logStr = StringUtils.getStringFromFile(logPath);
            PrecomputedTextCompat preText = PrecomputedTextCompat.create(logStr == null ? "" : logStr, log.getTextMetricsParamsCompat());
            runOnUiThread(() -> {
                TextViewCompat.setPrecomputedText(log, preText);
                refresh.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                log.setVisibility(View.VISIBLE);
            });
        }).start();
    }
}
