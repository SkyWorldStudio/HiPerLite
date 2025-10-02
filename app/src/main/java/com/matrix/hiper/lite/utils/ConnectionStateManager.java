package com.matrix.hiper.lite.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConnectionStateManager {
    private static final String PREFS_NAME = "ConnectionState";
    private static final String PENDING_SITE = "pending_site";

    public static void savePendingConnection(Context context, String siteName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // 添加应用包名前缀防止冲突
        prefs.edit().putString(PENDING_SITE, context.getPackageName() + ":" + siteName).apply();
    }

    public static String getPendingSite(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(PENDING_SITE, null);
        if (value != null && value.startsWith(context.getPackageName() + ":")) {
            return value.substring(value.indexOf(':') + 1);
        }
        return null;
    }



    public static void clearState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
