package com.matrix.hiper.lite.hiper;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.*;
import android.system.OsConstants;
import android.util.Log;

import android.widget.Toast;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import com.matrix.hiper.lite.MainActivity;
import com.matrix.hiper.lite.R;
import mobile.CIDR;


// import com.matrix.hiper.lite.utils.LogUtils;

public class HiPerVpnService extends VpnService {

    private static String TAG = "VlanLite";
    private Intent lastIntent; // 新增成员变量

    private static boolean running = false;
    private static Sites.Site site = null;
    private mobile.Bulk hiper = null;
    private static HiPerVpnService instance = null;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (instance == this) instance = null;
//    }
//    public static boolean isRunning(String name) {
//        return instance != null && instance.running &&
//                instance.site != null &&
//                name.equals(instance.site.getName());
//    }
//    public static Sites.Site getSite() {
//        return instance != null ? instance.site : null;
//    }

    public static boolean isRunning(String name) {
        return (site != null && running && Objects.equals(name, site.getName()));
    }

    public static Sites.Site getSite() {
        return site;
    }

    private static ParcelFileDescriptor vpnInterface = null;
    private NetworkCallback networkCallback = new NetworkCallback();
    private boolean didSleep = false;
    private NotificationManager notificationManager;
    private boolean isCallbackRegistered = false;

    private static HiPerCallback callback;


    public static void setHiPerCallback(HiPerCallback callback) {
        HiPerVpnService.callback = callback;
    }

    private boolean shouldRestartApp = true; // 添加实例变量


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lastIntent = intent;
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        // 检查是否需要重启应用（默认是true）
        shouldRestartApp = intent.getBooleanExtra("restart_app", true);
        // 优化停止请求处理
        if (intent.hasExtra("stop") && intent.getBooleanExtra("stop", false)) {
            stopVpn();
            return START_NOT_STICKY;
        }

//        if (intent.getExtras().getBoolean("stop")) {
//            stopVpn();
//            return Service.START_NOT_STICKY;
//        }


        if (running) {
            //TODO: can we signal failure?
            return super.onStartCommand(intent, flags, startId);
        }

        //TODO: if we fail to start, android will attempt a restart lacking all the intent data we need.
        // Link active site config in Main to avoid this
        site = Sites.Site.fromFile(getApplicationContext(), intent.getExtras().getString("name"));

        if (site.getCert() == null) {
            announceExit("Site is missing a certificate");
            //TODO: can we signal failure?
            return super.onStartCommand(intent, flags, startId);
        }

        startVpn();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // 只有在服务确实运行时才进行清理
        if (running || hiper != null || vpnInterface != null) {
            unregisterNetworkCallback();

            if (hiper != null) {
                try {
                    hiper.stop(); // 确保只清理一次
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hiper = null;
            }

            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                vpnInterface = null;
            }

            running = false;
            site = null;
        }
        super.onDestroy();
    }

    private void startVpn() {
        CIDR ipNet;

        try {
            ipNet = mobile.Mobile.parseCIDR(site.getCert().getCert().getDetails().getIps().get(0));
        } catch (Exception e) {
            announceExit(e.toString());
            return;
        }

        Builder builder;
        builder = new Builder()
                .addAddress(ipNet.getIp(), (int) ipNet.getMaskSize())
                .addRoute(ipNet.getNetwork(), (int) ipNet.getMaskSize())
                .setMtu(site.getMtu())
                .setSession(TAG)
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }

        // Add our unsafe routes
        for (Sites.UnsafeRoute unsafeRoute : site.getUnsafeRoutes()) {
            try {
                CIDR cidr = mobile.Mobile.parseCIDR(unsafeRoute.getRoute());
                builder.addRoute(cidr.getNetwork(), (int) cidr.getMaskSize());
            } catch (Exception e) {
                announceExit(e.toString());
                e.printStackTrace();
            }
        }


        vpnInterface = builder.establish();
        System.out.println(site.getConfig());
        Handler handler = new Handler();
        new Thread(() -> {
            try {
                hiper = mobile.Mobile.newBulk(site.getConfig(), site.getLogFile(), vpnInterface.getFd());
                handler.post(() -> {
                    try {
                        registerNetworkCallback();
                        hiper.start();
                        running = true;
                        sendSimple(1);
                    } catch (Exception e) {
                        // 改进错误处理：更详细的错误信息
                        String errorMsg = "Failed to start hiper: " + e.getMessage();
                        Log.e(TAG, errorMsg, e);
                        running = false;
                        handler.post(() -> {
                            try {
                                if (vpnInterface != null) {
                                    vpnInterface.close();
                                }
                            } catch (IOException ex) {
                                Log.e(TAG, "Error closing VPN interface", ex);
                            }
                            hiper = null;
                            // 传递更详细的错误信息
                            announceExit("Connection failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                            stopSelf();
                        });
                    }
                });
            } catch (Exception e) {
                // 改进错误处理：更详细的错误信息
                String errorMsg = "Got an error while initializing: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
                running = false;
                handler.post(() -> {
                    try {
                        if (vpnInterface != null) {
                            vpnInterface.close();
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, "Error closing VPN interface", ex);
                    }
                    // 传递更详细的错误信息
                    announceExit("Initialization failed: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"));
                });
                stopSelf();
            }
        }).start();
    }

    private static final String ACTION_SERVICE_STOPPED = "com.matrix.hiper.lite.SERVICE_STOPPED";


    private void stopVpn() {
        // 修复4: 检查服务是否真正处于运行状态
        if (!running) {
            Log.w(TAG, "Attempted to stop VPN but it's not running");
            stopSelf();
            return;
        }

        unregisterNetworkCallback();

        // 修复5: 保存当前状态的引用，避免并发问题
        mobile.Bulk currentHiper = hiper;
        ParcelFileDescriptor currentVpnInterface = vpnInterface;

        // 修复6: 先重置状态，再清理资源
        running = false;
        site = null;
        hiper = null;
        vpnInterface = null;

        try {
            if (currentVpnInterface != null) {
                currentVpnInterface.close();
            }
            if (currentHiper != null) {
                currentHiper.stop();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            // 仅当明确要求重启应用时才发送广播
            if (lastIntent != null && lastIntent.getBooleanExtra("send_stop_broadcast", false)) {
                Intent broadcastIntent = new Intent(ACTION_SERVICE_STOPPED);
                sendBroadcast(broadcastIntent);
            }

            running = false;
            site = null;
            announceExit(null);

            // 仅当 shouldRestartApp 为 true 时才重启
            if (shouldRestartApp && getApplicationContext() != null) {
                Toast.makeText(this, R.string.restart_required_message_ing, Toast.LENGTH_LONG).show();
                new Handler(Looper.getMainLooper()).postDelayed(() ->
                        MainActivity.requestRestartNotification(getApplicationContext()), 2500);
            }
            stopSelf();
        }
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        isCallbackRegistered = true;
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (isCallbackRegistered) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
            isCallbackRegistered = false;
        }
    }

    public boolean isShouldRestartApp() {
        return shouldRestartApp;
    }

    public class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            hiper.rebind("network change");
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            hiper.rebind("network change");
        }
    }

    private void registerSleep() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm.isDeviceIdleMode()) {
                    if (!didSleep) {
                        hiper.sleep();
                        //TODO: we may want to shut off our network change listener like we do with iOS, I haven't observed any issues with it yet though
                    }
                    didSleep = true;
                } else {
                    hiper.rebind("android wake");
                    didSleep = false;
                }
            }
        };

        registerReceiver(receiver, new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
    }

    private void sendSimple(int code) {
        if (callback != null) {
            callback.run(code);
        }
    }

    private void announceExit(String err) {
        if (callback != null) {
            callback.onExit(err);
        }
    }

}
