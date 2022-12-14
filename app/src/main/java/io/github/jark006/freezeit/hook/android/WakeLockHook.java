package io.github.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.os.WorkSource;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class WakeLockHook {
    final static String TAG = "Freezeit[WakeLockHook]:";
    Config config;

    public WakeLockHook(Config config, LoadPackageParam lpParam) {
        this.config = config;

        // A13 SDK T+ 33+
        // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r18:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;l=1473
        // private void acquireWakeLockInternal(IBinder lock, int displayId, int flags, String tag,
        //        String packageName, WorkSource ws, String historyTag, int uid, int pid,
        //        @Nullable IWakeLockCallback callback)

        // A12 SDK S+ 31-32
        // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;l=1358
        // private void acquireWakeLockInternal(IBinder lock, int displayId, int flags, String tag,
        //            String packageName, WorkSource ws, String historyTag, int uid, int pid)

        // A10-11 SDK Q-R 29-30
        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;l=1278
        // private void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName,
        //            WorkSource ws, String historyTag, int uid, int pid)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, callback,
                    Enum.Class.PowerManagerService, Enum.Method.acquireWakeLockInternal,
                    IBinder.class, int.class, int.class, String.class,
                    String.class, WorkSource.class, String.class, int.class, int.class,
                    Enum.Class.IWakeLockCallback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            XpUtils.hookMethod(TAG, lpParam.classLoader, callback,
                    Enum.Class.PowerManagerService, Enum.Method.acquireWakeLockInternal,
                    IBinder.class, int.class, int.class, String.class,
                    String.class, WorkSource.class, String.class, int.class, int.class);
        } else {
            XpUtils.hookMethod(TAG, lpParam.classLoader, callback,
                    Enum.Class.PowerManagerService, Enum.Method.acquireWakeLockInternal,
                    IBinder.class, int.class, String.class,
                    String.class, WorkSource.class, String.class, int.class, int.class);
        }
    }

    XC_MethodHook callback = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {

            // 测试应用实际是否获得唤醒锁  10XXX为UID
            // dumpsys power|grep 10xxx

            // android S+ 12+:[7]    X~11:[6]
            int uid = (int) param.args[Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 7 : 6];

            // 宽松前台允许唤醒锁，但冻结时会被设为 可忽略
            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid) || config.tolerant.contains(uid))
                return;

            param.setResult(null);

//            String packageName = (String) param.args[Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 4 : 3];
//            log(TAG, "阻止获取唤醒锁：" + packageName);
        }
    };
}