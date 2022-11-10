package com.jark006.freezeit.hook.android;

import com.jark006.freezeit.hook.Config;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WakeLockHook {
    final static String TAG = "Freezeit[WakeLockHook]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

    // TODO
    public WakeLockHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

    }

//        XC_MethodHook acquireHook = new XC_MethodHook() {
//            @SuppressLint("DefaultLocale")
//            public void beforeHookedMethod(MethodHookParam param) {
//                String mPackageName = (String) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mPackageName);
//                if (mPackageName == null || mPackageName.length() == 0)
//                    return;
//
////                if (!config.thirdApp.contains(mPackageName))
////                    return;
//
//                String mTag = (String) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mTag);
//                if (config.whitelist.contains(mPackageName)) {
//                    log("WakeLock allow whitelist:" + mPackageName + " mTag:" + mTag);
//                    return;
//                }
////                if (config.dynamic.contains(mPackageName) && soundDevices.playCount > 0) {
////                    String mTag = (String) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mTag);
////                    log("WakeLock allow dynamic:" + mPackageName + " mTag:" + mTag);
////                    return;
////                }
//                log("WakeLock block:" + mPackageName + " mTag:" + mTag);
//                param.setResult(null); // 阻止继续执行 Hook的函数
//            }
//        };
//        try {
//            XposedHelpers.findAndHookMethod(Enum.Class.WakeLock, lpParam.classLoader, Enum.Method.acquire, acquireHook);
//            XposedHelpers.findAndHookMethod(Enum.Class.WakeLock, lpParam.classLoader, Enum.Method.acquire, long.class, acquireHook);
//            log("hook WakeLock acquire success");
//        } catch (Exception e) {
//            log("hook WakeLock acquire fail:" + e);
//        }

//
//        try {
//            Class<?> clazz = XposedHelpers.findClassIfExists(Enum.Class.PowerManagerService, lpParam.classLoader);
//            if (clazz == null) {
//                log("PowerManagerService class not find");
//                return;
//            }
//            XposedHelpers.findAndHookMethod(clazz, "acquireWakeLockInternal", new XC_MethodHook() {
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                    super.beforeHookedMethod(param);
//
//                    String packageName;
//                    int uid;
//
//                    if (Build.VERSION.SDK_INT >= 31) {  //android 12+
//                        // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;l=1358
//                        packageName = (String) param.args[4];
//                        uid = (int) param.args[7];
//                    } else { //android 9~11
//                        // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/power/PowerManagerService.java;l=1278
//                        packageName = (String) param.args[3];
//                        uid = (int) param.args[6];
//                    }
//
//                    if (uid < 10000) return;
//
//                    if (!config.whitelist.contains(packageName)) {
//                        log("WakeLock block:" + packageName);
//                        param.setResult(null); // 阻止继续执行 被Hook函数
//                    }
//                }
//            });
//            log("hook WakeLock acquireWakeLockInternal success");
//        } catch (Exception e) {
//            log("hook WakeLock acquireWakeLockInternal fail:" + e);
//        }
}
