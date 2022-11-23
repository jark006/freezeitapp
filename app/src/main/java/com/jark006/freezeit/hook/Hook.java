package com.jark006.freezeit.hook;

import android.content.Context;

import com.jark006.freezeit.BuildConfig;
import com.jark006.freezeit.hook.android.AlarmHook;
import com.jark006.freezeit.hook.android.AnrHook;
import com.jark006.freezeit.hook.android.BroadCastHook;
import com.jark006.freezeit.hook.android.LruProcessesHook;
import com.jark006.freezeit.hook.android.WakeLockHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


public class Hook implements IXposedHookLoadPackage {
    final static String TAG = "Freezeit: ";

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) {
        switch (lpParam.packageName) {
            case Enum.Package.self:
                hookSelf(lpParam);
                return;
            case Enum.Package.powerkeeper:
                hookPowerkeeper(lpParam);
                return;
            case Enum.Package.android:
                hookAndroid(lpParam);
                return;
            default:
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    public void hookAndroid(LoadPackageParam lpParam) {
        log(BuildConfig.VERSION_NAME + " running");
        Config config = new Config();

//        Class<?> clazzAMS = XposedHelpers.findClass(Enum.Class.ActivityManagerService, lpParam.classLoader);
//        Object mProcessList = XposedHelpers.findField(clazzAMS, Enum.Field.mProcessList);
//        SparseArray<?> mActiveUids = (SparseArray<?>)XposedHelpers.getObjectField(mProcessList, Enum.Field.mActiveUids);

//        final Object[] ActiveUids = new Object[1];
//        Class<?> clazzActiveUids = XposedHelpers.findClass(Enum.Class.ActiveUids, lpParam.classLoader);
//        XposedHelpers.findAndHookConstructor(clazzActiveUids, new XC_MethodHook() {//Hook无参构造函数
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) {
//                ActiveUids[0] = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mActiveUids);
//            }
//        });
//        SparseArray<?> mActiveUids = (SparseArray<?>)ActiveUids[0];

        new AnrHook(config, lpParam);        // ANR
        new BroadCastHook(config, lpParam);  // Broadcast
        new WakeLockHook(config, lpParam);   // WakeLock
        new AlarmHook(config, lpParam);      // Alarm
        new LruProcessesHook(config, lpParam);// ProcessListHook
    }


    public void hookSelf(LoadPackageParam lpParam) {
        try {
            XposedHelpers.findAndHookMethod(Enum.Package.self + ".fragment.HomeFragment",
                    lpParam.classLoader, "isXposedActive",
                    XC_MethodReplacement.returnConstant(true));
            log("hook self: isXposedActive(): return:true");
        } catch (Exception e) {
            log("hook self fail:" + e);
        }
    }

    // MIUI电量与性能 禁用杀后台
    public void hookPowerkeeper(LoadPackageParam lpParam) {
        // 已通过 system.prop 禁用millet
        // persist.sys.gz.enable=false
        // persist.sys.millet.handshake=false
        // persist.sys.powmillet.enable=false
        // persist.sys.brightmillet.enable=false

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.ProcessManager, lpParam.classLoader, Enum.Method.kill, Enum.Class.ProcessConfig, XC_MethodReplacement.returnConstant(false));
            log("disable success ProcessManager.kill()");
        } catch (Exception e) {
            log("disable fail ProcessManager.kill(): " + e);
        }

        Class<?> clazzSleepModeControllerNew = XposedHelpers.findClassIfExists(Enum.Class.SleepModeControllerNew, lpParam.classLoader);
        Method methodClearApp = (clazzSleepModeControllerNew == null) ? null :
                XposedHelpers.findMethodExactIfExists(clazzSleepModeControllerNew, Enum.Method.clearApp);
        if (methodClearApp != null) {
            try {
                XposedHelpers.findAndHookMethod(Enum.Class.SleepModeControllerNew, lpParam.classLoader, Enum.Method.clearApp, XC_MethodReplacement.DO_NOTHING);
                log("disable success: clearApp()");
            } catch (Exception e) {
                log("disable fail: clearApp(): " + e);
            }
        } else {
            log("not support clearApp():" + (clazzSleepModeControllerNew != null) + " " + (methodClearApp != null));
        }

        Class<?> clazzPowerStateMachine = XposedHelpers.findClassIfExists(Enum.Class.PowerStateMachine, lpParam.classLoader);
        if (clazzPowerStateMachine != null) {
            try {
                XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader, Enum.Method.clearAppWhenScreenOffTimeOut, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader, Enum.Method.clearAppWhenScreenOffTimeOutInNight, XC_MethodReplacement.DO_NOTHING);
                XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader, Enum.Method.clearUnactiveApps, Context.class, XC_MethodReplacement.DO_NOTHING);
                log("disable success: clearAppWhenScreenOffTimeOut/InNight()");
                log("disable success: clearUnactiveApps()");
            } catch (Exception e) {
                log("disable fail: clearAppWhenScreenOffTimeOut/InNight()");
                log("disable fail: clearUnactiveApps(): " + e);
            }
        } else {
            log("can't find clearApp() / clearUnactiveApps()");
        }
    }
}
