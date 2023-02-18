package io.github.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class BroadCastHook {
    final static String TAG = "Freezeit[BroadCastHook]:";
    Config config;

    public BroadCastHook(Config config, LoadPackageParam lpParam) {
        this.config = config;

        XpUtils.hookMethod(TAG, lpParam.classLoader, callback,
                Enum.Class.BroadcastQueue, Enum.Method.deliverToRegisteredReceiverLocked,
                Enum.Class.BroadcastRecord, Enum.Class.BroadcastFilter, boolean.class, int.class);
    }

    /**
     * deliverToRegisteredReceiverLocked 处理动态注册的BroadCastReceiver
     * SDK29 ~ SDK33 (Android 10-13/Q-T) BroadcastQueue.java : deliverToRegisteredReceiverLocked()
     * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
     * <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=702">连接</a>
     * private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter,
     * boolean ordered, int index)
     */
    XC_MethodHook callback = new XC_MethodHook() {

        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {

            // BroadcastFilter https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastFilter.java
            int uid = XposedHelpers.getIntField(param.args[1], Enum.Field.owningUid); // receiverUid

            // 若是 [系统应用] [自由后台] [在顶层前台] 则不清理广播
            if (uid < 10000 || !config.managedApp.contains(uid) || config.whitelist.contains(uid) || config.foregroundUid.contains(uid))
                return;

            // BroadcastRecord https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastRecord.java
//            Object broadcastRecord = param.args[0];
//            int callerUid = (int) XposedHelpers.getObjectField(broadcastRecord, Enum.Field.callingUid);

            final int DELIVERY_SKIPPED = 2;  // BroadcastRecord.DELIVERY_SKIPPED == 2;
            int index = (int) param.args[3];
            int[] delivery = (int[]) XposedHelpers.getObjectField(param.args[0], "delivery");
            delivery[index] = DELIVERY_SKIPPED;
            param.setResult(null);
//            XpUtils.log(TAG, "跳过广播: " +
//                    config.pkgIndex.getOrDefault(callerUid, String.valueOf(callerUid)) +
//                    " 发往 " +
//                    config.pkgIndex.getOrDefault(receiverUid, String.valueOf(receiverUid))
//            );
        }
    };
}
