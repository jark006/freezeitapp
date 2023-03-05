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
                XpUtils.hookMethod("Freezeit[Self]:", lpParam.classLoader,
                        XC_MethodReplacement.returnConstant(true),
                        Enum.Class.self, Enum.Method.isXposedActive);
                return;
            case Enum.Package.android:
                hookAndroid(lpParam.classLoader);
                return;
            case Enum.Package.powerkeeper:
                PowerKeeper.Hook(lpParam.classLoader);
                return;
            default:
        }
    }

    public void hookAndroid(ClassLoader classLoader) {
        XposedBridge.log("Freezeit[" + BuildConfig.VERSION_NAME + "] Xposed running");

        Config config = new Config();

        new AndroidService(config, classLoader);
        new AlarmHook(config, classLoader);
        new AnrHook(config, classLoader);
        new BroadCastHook(config, classLoader);
//        new WakeLockHook(config, classLoader); // 改到 AndroidService
    }
}
