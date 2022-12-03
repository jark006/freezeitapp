package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LruProcessesHook {
    final static String TAG = "Freezeit[LruProcessesHook]:";
    Config config;

    Object mProcessList = null;
    ArrayList<?> mLruProcesses = null;

    LocalSocketServer serverThread = null;
    int lastTopUid = 0;

    // SDK33 a13_R8   https://cs.android.com/android/platform/superproject/+/android-13.0.0_r8:frameworks/base/services/core/java/com/android/server/am/ProcessList.java;l=3921
    // SDK32 A12L_R27 https://cs.android.com/android/platform/superproject/+/android-12.1.0_r27:frameworks/base/services/core/java/com/android/server/am/ProcessList.java;l=4018
    // SDK31 A12_R1   https://cs.android.com/android/platform/superproject/+/android-12.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ProcessList.java;l=3975
    // ProcessList: boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK30 A11_R48 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=11240
    // ActivityManagerService: boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK29 A10_R47 https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=10477
    // ActivityManagerService: void dumpLruLocked(PrintWriter pw, String dumpPackage)
    public LruProcessesHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;

        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader,
//                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class, String.class,
//                        dumpLruLockedReplacementS);
//            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
//                XposedHelpers.findAndHookMethod(Enum.Class.ActivityManagerService, lpParam.classLoader,
//                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class, String.class,
//                        dumpLruLockedReplacementQ_R);
//            } else {
//                XposedHelpers.findAndHookMethod(Enum.Class.ActivityManagerService, lpParam.classLoader,
//                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class,
//                        dumpLruLockedReplacementQ_R);
//            }
//            log(TAG + "hook LruProcesses() success");


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader,
                        Enum.Method.updateLruProcessLSP,
                        Enum.Class.ProcessRecord, Enum.Class.ProcessRecord, boolean.class, boolean.class,
                        updateLruHook);
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader,
                        Enum.Method.updateLruProcessLocked,
                        Enum.Class.ProcessRecord, boolean.class, Enum.Class.ProcessRecord,
                        updateLruHook);
            }
            log(TAG + "hook updateLruProcessLocked() success");

//            XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader,
//                    Enum.Method.removeLruProcessLocked, Enum.Class.ProcessRecord,
//                    updateLruHook);
//            log(TAG + "hook removeLruProcessLocked() success");

            serverThread = new LocalSocketServer();
            serverThread.start();

        } catch (Exception e) {
            log(TAG + "hook LruProcesses fail:" + e);
        }
    }
//
//    // SDK31+
//    XC_MethodReplacement dumpLruLockedReplacementS = new XC_MethodReplacement() {
//        @Override
//        protected Object replaceHookedMethod(MethodHookParam param) {
//            if (mLruProcesses == null) {
//                mProcessList = param.thisObject;
//                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
//                log(TAG + "初始化 SDK31+ mProcessList mLruProcesses");
//            }
//            return DumpHandle(param);
//        }
//    };
//
//    // SDK29-30
//    XC_MethodReplacement dumpLruLockedReplacementQ_R = new XC_MethodReplacement() {
//        @Override
//        protected Object replaceHookedMethod(MethodHookParam param) {
//            if (mLruProcesses == null) {
//                mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
//                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
//                log(TAG + "初始化 SDK29-30 mProcessList mLruProcesses");
//            }
//            return DumpHandle(param);
//        }
//    };
//
//    boolean DumpHandle(XC_MethodHook.MethodHookParam param) {
//
//        config.top.clear();
//        PrintWriter pw = (PrintWriter) param.args[0];
//
//        int mLruProcessActivityStart = XposedHelpers.getIntField(mProcessList, Enum.Field.mLruProcessActivityStart);
//
//        pw.println("JARK006_LRU"); // Hook标识
//        for (int i = mLruProcesses.size() - 1; i >= mLruProcessActivityStart; i--) {
//
//            Object processRecord = mLruProcesses.get(i);
//            int uid = XposedHelpers.getIntField(processRecord, "uid");
//
//            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid))
//                continue;
//
//            Object mState = XposedHelpers.getObjectField(processRecord, "mState");
//            int mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");
//
//
//            // 在顶层 或 绑定了顶层应用 或 有前台服务的宽松前台
//            if (mCurProcState == 2 || mCurProcState == 3 || (mCurProcState <= 6 && config.tolerant.contains(uid))) {
//                config.top.add(uid);
//
//                pw.print(uid);
//                pw.print(' ');
//                pw.println(mCurProcState);
//            }
//        }
//        return true;
//    }

    // TODO HOOK lru dump 或某个函数，输出到这里
    // TODO 改回数组 新数组
    XC_MethodHook updateLruHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) throws InterruptedException {
//            config.top[0] = 0; //清空

            if (mLruProcesses == null)
                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mLruProcesses);

//            ArrayList<?> mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mLruProcesses);

            Object processRecord = mLruProcesses.get(mLruProcesses.size() - 1);
            int uid = XposedHelpers.getIntField(processRecord, "uid");
            if (uid == lastTopUid) {
//                log(TAG+"return");
                return;
            }

            Thread.sleep(50);

            lastTopUid = uid;
            log(TAG + "更新了LRU " + (String) XposedHelpers.getObjectField(processRecord, "processName"));

            config.top.clear();

            int startIdx = XposedHelpers.getIntField(param.thisObject, Enum.Field.mLruProcessActivityStart);
            int endIdx = mLruProcesses.size();

            log(TAG + "start " + startIdx + "  " + "endIdx" + endIdx);

            for (int i = startIdx; i < endIdx; i++) {
                processRecord = mLruProcesses.get(i);
                uid = XposedHelpers.getIntField(processRecord, "uid");

                if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid)) {
//                    log(TAG+"skip "+uid);
                    continue;
                }

                Object mState = XposedHelpers.getObjectField(processRecord, "mState");
                int mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");

                // 在顶层(2) 或 绑定了顶层应用(3) 或 有前台服务的宽松前台(4-6)
                if (mCurProcState <= 3 || (mCurProcState <= 6 && config.tolerant.contains(uid))) {
//                    config.top[++config.top[0]] = uid;
                    config.top.add(uid);
                    log(TAG + "新增 " + uid + " " + mCurProcState);
                } else log(TAG + "skip2 " + uid + " " + mCurProcState);
            }
            log(TAG + "更新了LRU " + config.top.size());
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
                    LocalSocket client = mSocketServer.accept();//堵塞,等待客户端连接
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
