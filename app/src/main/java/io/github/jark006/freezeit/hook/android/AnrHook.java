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

        // A10-A13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActiveServices.java;l=5731
        XpUtils.hookMethod(TAG, lpParam.classLoader, callbackServiceTimeout,
                Enum.Class.ActiveServices, Enum.Method.serviceTimeout, Enum.Class.ProcessRecord);

        // A10-A13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActiveServices.java;l=5782
        XpUtils.hookMethod(TAG, lpParam.classLoader, callbackServiceForegroundTimeout,
                Enum.Class.ActiveServices, Enum.Method.serviceForegroundTimeout, Enum.Class.ServiceRecord);

        // 忽略 ContentProvider Timeout 和 InputDispatching Timeout
    }

    // A12+ ProcessErrorStateRecord
    XC_MethodHook callbackErrorState = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            var aInfo = (ApplicationInfo) param.args[1];
            if (aInfo == null) return;
            final int uid = aInfo.uid;
            if (!config.managedApp.contains(uid) || config.whitelist.contains(uid))
                return;

            param.setResult(null);
            if (XpUtils.DEBUG_XPOSED)
                XpUtils.log(TAG, "跳过ANR ErrorState:" + aInfo.packageName + " " +
                        param.args[0] + " " + param.args[2]);
        }
    };

    // A11+ AnrHelper
    XC_MethodHook callbackAnrHelper = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            var processRecord = param.args[0];
            if (processRecord == null) return;
            final int uid = XposedHelpers.getIntField(processRecord, Enum.Field.uid);
            if (!config.managedApp.contains(uid) || config.whitelist.contains(uid))
                return;

            param.setResult(null);
            if (XpUtils.DEBUG_XPOSED)
                XpUtils.log(TAG, "跳过ANR AnrHelper:" +
                        XposedHelpers.getObjectField(processRecord, Enum.Field.processName));
        }
    };

    // A10-A13
    XC_MethodHook callbackServiceTimeout = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            var processRecord = param.args[0];
            if (processRecord == null) return;
            final int uid = XposedHelpers.getIntField(processRecord, Enum.Field.uid);
            if (!config.managedApp.contains(uid) || config.whitelist.contains(uid))
                return;

            param.setResult(null);
            if (XpUtils.DEBUG_XPOSED)
                XpUtils.log(TAG, "跳过 ServiceTimeout: " +
                        XposedHelpers.getObjectField(processRecord, Enum.Field.processName));
        }
    };

    // A10-A13
    XC_MethodHook callbackServiceForegroundTimeout = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            var serviceRecord = param.args[0];
            if (serviceRecord == null) return;
            final int uid = XposedHelpers.getIntField(serviceRecord, Enum.Field.definingUid);
            if (!config.managedApp.contains(uid) || config.whitelist.contains(uid))
                return;

            param.setResult(null);
            if (XpUtils.DEBUG_XPOSED)
                XpUtils.log(TAG, "跳过 ServiceForegroundTimeout: " +
                        XposedHelpers.getObjectField(serviceRecord, Enum.Field.packageName));
        }
    };
}
