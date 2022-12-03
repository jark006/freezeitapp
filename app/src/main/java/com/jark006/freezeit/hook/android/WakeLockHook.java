package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.os.WorkSource;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WakeLockHook {
    final static String TAG = "Freezeit[WakeLockHook]:";
    Config config;

    public WakeLockHook(Config config, LoadPackageParam lpParam) {
        this.config = config;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.PowerManagerService, lpParam.classLoader,
                        Enum.Method.acquireWakeLockInternal,
                        IBinder.class, int.class, int.class, String.class,
                        String.class, WorkSource.class, String.class, int.class, int.class,
                        acquireWakeLockInternalHook);
                log(TAG + "hook success: acquireWakeLockInternal SDK S+ ");
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.PowerManagerService, lpParam.classLoader,
                        Enum.Method.acquireWakeLockInternal,
                        IBinder.class, int.class, String.class,
                        String.class, WorkSource.class, String.class, int.class, int.class,
                        acquireWakeLockInternalHook);
                log(TAG + "hook success: acquireWakeLockInternal X ~ R ");
            }
        } catch (Exception e) {
            log(TAG + "hook fail: acquireWakeLockInternal\n" + e);
        }
    }

    // SDK S+
    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;drc=62458f4b73f6f5a1e1b6c0045932192486f93601;l=1358
    // private void acquireWakeLockInternal(IBinder lock, int displayId, int flags, String tag,
    //            String packageName, WorkSource ws, String historyTag, int uid, int pid)

    // SDK x ~ R
    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;drc=4b1a1f130979c751f7690f5ecf75be7438121a83;l=1278
    // private void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName,
    //            WorkSource ws, String historyTag, int uid, int pid)
    XC_MethodHook acquireWakeLockInternalHook = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {

            // 测试应用实际是否获得唤醒锁  10XXX为UID
            // dumpsys power|grep 10xxx

            // android S+ 12+:7    X~11:6
            int uid = (int) param.args[Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 7 : 6];

            // 宽松前台允许唤醒锁，但冻结时会被设为 可忽略
            if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid) || config.tolerant.contains(uid))
                return;

//            String packageName = (String) param.args[Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 4 : 3];
//            log(TAG + "阻止：" + packageName);
            param.setResult(null);
        }
    };
}