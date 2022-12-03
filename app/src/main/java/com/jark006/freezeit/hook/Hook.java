package com.jark006.freezeit.hook;


import com.jark006.freezeit.BuildConfig;
import com.jark006.freezeit.hook.android.AMSHook;
import com.jark006.freezeit.hook.android.AlarmHook;
import com.jark006.freezeit.hook.android.AnrHook;
import com.jark006.freezeit.hook.android.BroadCastHook;
import com.jark006.freezeit.hook.android.LruProcessesHook;
import com.jark006.freezeit.hook.android.ProcessStateRecordHook;
import com.jark006.freezeit.hook.android.WakeLockHook;
import com.jark006.freezeit.hook.app.powerkeeper;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;


public class Hook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam)
    {
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
        XposedBridge.log("Freezeit: "+BuildConfig.VERSION_NAME + " running");

        Config config = new Config();

        new AlarmHook(config, lpParam);         // Alarm
//        new AMSHook(config, lpParam);           // AMSHook
        new AnrHook(config, lpParam);           // ANR
        new BroadCastHook(config, lpParam);     // Broadcast
//        new LruProcessesHook(config, lpParam);  // LruProcesses
        new ProcessStateRecordHook(config, lpParam);  // LruProcesses
        new WakeLockHook(config, lpParam);      // WakeLock
    }
}
