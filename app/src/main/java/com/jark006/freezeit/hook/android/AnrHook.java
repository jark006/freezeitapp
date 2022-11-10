package com.jark006.freezeit.hook.android;

import static com.jark006.freezeit.hook.Enum.Method.add;

import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/*
 * SDK30, Android 11/R add AnrHelper.java
 * SourceCode frameworks/base/services/core/java/com/android/server/am/AnrHelper.java
 * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/AnrHelper.java;l=73
 * param void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName,
 *         ApplicationInfo aInfo, String parentShortComponentName,
 *         WindowProcessController parentProcess, boolean aboveSystem, String annotation)
 */

public class AnrHook {
    final static String TAG = "Freezeit[AnrHandle]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

    public AnrHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedHelpers.findAndHookMethod(Enum.Class.AnrHelper, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, String.class, Enum.Class.ApplicationInfo, String.class,
                        Enum.Class.WindowProcessController, boolean.class, String.class, appNotRespondingReplacement);
                log("hook AnrHelper: Android 11+/R+ success");
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessRecord, lpParam.classLoader, Enum.Method.appNotResponding,
                        String.class, Enum.Class.ApplicationInfo, String.class, Enum.Class.WindowProcessController,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log("hook ProcessRecord: Android 10/Q success");
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.AppErrors, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, Enum.Class.ActivityRecord, Enum.Class.ActivityRecord,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log("hook AppErrors: Android 7.0-9/N-P success");
            }
        } catch (Exception e) {
            log("hook [ AnrHelper/ProcessRecord/AppErrors ] fail:" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    XC_MethodReplacement appNotRespondingReplacement = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            Object[] args = param.args;
            Object processRecord = args[0];

            if (processRecord == null) return null;

            int uid = (int) XposedHelpers.getObjectField(processRecord, Enum.Field.uid);
            if (!config.thirdApp.contains(uid)) {
                Class<?> AnrRecord = XposedHelpers.findClass(Enum.Class.AnrRecord, lpParam.classLoader);
                Object anrRecord = XposedHelpers.newInstance(AnrRecord, args);
                Object anrRecords = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mAnrRecords);
                XposedHelpers.callMethod(anrRecords, add, anrRecord);
                XposedHelpers.callMethod(param.thisObject, Enum.Method.startAnrConsumerIfNeeded);
            }
            return null;
        }
    };

}
