package io.github.jark006.freezeit.hook.android;

import static io.github.jark006.freezeit.hook.XpUtils.log;

import android.content.pm.ApplicationInfo;
import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class AnrHook {
    final static String TAG = "Freezeit[AnrHook]:";
    Config config;
    LoadPackageParam lpParam;

    public AnrHook(Config config, LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        // https://cs.android.com/android/platform/superproject/+/android-mainline-12.0.0_r126:frameworks/base/services/core/java/com/android/server/am/ProcessErrorStateRecord.java;l=219
        // void appNotResponding(String activityShortComponentName, ApplicationInfo aInfo,
        //            String parentShortComponentName, WindowProcessController parentProcess,
        //            boolean aboveSystem, String annotation, boolean onlyDumpSelf)
        // ProcessErrorStateRecord A12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, callbackErrorState,
                    Enum.Class.ProcessErrorStateRecord, Enum.Method.appNotResponding,
                    String.class, ApplicationInfo.class,
                    String.class, Enum.Class.WindowProcessController,
                    boolean.class, String.class, boolean.class);
        }

        // AnrHelper A11-13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, callbackAnrHelper,
                    Enum.Class.AnrHelper, Enum.Method.appNotResponding,
                    Enum.Class.ProcessRecord, String.class, ApplicationInfo.class, String.class,
                    Enum.Class.WindowProcessController, boolean.class, String.class);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, XC_MethodReplacement.DO_NOTHING,
                    Enum.Class.ProcessRecord, Enum.Method.appNotResponding,
                    String.class, ApplicationInfo.class, String.class, Enum.Class.WindowProcessController,
                    boolean.class, String.class);
        }
    }

    // A12+ ProcessErrorStateRecord
    XC_MethodHook callbackErrorState = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            ApplicationInfo aInfo = (ApplicationInfo) param.args[1];
            if (aInfo == null) {
                log(TAG, "跳过ANR ErrorState: null aInfo " + param.args[0] + " " + param.args[2]);
                return;
            }
            int uid = aInfo.uid;
            if (config.thirdApp.contains(uid) && !config.whitelist.contains(uid)) {
                log(TAG, "跳过ANR ErrorState:" + aInfo.packageName + " " + param.args[0] + " " + param.args[2]);
                param.setResult(null);
            }
        }
    };

    // A11+ AnrHelper
    XC_MethodHook callbackAnrHelper = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            Object processRecord = param.args[0];
            if (processRecord == null) return;
            int uid = XposedHelpers.getIntField(processRecord, Enum.Field.uid);
            if (config.thirdApp.contains(uid) && !config.whitelist.contains(uid)) {
                log(TAG, "跳过ANR AnrHelper:" + XposedHelpers.getObjectField(processRecord, "processName"));
                param.setResult(null);
            }
        }
    };

}
