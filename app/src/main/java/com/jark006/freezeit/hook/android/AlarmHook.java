package com.jark006.freezeit.hook.android;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.IBinder;
import android.os.WorkSource;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Iterator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AlarmHook {
    final static String TAG = "Freezeit[AlarmHook]:";
    Config config;
    LoadPackageParam lpParam;

    public AlarmHook(Config config, LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.AlarmManagerServiceS, lpParam.classLoader,
                        Enum.Method.triggerAlarmsLocked, ArrayList.class, long.class, triggerAlarmsLockedHook);
                log("hook success: AlarmManagerServiceS SDK S+ ");
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.AlarmManagerServiceR, lpParam.classLoader,
                        Enum.Method.triggerAlarmsLocked, ArrayList.class, long.class, triggerAlarmsLockedHook);
                log("hook success: AlarmManagerServiceS X ~ R ");
            }
        } catch (Exception e) {
            log("hook fail: AlarmManagerServiceS\n" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    // SDK S+
    // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java;l=3870
    // int triggerAlarmsLocked(ArrayList<Alarm> triggerList, final long nowELAPSED)

    // SDK x ~ R
    // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3499
    // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3469
    XC_MethodHook triggerAlarmsLockedHook = new XC_MethodHook() {
        @SuppressLint("DefaultLocale")
        public void afterHookedMethod(MethodHookParam param) {
            Object[] args = param.args;

            // Alarm
            // SDK31 https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:frameworks/base/apex/jobscheduler/service/java/com/android/server/alarm/Alarm.java
            // SDK30 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/AlarmManagerService.java;l=3636
            ArrayList<?> triggerList = (ArrayList<?>) args[0];

            // 注意：JAVA迭代器与C++不同，C++ item.begin() 指向首个元素
            // JAVA的迭代器初始状：指向首个元素的前一个位置
            Iterator<?> iterator = triggerList.iterator();
            while (iterator.hasNext()) {
                Object Alarm = iterator.next(); //迭代器后移，再返回新位置的元素
                int uid = XposedHelpers.getIntField(Alarm, Enum.Field.uid);

                if (!config.thirdApp.contains(uid)) continue;
                if (config.whitelist.contains(uid)) continue;

                // TODO  播放中不冻结 也暂时屏蔽
//                String packageName = (String) XposedHelpers.getObjectField(Alarm, Enum.Field.packageName);
//                if (config.playingExcept.contains(uid)) {
//                    log("播放中不冻结：" + packageName);
//                    return;
//                }
//                log("清理Alarm: " + packageName);
                iterator.remove();
            }
        }
    };
}
