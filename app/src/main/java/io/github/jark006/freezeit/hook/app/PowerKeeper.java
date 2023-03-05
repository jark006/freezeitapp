package io.github.jark006.freezeit.hook.app;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

// MIUI电量与性能 禁用杀后台
public class PowerKeeper {
    final static String TAG = "Freezeit[PowerKeeper]:";

    public static void Hook(ClassLoader classLoader) {

        final XC_MethodHook doNothing = XC_MethodReplacement.DO_NOTHING;
        final XC_MethodHook returnTrue = XC_MethodReplacement.returnConstant(true);

        XpUtils.hookMethod(TAG, classLoader, returnTrue, Enum.Class.ProcessManager, Enum.Method.kill, Enum.Class.ProcessConfig);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.SleepModeControllerNew, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.PowerStateMachine, Enum.Method.clearAppWhenScreenOffTimeOut);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.PowerStateMachine, Enum.Method.clearAppWhenScreenOffTimeOutInNight);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.PowerStateMachine, Enum.Method.clearUnactiveApps, Context.class);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.PowerCheckerController, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, returnTrue, Enum.Class.PowerCheckerController, Enum.Method.autoKillApp, int.class, String.class);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.DynamicTurboPowerHandler, Enum.Method.clearApp);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.SleepProcessHelper, Enum.Method.killAppsInSleep);

        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.ForceDozeController, Enum.Method.removeWhiteListAppsIfEnterForceIdle);
        XpUtils.hookMethod(TAG, classLoader, doNothing, Enum.Class.ForceDozeController, Enum.Method.restoreWhiteListAppsIfQuitForceIdle);

    }
}
