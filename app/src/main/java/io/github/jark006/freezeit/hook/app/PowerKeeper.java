package io.github.jark006.freezeit.hook.app;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

// MIUI电量与性能 禁用杀后台
public class PowerKeeper {
    final static String TAG = "Freezeit[PowerKeeper]:";

    public static void Hook(XC_LoadPackage.LoadPackageParam lpParam) {

        ClassLoader classLoader = lpParam.classLoader;
        XC_MethodHook callback = XC_MethodReplacement.DO_NOTHING;

        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.ProcessManager, Enum.Method.kill, Enum.Class.ProcessConfig);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.SleepModeControllerNew, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.PowerStateMachine, Enum.Method.clearAppWhenScreenOffTimeOut);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.PowerStateMachine, Enum.Method.clearAppWhenScreenOffTimeOutInNight);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.PowerStateMachine, Enum.Method.clearUnactiveApps, Context.class);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.PowerCheckerController, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.PowerCheckerController, Enum.Method.autoKillApp, int.class, String.class);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.DynamicTurboPowerHandler, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, callback, Enum.Class.SleepProcessHelper, Enum.Method.killAppsInSleep);

    }
}
