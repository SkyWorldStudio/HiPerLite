package com.matrix.hiper.lite;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.matrix.hiper.lite.hiper.Sites;
import com.matrix.hiper.lite.utils.NetworkUtils;

import java.io.File;
import java.io.IOException;

public class AddInstanceDialog extends Dialog implements View.OnClickListener {

    private final AddInstanceCallback callback;
    private final Handler handler;

    private EditText editName;
    private EditText editToken;
    private TextView errorText;
    private Button positive;
    private Button negative;

    public AddInstanceDialog(@NonNull Context context, AddInstanceCallback callback) {
        super(context);
        setContentView(R.layout.dialog_add_instance);
        // 修复1: 允许通过返回键关闭对话框
        setCancelable(true);
        this.callback = callback;
        this.handler = new Handler();

        editName = findViewById(R.id.edit_name);
        editToken = findViewById(R.id.edit_token);
        errorText = findViewById(R.id.error_text);
        positive = findViewById(R.id.ok);
        negative = findViewById(R.id.cancel);
        positive.setOnClickListener(this);
        negative.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // 修复2: 正确处理两个按钮的点击事件
        if (view == positive) {
            // 强化连电保护
            positive.setEnabled(false);
            negative.setEnabled(false);
            errorText.setVisibility(View.GONE);

            new Thread(() -> {
                try {
                    String name = editName.getText().toString().trim();
                    String token = editToken.getText().toString().trim();

                    if (name.isEmpty() || token.isEmpty()) {
                        handler.post(() -> {
                            Toast.makeText(getContext(), "Name and token are required", Toast.LENGTH_SHORT).show();
                            positive.setEnabled(true);
                            negative.setEnabled(true);
                        });
                        return;
                    }

                    String path = getContext().getFilesDir().getAbsolutePath() + "/" + name;
                    if (new File(path).exists()) {
                        handler.post(() -> {
                            Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_new_instance_warn), Toast.LENGTH_SHORT).show();
                            positive.setEnabled(true);
                            negative.setEnabled(true);
                        });
                    } else {
                        getConfig(name, token);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.post(() -> {
                        positive.setEnabled(true);
                        negative.setEnabled(true);
                    });
                }
            }).start();
        }
        // 修复3: 正确处理取消按钮点击
        else if (view == negative) {
            dismiss();
        }
    }

    private void getConfig(String name, String token) {
        new Thread(() -> {
            try {
                String url;
                if (token.startsWith("https") || token.startsWith("http")) {
                    url = token;
                }
                else {
                    url = String.format("https://cert.mcer.cn/%s.yml", token);
                }
                String conf = NetworkUtils.doGet(NetworkUtils.toURL(url));
                if (conf.isEmpty()) {
                    handler.post(() -> {
                        Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_new_instance_error_invalid), Toast.LENGTH_SHORT).show();
                        positive.setEnabled(true);
                        negative.setEnabled(true);
                    });
                }
                else {
                    conf = conf.replaceAll("HIPER", "VLAN");
                    conf = conf.replaceAll("\u003d", "=");
                    String syncAddition = null;
                    String syncAdditionUrl = mobile.Mobile.getConfigSetting(conf, "sync.addition");
                    if (syncAdditionUrl != null && !syncAdditionUrl.isEmpty()) {
                        syncAddition = NetworkUtils.doGet(NetworkUtils.toURL(syncAdditionUrl));
                    }
                    Sites.IncomingSite incomingSite = Sites.IncomingSite.parse(name, token, conf, syncAddition);
                    incomingSite.save(getContext());
                    handler.post(() -> {
                        callback.onInstanceAdd();
                        dismiss();  // 确保在成功时关闭对话框
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    errorText.setText(e.getMessage() != null ? e.getMessage() : e.toString());
                    errorText.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_new_instance_error_network), Toast.LENGTH_SHORT).show();
                    positive.setEnabled(true);
                    negative.setEnabled(true);
                });
            }
        }).start();
    }

    public interface AddInstanceCallback {
        void onInstanceAdd();
    }
}
