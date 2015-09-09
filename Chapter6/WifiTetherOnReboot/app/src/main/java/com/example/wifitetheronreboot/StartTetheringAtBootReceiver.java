package com.example.wifitetheronreboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;


public class StartTetheringAtBootReceiver extends BroadcastReceiver {
    public static void setWifiTetheringEnabled(boolean enable, Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        Method[] methods = wifiManager.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals("setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, null, enable);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            setWifiTetheringEnabled(true, context);
        }
    }
}
