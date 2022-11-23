package com.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.os.WorkSource;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WakeLockHook {
    final static String TAG = "Freezeit[WakeLockHook]:";
    Config config;
    LoadPackageParam lpParam;

    public WakeLockHook(Config config, LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.PowerManagerService, lpParam.classLoader,
                        Enum.Method.acquireWakeLockInternal,
                        IBinder.class, int.class, int.class, String.class,
                        String.class, WorkSource.class, String.class, int.class, int.class,
                        acquireWakeLockInternalHook);
                log("hook success: acquireWakeLockInternal SDK S+ ");
            }else{
                XposedHelpers.findAndHookMethod(Enum.Class.PowerManagerService, lpParam.classLoader,
                        Enum.Method.acquireWakeLockInternal,
                        IBinder.class, int.class, String.class,
                        String.class, WorkSource.class, String.class, int.class, int.class,
                        acquireWakeLockInternalHook);
                log("hook success: acquireWakeLockInternal X ~ R ");
            }
        } catch (Exception e) {
            log("hook fail: acquireWakeLockInternal\n" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }


    // SDK S+
    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;drc=62458f4b73f6f5a1e1b6c0045932192486f93601;l=1358
    // private void acquireWakeLockInternal(IBinder lock, int displayId, int flags, String tag,
    //            String packageName, WorkSource ws, String historyTag, int uid, int pid)

    // SDK x ~ R
    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;drc=4b1a1f130979c751f7690f5ecf75be7438121a83;l=1278
    // private void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName,
    //            WorkSource ws, String historyTag, int uid, int pid)
    XC_MethodHook acquireWakeLockInternalHook= new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void beforeHookedMethod(MethodHookParam param) {
            Object[] args = param.args;

            // 测试应用实际是否获得唤醒锁  10XXX为UID
            // dumpsys power|grep 10xxx

            //String packageName;
            int uid;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) {  //android S+ 12+
                //packageName = (String) param.args[4];
                uid = (int) args[7];
            } else { //android x~R  X~11
                //packageName = (String) param.args[3];
                uid = (int) args[6];
            }

            if (!config.thirdApp.contains(uid)) return;
            if (config.whitelist.contains(uid)) return;
            if (config.playingExcept.contains(uid)) return;// 允许唤醒锁，但冻结时会被设为 可忽略

            //log("阻止：" + packageName);
            param.setResult(null); // 阻止继续执行 被Hook函数
        }
    };
}