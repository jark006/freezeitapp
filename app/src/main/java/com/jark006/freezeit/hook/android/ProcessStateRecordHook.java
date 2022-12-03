package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ProcessStateRecordHook {
    final static String TAG = "Freezeit[ProcessStateRecord]:";
    Config config;
    LocalSocketServer serverThread = null;

    public ProcessStateRecordHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;

        try {
            // SDK31 A12_R1   https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessStateRecord.java;l=513
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessStateRecord, lpParam.classLoader,
                        Enum.Method.setCurProcState, int.class, setCurProcStateHook);
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessStateRecord, lpParam.classLoader,
                        Enum.Method.onCleanupApplicationRecordLSP, onCleanupApplicationRecordLSPHook);
            } else {
                // SDK30 A11_R48  https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/am/ProcessRecord.java;l=1178
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessRecord, lpParam.classLoader,
                        Enum.Method.setCurProcState, int.class, setCurProcStateHook);
            }
            log(TAG + "hook setCurProcState() success");

            serverThread = new LocalSocketServer();
            serverThread.start();

        } catch (Exception e) {
            log(TAG + "hook setCurProcState() fail:" + e);
        }
    }

    XC_MethodHook setCurProcStateHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {

            Object processRecord = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    param.thisObject : XposedHelpers.getObjectField(param.thisObject, "mApp");

            int uid = XposedHelpers.getIntField(processRecord, "uid");
            if (uid < 10000) return;
            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid)) return;

            String processName = (String) XposedHelpers.getObjectField(processRecord, "processName");
            if (processName.contains(":")) return; // 跳过子进程判断 //TODO BUG 子线程也有前台播放服务

            int mCurProcState = (int) param.args[0];
            if (mCurProcState <= 3 || (mCurProcState <= 6 && config.tolerant.contains(uid))) {
                config.top.add(uid);
//                log(TAG + "新增 " + uid + " " + mCurProcState);
            } else {
                config.top.remove(uid);
//                log(TAG + "移除 " + uid + " " + mCurProcState);
            }
        }
    };

    XC_MethodHook onCleanupApplicationRecordLSPHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {

            Object processRecord = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ?
                    param.thisObject : XposedHelpers.getObjectField(param.thisObject, "mApp");

            int uid = XposedHelpers.getIntField(processRecord, "uid");
            if (uid < 10000) return;

            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                return;
            config.top.remove(uid);
//            log("移除 清除 "+uid);
        }
    };


    public class LocalSocketServer extends Thread {
        private static final String CONNECT_NAME = "JARK006TOP";
        LocalServerSocket mSocketServer = null;
        byte[] topBytes = new byte[32 << 2]; // 32*4 bytes

        @Override
        public void run() {
            try {
                mSocketServer = new LocalServerSocket(CONNECT_NAME);
                while (true) {
                    LocalSocket client = mSocketServer.accept();//堵塞,等待连接
                    if (client == null) continue;

                    // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
                    int byteCnt = 4;
                    for (int uid : config.top) {
                        topBytes[byteCnt++] = (byte) uid;
                        topBytes[byteCnt++] = (byte) (uid >> 8);
                        topBytes[byteCnt++] = (byte) (uid >> 16);
                        topBytes[byteCnt++] = (byte) (uid >> 24);
                    }

                    // 头部放置长度
                    int UID_len = (byteCnt / 4) - 1;
                    topBytes[0] = (byte) UID_len;
                    topBytes[1] = (byte) (UID_len >> 8);
                    topBytes[2] = (byte) (UID_len >> 16);
                    topBytes[3] = (byte) (UID_len >> 24);

                    OutputStream os = client.getOutputStream();
                    os.write(topBytes, 0, byteCnt);
                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
