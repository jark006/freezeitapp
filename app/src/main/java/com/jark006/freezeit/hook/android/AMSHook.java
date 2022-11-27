package com.jark006.freezeit.hook.android;

import android.content.Context;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AMSHook {
    final static String TAG = "Freezeit[AMSHook]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

    // SDK30 A11_R48 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=11240
    // ActivityManagerService: boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK29 A10_R47 https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=10477
    // ActivityManagerService: void dumpLruLocked(PrintWriter pw, String dumpPackage)
    public AMSHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            XposedHelpers.findAndHookConstructor(Enum.Class.ActivityManagerService, lpParam.classLoader,
                    Context.class, Enum.Class.ActivityTaskManagerService, AMSHookMethod);
            log("hook AMSHook success");
        } catch (Exception e) {
            log("hook AMSHook fail:" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    XC_MethodHook AMSHookMethod = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            log("啦啦啦 AMSHookMethod !!!");
        }
    };

}
