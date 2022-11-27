package com.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * note:
 * https://www.jianshu.com/p/bc290e328e56
 * https://www.jianshu.com/p/59ef3150b171
 * https://www.jianshu.com/p/6fbc1a43c837
 * https://blog.csdn.net/huaxun66/article/details/52935631
 */

/* deliverToRegisteredReceiverLocked 处理动态注册的BroadCastReceiver
 * SDK26 ~ SDK33 (Android 8.0-13/O-T) BroadcastQueue.java : deliverToRegisteredReceiverLocked()
 * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
 * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=629
 * Param private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered, int index)
 */

public class BroadCastHook {
    final static String TAG = "Freezeit[BroadCastHook]:";
    Config config;
    LoadPackageParam lpParam;

    public BroadCastHook(Config config, LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.BroadcastQueue, lpParam.classLoader,
                    Enum.Method.deliverToRegisteredReceiverLocked,
                    Enum.Class.BroadcastRecord, Enum.Class.BroadcastFilter, boolean.class, int.class,
                    deliverToRegisteredReceiverLockedHook);
            log("hook success: " + Enum.Class.BroadcastQueue);
        } catch (Exception e) {
            log("hook fail: " + Enum.Class.BroadcastQueue + "\n" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    XC_MethodHook deliverToRegisteredReceiverLockedHook = new XC_MethodHook() {

        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {
            Object[] args = param.args;

            // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastRecord.java
//            Object broadcastRecord = args[0];
//            int callerUid = (int) XposedHelpers.getObjectField(broadcastRecord, Enum.Field.callingUid);

            // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastFilter.java
            Object broadcastFilter = args[1];
            int receiverUid = (int) XposedHelpers.getObjectField(broadcastFilter, Enum.Field.owningUid);

            // 若是 [系统应用] [自由后台] [在顶层前台] [暂未冻结] 则不清理广播
            if (!config.thirdApp.contains(receiverUid) || config.whitelist.contains(receiverUid) ||
                    config.top.contains(receiverUid) || config.topOrRunning.contains(receiverUid))
                return;

            ArrayList<?> receiverList = (ArrayList<?>) XposedHelpers.getObjectField(broadcastFilter, Enum.Field.receiverList);
            if (receiverList == null)
                return;

            // 清理广播 https://cs.android.com/android/platform/superproject/+/android-12.1.0_r27:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;drc=ac41939818667beefaa7fbbdc16ad7eeb63e65ee;l=680
            XposedHelpers.setObjectField(receiverList, Enum.Field.app, null);

//            String callerPackage = (String) XposedHelpers.getObjectField(broadcastRecord, Enum.Field.callerPackage);
//            String receiverPackage = (String) XposedHelpers.getObjectField(broadcastFilter, Enum.Field.packageName);
//            log("Clear broadcast of [" + callerPackage + "] to [" + receiverPackage + "]");
        }
    };
}
