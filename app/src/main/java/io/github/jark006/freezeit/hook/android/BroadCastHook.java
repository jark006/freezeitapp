package io.github.jark006.freezeit.hook.android;

import static io.github.jark006.freezeit.hook.XpUtils.log;

import android.annotation.SuppressLint;
import android.os.Build;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class BroadCastHook {
    final static String TAG = "Freezeit[BroadCastHook]:";
    Config config;

    Method skipReceiverLockedMethod;

    public BroadCastHook(Config config, ClassLoader classLoader) {
        this.config = config;

        // 动态广播
        XpUtils.hookMethod(TAG, classLoader, registeredReceiverCallback,
                Enum.Class.BroadcastQueue, Enum.Method.deliverToRegisteredReceiverLocked,
                Enum.Class.BroadcastRecord, Enum.Class.BroadcastFilter, boolean.class, int.class);

        // 静态广播
        skipReceiverLockedMethod = XposedHelpers.findMethodExactIfExists(
                Enum.Class.BroadcastQueue, classLoader, Enum.Method.skipReceiverLocked,
                Enum.Class.BroadcastRecord);
        boolean isSuccess = false;
        if (skipReceiverLockedMethod != null) {
            skipReceiverLockedMethod.setAccessible(true);

            // A13-30 https://cs.android.com/android/platform/superproject/+/android-13.0.0_r31:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=323
            // private final void processCurBroadcastLocked(BroadcastRecord r,
            //            ProcessRecord app)

            // A13-release https://cs.android.com/android/platform/superproject/+/android13-release:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=323
            // private final void processCurBroadcastLocked(BroadcastRecord r,
            //            ProcessRecord app, int receiverType, int processTemperature)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                isSuccess = XpUtils.hookMethod(TAG, classLoader, receiverCallback,
                        Enum.Class.BroadcastQueue, Enum.Method.processCurBroadcastLocked,
                        Enum.Class.BroadcastRecord, Enum.Class.ProcessRecord, int.class, int.class);
                if (!isSuccess)
                    isSuccess = XpUtils.hookMethod(TAG, classLoader, receiverCallback,
                            Enum.Class.BroadcastQueue, Enum.Method.processCurBroadcastLocked,
                            Enum.Class.BroadcastRecord, Enum.Class.ProcessRecord);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isSuccess = XpUtils.hookMethod(TAG, classLoader, receiverCallback,
                        Enum.Class.BroadcastQueue, Enum.Method.processCurBroadcastLocked,
                        Enum.Class.BroadcastRecord, Enum.Class.ProcessRecord);
            } else {
                isSuccess = XpUtils.hookMethod(TAG, classLoader, receiverCallback,
                        Enum.Class.BroadcastQueue, Enum.Method.processCurBroadcastLocked,
                        Enum.Class.BroadcastRecord, Enum.Class.ProcessRecord, boolean.class);
            }
        }
        log(TAG, "Init skipReceiverLockedMethod " + (isSuccess ? "success" : "fail"));
    }

    /**
     * deliverToRegisteredReceiverLocked 处理动态注册的BroadCastReceiver
     * SDK29 ~ SDK33 (Android 10-13/Q-T) BroadcastQueue.java : deliverToRegisteredReceiverLocked()
     * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
     * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=702">连接</a>
     * private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter,
     * boolean ordered, int index)
     */
    XC_MethodHook registeredReceiverCallback = new XC_MethodHook() {

        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {
            // BroadcastFilter https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastFilter.java
            final int uid = config.getBroadcastFilterOwningUid(param.args[1]);// BroadcastFilter

            // 不在管理范围，或顶层前台 则不清理广播
            if (!config.managedApp.contains(uid) || config.foregroundUid.contains(uid))
                return;

            // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=946
            final var delivery = config.getBroadcastRecordDelivery(param.args[0]);
            if (delivery == null) return;
            delivery[(int) param.args[3]] = 2;// index == param.args[3], DELIVERY_SKIPPED == 2;

            param.setResult(null);
            if (XpUtils.DEBUG_BROADCAST) {
                // BroadcastRecord https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastRecord.java
                final int callerUid = config.getBroadcastRecordCallingUid(param.args[0]); // broadcastRecord
                XpUtils.log(TAG, "动态广播 清理: " +
                        config.pkgIndex.getOrDefault(callerUid, String.valueOf(callerUid)) +
                        " 发往 " +
                        config.pkgIndex.getOrDefault(uid, String.valueOf(uid))
                );
            }
        }
    };

    /**
     * processCurBroadcastLocked 处理静态广播
     * SDK29 ~ SDK33 (Android 10-13/Q-T) BroadcastQueue.java : processCurBroadcastLocked()
     * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
     * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=325">连接</a>
     * private final void processCurBroadcastLocked(BroadcastRecord r, ProcessRecord app)
     */
    XC_MethodHook receiverCallback = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {
            final int uid = config.getProcessRecordUid(param.args[1]);// processRecord

            // 不在管理范围，或顶层前台 则不清理广播
            if (!config.managedApp.contains(uid) || config.foregroundUid.contains(uid))
                return;

            try {
                skipReceiverLockedMethod.invoke(param.thisObject, param.args[0]);
                param.setResult(null);
                if (XpUtils.DEBUG_BROADCAST) {
                    final int callerUid = config.getBroadcastRecordCallingUid(param.args[0]);// broadcastRecord
                    XpUtils.log(TAG, "静态广播 清理: " +
                            config.pkgIndex.getOrDefault(callerUid, String.valueOf(callerUid)) +
                            " 发往 " +
                            config.pkgIndex.getOrDefault(uid, String.valueOf(uid))
                    );
                }
            } catch (Exception ignore) {
            }
        }
    };
}