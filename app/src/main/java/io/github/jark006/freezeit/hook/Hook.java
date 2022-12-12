package io.github.jark006.freezeit.hook;


import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import io.github.jark006.freezeit.BuildConfig;
import io.github.jark006.freezeit.hook.android.AndroidService;
import io.github.jark006.freezeit.hook.android.AlarmHook;
import io.github.jark006.freezeit.hook.android.AnrHook;
import io.github.jark006.freezeit.hook.android.BroadCastHook;
import io.github.jark006.freezeit.hook.android.WakeLockHook;
import io.github.jark006.freezeit.hook.app.PowerKeeper;


public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) {
        switch (lpParam.packageName) {
            case Enum.Package.self:
                XC_MethodReplacement callback = XC_MethodReplacement.returnConstant(true);
                XpUtils.hookMethod("Freezeit[Self]:", lpParam.classLoader, callback, Enum.Class.self, Enum.Method.isXposedActive);
                return;
            case Enum.Package.android:
                hookAndroid(lpParam);
                return;
            case Enum.Package.powerkeeper:
                PowerKeeper.Hook(lpParam);
                return;
            default:
        }
    }

    public void hookAndroid(LoadPackageParam lpParam) {
        XposedBridge.log("Freezeit: " + BuildConfig.VERSION_NAME + " running");

        Config config = new Config();

        new AndroidService(config, lpParam);
        new AlarmHook(config, lpParam);
        new AnrHook(config, lpParam);
        new BroadCastHook(config, lpParam);
        new WakeLockHook(config, lpParam);
    }
}
