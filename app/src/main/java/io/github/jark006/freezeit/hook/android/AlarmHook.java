package io.github.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;
import android.os.Build;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class AlarmHook {
    final static String TAG = "Freezeit[AlarmHook]:";
    Config config;

    public AlarmHook(Config config, LoadPackageParam lpParam) {
        this.config = config;

        XpUtils.hookMethod(TAG, lpParam.classLoader, callback,
                Build.VERSION.SDK_INT >= 31 ? Enum.Class.AlarmManagerServiceS : Enum.Class.AlarmManagerServiceR,
                Enum.Method.triggerAlarmsLocked, ArrayList.class, long.class);
    }

    // SDK S-T A12-13
    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java;l=3870
    // int triggerAlarmsLocked(ArrayList<Alarm> triggerList, final long nowELAPSED)

    // SDK x ~ R
    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3499
    // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3469
    XC_MethodHook callback = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void afterHookedMethod(MethodHookParam param) {
            // Alarm
            // SDK31 https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/apex/jobscheduler/service/java/com/android/server/alarm/Alarm.java
            // SDK30 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3636
            var iterator = ((ArrayList<?>) param.args[0]).iterator(); //triggerList
            while (iterator.hasNext()) {
                final var Alarm = iterator.next(); //迭代器后移，再返回新位置的元素
                final int uid = XpUtils.getInt(Alarm, Enum.Field.uid);
                if (!config.managedApp.contains(uid) || config.foregroundUid.contains(uid))
                    continue;

                iterator.remove();
                if (XpUtils.DEBUG_ALARM)
                    XpUtils.log(TAG, "清理 Alarm: " + XpUtils.getString(Alarm, Enum.Field.packageName) +
                            " statsTag:" + XpUtils.getString(Alarm, "statsTag"));
            }
        }
    };
}
