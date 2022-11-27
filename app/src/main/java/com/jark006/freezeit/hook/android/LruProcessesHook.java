package com.jark006.freezeit.hook.android;

import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.io.PrintWriter;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LruProcessesHook {
    final static String TAG = "Freezeit[LruProcessesHook]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

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
        this.lpParam = lpParam;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader,
                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class, String.class,
                        dumpLruLockedReplacementS);
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                XposedHelpers.findAndHookMethod(Enum.Class.ActivityManagerService, lpParam.classLoader,
                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class, String.class,
                        dumpLruLockedReplacementQ_R);
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.ActivityManagerService, lpParam.classLoader,
                        Enum.Method.dumpLruLocked, PrintWriter.class, String.class,
                        dumpLruLockedReplacementQ_R);
            }
            log("hook LruProcesses success");
        } catch (Exception e) {
            log("hook LruProcesses fail:" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    // SDK31+
    XC_MethodReplacement dumpLruLockedReplacementS = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            return DumpHandle(param, param.thisObject);
        }
    };

    // SDK29-30
    XC_MethodReplacement dumpLruLockedReplacementQ_R = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            Object mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
            return DumpHandle(param, mProcessList);
        }
    };

    boolean DumpHandle(XC_MethodHook.MethodHookParam param, Object mProcessList) {

        config.top.clear();
        PrintWriter pw = (PrintWriter) param.args[0];

        // ArrayList<ProcessRecord> mLruProcesses;
        ArrayList<?> mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
        int mLruProcessActivityStart = XposedHelpers.getIntField(mProcessList, Enum.Field.mLruProcessActivityStart);

        pw.println("JARK006_LRU"); // Hook标识
        for (int i = mLruProcesses.size() - 1; i >= mLruProcessActivityStart; i--) {

            Object processRecord = mLruProcesses.get(i);
            int uid = XposedHelpers.getIntField(processRecord, "uid");

            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                continue;

            Object mState = XposedHelpers.getObjectField(processRecord, "mState");
            int mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");

            pw.print(uid);
            pw.print(' ');
            pw.println(mCurProcState);

            // 在顶层 或者 有前台服务的宽松前台
            if (mCurProcState == 2 || (mCurProcState <= 6 && config.tolerant.contains(uid)))
                config.top.add(uid);
        }
        return true;
    }
}
