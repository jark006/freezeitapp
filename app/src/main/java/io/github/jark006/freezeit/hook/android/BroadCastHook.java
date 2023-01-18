package io.github.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

/**
 * note:
 *
 * @link https://www.jianshu.com/p/bc290e328e56
 * @link https://www.jianshu.com/p/59ef3150b171
 * @link https://www.jianshu.com/p/6fbc1a43c837
 * @link https://blog.csdn.net/huaxun66/article/details/52935631
 */

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
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=702
     * private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter,
     * boolean ordered, int index)
     */
    XC_MethodHook callback = new XC_MethodHook() {

        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {

            // BroadcastFilter https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastFilter.java
            Object broadcastFilter = param.args[1];
            int receiverUid = XposedHelpers.getIntField(broadcastFilter, Enum.Field.owningUid);

            // 若是 [系统应用] [自由后台] [在顶层前台] 则不清理广播
            if (!config.thirdApp.contains(receiverUid) || config.whitelist.contains(receiverUid) || config.foreground.contains(receiverUid))
                return;

            // BroadcastRecord https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastRecord.java
            Object broadcastRecord = param.args[0];
//            int callerUid = (int) XposedHelpers.getObjectField(broadcastRecord, Enum.Field.callingUid);

            final int DELIVERY_SKIPPED = 2;  // BroadcastRecord.DELIVERY_SKIPPED == 2;
            int index = (int) param.args[3];
            int[] delivery = (int[]) XposedHelpers.getObjectField(broadcastRecord, "delivery");
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
