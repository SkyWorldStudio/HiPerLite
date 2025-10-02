package com.matrix.hiper.lite;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.utils.widget.ImageFilterButton;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;

import com.matrix.hiper.lite.hiper.Setting;
import com.matrix.hiper.lite.hiper.Sites;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        site = Sites.Site.fromFile(getApplicationContext(), getIntent().getExtras().getString("name"));
        logPath = site.getLogFile();
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
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 1111
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
