package io.github.jark006.freezeit.hook;


import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.BuildConfig;
import io.github.jark006.freezeit.hook.android.AndroidService;
import io.github.jark006.freezeit.hook.android.AlarmHook;
import io.github.jark006.freezeit.hook.android.AnrHook;
import io.github.jark006.freezeit.hook.android.BroadCastHook;
import io.github.jark006.freezeit.hook.android.WakeLockHook;
import io.github.jark006.freezeit.hook.app.powerkeeper;


public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) {
        switch (lpParam.packageName) {
            case Enum.Package.self:
                XposedHelpers.findAndHookMethod(Enum.Package.self + ".fragment.HomeFragment",
                        lpParam.classLoader, "isXposedActive", XC_MethodReplacement.returnConstant(true));
                return;
            case Enum.Package.powerkeeper:
                powerkeeper.Hook(lpParam);
                return;
            case Enum.Package.android:
                hookAndroid(lpParam);
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
