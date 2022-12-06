package io.github.jark006.freezeit.hook.app;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;

import io.github.jark006.freezeit.hook.Enum;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

// MIUI电量与性能 禁用杀后台
public class powerkeeper {
    final static String TAG = "Freezeit[PowerKeeper]:";

    public static void Hook(XC_LoadPackage.LoadPackageParam lpParam) {
        // 已通过 system.prop 禁用millet
        // persist.sys.gz.enable=false
        // persist.sys.millet.handshake=false
        // persist.sys.powmillet.enable=false
        // persist.sys.brightmillet.enable=false

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.ProcessManager, lpParam.classLoader,
                    Enum.Method.kill, Enum.Class.ProcessConfig,
                    XC_MethodReplacement.returnConstant(false));
            log(TAG + "disable success: ProcessManager.kill()");
        } catch (Exception e) {
            log(TAG + "disable fail: ProcessManager.kill()");
        }

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.SleepModeControllerNew, lpParam.classLoader,
                    Enum.Method.clearApp, XC_MethodReplacement.DO_NOTHING);
            log(TAG + "disable success: SleepModeControllerNew.clearApp()");
        } catch (Exception e) {
            log(TAG + "disable fail: SleepModeControllerNew.clearApp()");
        }

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader,
                    Enum.Method.clearAppWhenScreenOffTimeOut, XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader,
                    Enum.Method.clearAppWhenScreenOffTimeOutInNight, XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader,
                    Enum.Method.clearUnactiveApps, Context.class, XC_MethodReplacement.DO_NOTHING);
            log(TAG + "disable success: PowerStateMachine.clearAppWhenScreenOffTimeOut/InNight()");
            log(TAG + "disable success: PowerStateMachine.clearUnactiveApps()");
        } catch (Exception e) {
            log(TAG + "disable fail: PowerStateMachine.clearAppWhenScreenOffTimeOut/InNight()");
            log(TAG + "disable fail: PowerStateMachine.clearUnactiveApps()");
        }

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.PowerCheckerController, lpParam.classLoader,
                    Enum.Method.clearApp, XC_MethodReplacement.DO_NOTHING);
            XposedHelpers.findAndHookMethod(Enum.Class.PowerCheckerController, lpParam.classLoader,
                    Enum.Method.autoKillApp, int.class, String.class, XC_MethodReplacement.DO_NOTHING);
            log(TAG + "disable success: PowerCheckerController.clearApp()");
            log(TAG + "disable success: PowerCheckerController.autoKillApp()");
        } catch (Exception e) {
            log(TAG + "disable fail: PowerCheckerController.clearApp()");
            log(TAG + "disable fail: PowerCheckerController.autoKillApp()");
        }

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.DynamicTurboPowerHandler, lpParam.classLoader,
                    Enum.Method.clearApp, XC_MethodReplacement.DO_NOTHING);
            log(TAG + "disable success: DynamicTurboPowerHandler.clearApp()");
        } catch (Exception e) {
            log(TAG + "disable fail: DynamicTurboPowerHandler.clearApp()");
        }

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.SleepProcessHelper, lpParam.classLoader,
                    Enum.Method.killAppsInSleep, XC_MethodReplacement.DO_NOTHING);
            log(TAG + "disable success: SleepProcessHelper.killAppsInSleep()");
        } catch (Exception e) {
            log(TAG + "disable fail: SleepProcessHelper.killAppsInSleep()");
        }
    }
}
