package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
/*
 * SDK30, Android 11/R add AnrHelper.java
 * SourceCode frameworks/base/services/core/java/com/android/server/am/AnrHelper.java
 * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/AnrHelper.java;l=73
 * param void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName,
 *         ApplicationInfo aInfo, String parentShortComponentName,
 *         WindowProcessController parentProcess, boolean aboveSystem, String annotation)
 */

public class AnrHook {
    final static String TAG = "Freezeit[AnrHook]:";
    Config config;
    LoadPackageParam lpParam;

    public AnrHook(Config config, LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedHelpers.findAndHookMethod(Enum.Class.AnrHelper, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, String.class, Enum.Class.ApplicationInfo, String.class,
                        Enum.Class.WindowProcessController, boolean.class, String.class, appNotRespondingReplacement);
                log(TAG + "hook AnrHelper: Android 11+/R+ success");
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessRecord, lpParam.classLoader, Enum.Method.appNotResponding,
                        String.class, Enum.Class.ApplicationInfo, String.class, Enum.Class.WindowProcessController,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log(TAG + "hook ProcessRecord: Android 10/Q success");
            } else {
                // TODO v2.2.18起 不再兼容 Android 9.0及以下
                XposedHelpers.findAndHookMethod(Enum.Class.AppErrors, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, Enum.Class.ActivityRecord, Enum.Class.ActivityRecord,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log(TAG + "hook AppErrors: Android 7.0-9/N-P success");
            }
        } catch (Exception e) {
            log(TAG + "hook [ AnrHelper/ProcessRecord/AppErrors ] fail:" + e);
        }
    }

    XC_MethodReplacement appNotRespondingReplacement = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            Object[] args = param.args;
            Object processRecord = args[0];

            if (processRecord == null)
                return null;

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

}
