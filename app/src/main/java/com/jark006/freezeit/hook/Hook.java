package com.jark006.freezeit.hook;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

import static com.jark006.freezeit.hook.Enum.Method.add;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.jark006.freezeit.BuildConfig;
import com.jark006.freezeit.Utils;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.lang.reflect.Method;
import java.util.HashSet;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * note:
 * https://www.jianshu.com/p/bc290e328e56
 * https://www.jianshu.com/p/59ef3150b171
 * http://events.jianshu.io/p/6fbc1a43c837
 * https://blog.csdn.net/huaxun66/article/details/52935631
 */
public class Hook implements IXposedHookLoadPackage {

    final static String TAG = "Freezeit: ";
    final boolean LOG2UDP = false;

    static final HashSet<String> whiteListAction = new HashSet<String>() {{
        add("asd");
        add("asad");
    }};

    @Override
    public void handleLoadPackage(LoadPackageParam lpParam) {

        if (lpParam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            try {
                XposedHelpers.findAndHookMethod(BuildConfig.APPLICATION_ID + ".HomeFragment",
                        lpParam.classLoader, "isXposedActive",
                        XC_MethodReplacement.returnConstant(true));
                log("hook self: isXposedActive(): return:true");
            } catch (Exception e) {
                log("hook self fail:" + e);
            }
            return;
        } else if (lpParam.packageName.equals("com.miui.powerkeeper")) {
            Class<?> clazzMilletConfig = XposedHelpers.findClassIfExists(Enum.Class.MilletConfig, lpParam.classLoader);
            Method method = (clazzMilletConfig == null) ? null :
                    XposedHelpers.findMethodExactIfExists(clazzMilletConfig, Enum.Method.getEnable, Context.class);
            if (method != null) {
                try {
                    XposedHelpers.findAndHookMethod(Enum.Class.MilletConfig,
                            lpParam.classLoader, Enum.Method.getEnable, Context.class,
                            XC_MethodReplacement.returnConstant(false));
                    log("disable millet success");
                } catch (Exception e) {
                    log("disable millet fail:" + e);
                }
            } else {
                log("not support millet");
            }

            Class<?> clazzSleepModeControllerNew = XposedHelpers.findClassIfExists(Enum.Class.SleepModeControllerNew, lpParam.classLoader);
            Method methodClearApp = (clazzSleepModeControllerNew == null) ? null :
                    XposedHelpers.findMethodExactIfExists(clazzSleepModeControllerNew, Enum.Method.clearApp);
            if (methodClearApp != null) {
                try {
                    XposedHelpers.findAndHookMethod(Enum.Class.SleepModeControllerNew, lpParam.classLoader, Enum.Method.clearApp, XC_MethodReplacement.DO_NOTHING);
                    log("disable clearApp success");
                } catch (Exception e) {
                    log("disable clearApp fail:" + e);
                }
            } else {
                log("not support clearApp:" + (clazzSleepModeControllerNew != null) + " " + (methodClearApp != null));
            }

            Class<?> clazzPowerStateMachine = XposedHelpers.findClassIfExists(Enum.Class.PowerStateMachine, lpParam.classLoader);
            if (clazzPowerStateMachine != null) {
                try {
                    XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader, Enum.Method.clearAppWhenScreenOffTimeOut, XC_MethodReplacement.DO_NOTHING);
                    XposedHelpers.findAndHookMethod(Enum.Class.PowerStateMachine, lpParam.classLoader, Enum.Method.clearAppWhenScreenOffTimeOutInNight, XC_MethodReplacement.DO_NOTHING);
                    log("disable clearAppWhenScreenOffTimeOut/InNight success");
                } catch (Exception e) {
                    log("disable clearAppWhenScreenOffTimeOut/InNight fail:" + e);
                }
            } else {
                log("can't find clearApp");
            }


            return;
        }

        if (!lpParam.packageName.equals("android"))
            return;

        log(BuildConfig.VERSION_NAME + " running");

        Config config = new Config();
        config.startWatching();

        Sound soundDevices = new Sound();
        soundDevices.startWatching();


        /*
         * SDK30, Android 11/R add AnrHelper.java
         * SourceCode frameworks/base/services/core/java/com/android/server/am/AnrHelper.java
         * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/AnrHelper.java;l=73
         * param void appNotResponding(ProcessRecord anrProcess, String activityShortComponentName,
         *         ApplicationInfo aInfo, String parentShortComponentName,
         *         WindowProcessController parentProcess, boolean aboveSystem, String annotation)
         */
        XC_MethodReplacement appNotRespondingReplacement = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                Object[] args = param.args;

                if (args[0] == null) return null;

                ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(args[0], Enum.Field.info);
                if (appInfo == null) return null;

                boolean isSystem = (appInfo.flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;

                if (isSystem) {
                    Object anrRecords = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mAnrRecords);
                    synchronized (anrRecords) {
                        Class<?> AnrRecord = XposedHelpers.findClass(Enum.Class.AnrRecord, lpParam.classLoader);
                        Object anrRecord = XposedHelpers.newInstance(AnrRecord, args);
                        XposedHelpers.callMethod(anrRecords, add, anrRecord);
                    }

                    XposedHelpers.callMethod(param.thisObject, Enum.Method.startAnrConsumerIfNeeded);
                }
                return null;
            }
        };

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                XposedHelpers.findAndHookMethod(Enum.Class.AnrHelper, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, String.class, Enum.Class.ApplicationInfo, String.class,
                        Enum.Class.WindowProcessController, boolean.class, String.class, appNotRespondingReplacement);
                log("hook AnrHelper: Android 11+/R+ success");
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessRecord, lpParam.classLoader, Enum.Method.appNotResponding,
                        String.class, Enum.Class.ApplicationInfo, String.class, Enum.Class.WindowProcessController,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log("hook ProcessRecord: Android 10/Q success");
            } else {
                XposedHelpers.findAndHookMethod(Enum.Class.AppErrors, lpParam.classLoader, Enum.Method.appNotResponding,
                        Enum.Class.ProcessRecord, Enum.Class.ActivityRecord, Enum.Class.ActivityRecord,
                        boolean.class, String.class, XC_MethodReplacement.DO_NOTHING);
                log("hook AppErrors: Android 7.0-9/N-P success");
            }
        } catch (Exception e) {
            log("hook [ AnrHelper/ProcessRecord/AppErrors ] fail:" + e);
        }

        /**
         * 静态和动态注册的广播接受者
         *
         * 1. 普通/无序广播（Normal Broadcast）并发广播
         * 2. 有序广播（Ordered Broadcast）串行广播
         * 3. 前台广播和后台广播
         *
         * 不参与 应用内广播LocalBroadCastManager
         */


        /* deliverToRegisteredReceiverLocked 处理动态注册的BroadCastReceiver
         * SDK26 ~ SDK33 (Android 8.0-13/O-T) BroadcastQueue.java : deliverToRegisteredReceiverLocked()
         * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
         * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=629
         * Param private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered, int index)
         */
        XC_MethodHook deliverToRegisteredReceiverLockedHook = new XC_MethodHook() {
            @SuppressLint("DefaultLocale")
            public void beforeHookedMethod(MethodHookParam param) {
                Object[] args = param.args;
                if (args[1] == null)
                    return;

                // BroadcastFilter
                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastFilter.java
                Object broadcastFilter = args[1];

                String receiverPackage = (broadcastFilter == null) ? null : (String) XposedHelpers.getObjectField(broadcastFilter, Enum.Field.packageName);
                if (receiverPackage == null) {
                    log("skip null receiverPackage");
                    return;
                }

                if (config.whiteListForce.contains(receiverPackage) ||
                        config.whiteListConf.contains(receiverPackage))
                    return;

                ArrayList<?> receiverList = (broadcastFilter == null) ? null : (ArrayList<?>) XposedHelpers.getObjectField(broadcastFilter, Enum.Field.receiverList);
                String callerPackage = (String) XposedHelpers.getObjectField(args[0], Enum.Field.callerPackage);

                if (receiverList == null) {
                    log("broadcast of [" + callerPackage + "] to [" + receiverPackage + "] receiverList == null");
                    return;
                }

                Object processRecord = (receiverList == null) ? null : XposedHelpers.getObjectField(receiverList, Enum.Field.app);
                ApplicationInfo appInfo = (processRecord == null) ? null : (ApplicationInfo) XposedHelpers.getObjectField(processRecord, Enum.Field.info);

                // TODO 很多 receiverList[instantiate] processRecord[null] applicationInfo[null]
                if (processRecord == null || appInfo == null) {
                    XposedHelpers.setObjectField(receiverList, Enum.Field.app, null);
                    return;
                }

                boolean isSystem;
                if (config.thirdApp.size() > 3) {
                    isSystem = !config.thirdApp.contains(receiverPackage);
                } else
                    isSystem = (appInfo.flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0;

                if (isSystem) return;

                // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
                Object activityManagerService = XposedHelpers.getObjectField(receiverList, Enum.Field.owner);

                Class<?> clazz = activityManagerService.getClass();
                Method method = null;

                while (clazz != null) {
                    method = XposedHelpers.findMethodExactIfExists(clazz, Enum.Method.isAppForeground, int.class);
                    if (method != null) {
                        break;
                    } else {
                        clazz = clazz.getSuperclass();
                    }
                }

                int uid = XposedHelpers.getIntField(receiverList, Enum.Field.uid);
                boolean isAppForeground;
                try {
                    isAppForeground = (method != null) &&
                            (boolean) XposedHelpers.findMethodBestMatch(clazz, Enum.Method.isAppForeground, uid).invoke(activityManagerService, uid);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log("find isAppForeground failed");
                    isAppForeground = false;
                }

                if (isAppForeground) {
                    log("skip foreground:" + receiverPackage);
                    return;
                }

                if (config.isEnableDynamicWhiteList() && config.whiteListDynamic.contains(receiverPackage) &&
                        soundDevices.playCount > 0) {
                    return;
                }

                // 以下则是黑名单，且在后台的应用的广播
                // TODO 判断部分广播，放行并临时后台唤醒
                boolean need2wakeup = false;
                for (Object BroadcastFilter : receiverList) {
                    ArrayList<?> mActions = (ArrayList<?>) XposedHelpers.getObjectField(BroadcastFilter, "mActions");
                    if (mActions == null) {
                        log("mActions == null");
                    } else {
//                        for (Object str : mActions){
//                            if(whiteListAction.contains((String)str)){
//                                need2wakeup = true;
//                                break;
//                            }
//                        }

                        StringBuilder ss = new StringBuilder("[");
                        for (Object str : mActions)
                            ss.append((String) str).append("] [");
                        ss.append(']');
                        log(receiverPackage+" mActions:" + ss.toString());
                    }

                    if(need2wakeup)
                        break;
                }

                if (need2wakeup) {
                    new Thread(() -> Utils.freezeitTask(Utils.discharged, receiverPackage.getBytes(StandardCharsets.UTF_8),null)).start();
                } else {
                    // Clear receiverList
                    // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ReceiverList.java
                    XposedHelpers.setObjectField(receiverList, Enum.Field.app, null);
                    log("Clear broadcast of [" + callerPackage + "] to [" + receiverPackage + "]");
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Enum.Class.BroadcastQueue, lpParam.classLoader, Enum.Method.deliverToRegisteredReceiverLocked,
                    Enum.Class.BroadcastRecord,
                    Enum.Class.BroadcastFilter, boolean.class, int.class, deliverToRegisteredReceiverLockedHook);
            log("hook BroadcastQueue: deliverToRegisteredReceiverLocked success");
        } catch (Exception e) {
            log("hook BroadcastQueue fail:" + e);
        }


        /* processCurBroadcastLocked 处理静态注册的BroadCastReceiver
         * SDK26 ~ SDK33 (Android 8.0-13/O-T) BroadcastQueue.java : processCurBroadcastLocked()
         * SourceCode frameworks/base/services/core/java//com/android/server/am/BroadcastQueue.java
         * link https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/BroadcastQueue.java;l=298
         * Param private final void processCurBroadcastLocked(BroadcastRecord r, ProcessRecord app)
         */
        XC_MethodHook processCurBroadcastLockedHook = new XC_MethodHook() {
            public void beforeHookedMethod(MethodHookParam param) {
                Object[] args = param.args;

                // TODO 静态广播
                String callerPackage = (String) XposedHelpers.getObjectField(args[0], Enum.Field.callerPackage);
                Object processRecord = args[1];
                ApplicationInfo appInfo = (ApplicationInfo) XposedHelpers.getObjectField(args[1], Enum.Field.info);

                if (processRecord == null || appInfo == null) {
//                    XposedHelpers.setObjectField(receiverList, Enum.Field.app, null);

//                    String ss = receiverList.size() + ":1" +
//                            (processRecord == null ? "0" : "1") +
//                            (appInfo == null ? "0" : "1");
//                    log("Clear broadcast of [" + callerPackage + "] to [" + receiverPackage + "]: size:" + ss);
                    return;
                }
            }
        };

//        try {
//            XposedHelpers.findAndHookMethod(Enum.Class.BroadcastQueue, lpParam.classLoader, Enum.Method.processCurBroadcastLocked,
//                    Enum.Class.BroadcastRecord, Enum.Class.ProcessRecord, processCurBroadcastLockedHook);
//        log("Freezeit hook BroadcastQueue: processCurBroadcastLocked success");
//        } catch (Exception e) {
//            log("Freezeit hook BroadcastQueue fail:" + e);
//        }
    }

    void log(String str) {
        if (LOG2UDP) Utils.freezeitUDP_Log(str.getBytes(StandardCharsets.UTF_8));
        else XposedBridge.log(TAG + str);
    }
}
