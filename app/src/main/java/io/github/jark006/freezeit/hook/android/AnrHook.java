package io.github.jark006.freezeit.hook.android;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, appNotRespondingHookS,
                    Enum.Class.ProcessErrorStateRecord, Enum.Method.appNotResponding,
                    String.class, ApplicationInfo.class,
                    String.class, Enum.Class.WindowProcessController,
                    boolean.class, String.class, boolean.class);
        }

        // A11-13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, appNotRespondingHook,
                    Enum.Class.AnrHelper, Enum.Method.appNotResponding,
                    Enum.Class.ProcessRecord, String.class, ApplicationInfo.class, String.class,
                    Enum.Class.WindowProcessController, boolean.class, String.class);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, XC_MethodReplacement.DO_NOTHING,
                    Enum.Class.ProcessRecord, Enum.Method.appNotResponding,
                    String.class, ApplicationInfo.class, String.class, Enum.Class.WindowProcessController,
                    boolean.class, String.class);
        } else {
            // v2.2.18起 不再兼容 Android 9.0及以下
            XpUtils.hookMethod(TAG, lpParam.classLoader, XC_MethodReplacement.DO_NOTHING,
                    Enum.Class.AppErrors, Enum.Method.appNotResponding,
                    String.class, ApplicationInfo.class, String.class, Enum.Class.WindowProcessController,
                    boolean.class, String.class);
        }
    }

    /**
     * SDK30, Android 11/R add AnrHelper.java
     * SourceCode frameworks/base/services/core/java/com/android/server/am/AnrHelper.java
     * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/AnrHelper.java;l=73
     * param void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName,
     * ApplicationInfo aInfo, String parentShortComponentName,
     * WindowProcessController parentProcess, boolean aboveSystem, String annotation)
     */
    XC_MethodReplacement appNotRespondingReplacement = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            Object[] args = param.args;
            Object processRecord = args[0];

            if (processRecord == null) return null;

            int uid = XposedHelpers.getIntField(processRecord, Enum.Field.uid);

            // 代替 appNotResponding() 处理 系统应用和自由后台应用的ANR, 其他则不处理
            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid)) {
                Class<?> AnrRecord = XposedHelpers.findClass(Enum.Class.AnrRecord, lpParam.classLoader);
                Object anrRecord = XposedHelpers.newInstance(AnrRecord, args);
                Object anrRecords = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mAnrRecords);
                XposedHelpers.callMethod(anrRecords, Enum.Method.add, anrRecord);
                XposedHelpers.callMethod(param.thisObject, Enum.Method.startAnrConsumerIfNeeded);
            }
            return null;
        }
    };

    // A12+ ProcessErrorStateRecord
    XC_MethodHook appNotRespondingHookS = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            ApplicationInfo aInfo = (ApplicationInfo)param.args[1];

            int uid = aInfo.uid;

            if (config.thirdApp.contains(uid) && !config.whitelist.contains(uid))
                param.setResult(null);
        }
    };

    // A11+ AnrHelper
    XC_MethodHook appNotRespondingHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            Object processRecord = param.args[0];

            if (processRecord == null) return;

            int uid = XposedHelpers.getIntField(processRecord, Enum.Field.uid);

            if (config.thirdApp.contains(uid) && !config.whitelist.contains(uid))
                param.setResult(null);
        }
    };

}
