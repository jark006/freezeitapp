package io.github.jark006.freezeit.hook;

import io.github.jark006.freezeit.BuildConfig;

public class Enum {

    public final static class Package {
        public final static String self = BuildConfig.APPLICATION_ID;
        public final static String powerkeeper = "com.miui.powerkeeper";
        public final static String android = "android";
    }

    public final static class Class {
        public final static String BroadcastQueue = "com.android.server.am.BroadcastQueue";
        public final static String BroadcastRecord = "com.android.server.am.BroadcastRecord";
        public final static String BroadcastFilter = "com.android.server.am.BroadcastFilter";
        public final static String AnrHelper = "com.android.server.am.AnrHelper";
        public final static String ProcessRecord = "com.android.server.am.ProcessRecord";
        public final static String ApplicationInfo = "android.content.pm.ApplicationInfo";
        public final static String WindowProcessController = "com.android.server.wm.WindowProcessController";
        public final static String AnrRecord = "com.android.server.am.AnrHelper$AnrRecord";
        public final static String AppErrors = "com.android.server.am.AppErrors";
        public final static String ActivityRecord = "com.android.server.am.ActivityRecord";
        public final static String ActivityManagerService = "com.android.server.am.ActivityManagerService";
        public final static String ActivityTaskManagerService = "com.android.server.wm.ActivityTaskManagerService";
        public final static String ActiveUids = "com.android.server.am.ActiveUids";
        public final static String ProcessStateRecord = "com.android.server.am.ProcessStateRecord";

        public final static String WakeLock = "android.os.PowerManager$WakeLock";
        public final static String PowerManagerService = "com.android.server.power.PowerManagerService";
        public final static String AlarmManagerServiceR = "com.android.server.AlarmManagerService"; //SDK x ~ 30
        public final static String AlarmManagerServiceS = "com.android.server.alarm.AlarmManagerService"; // SDK 31+

        public final static String MilletConfig = "com.miui.powerkeeper.millet.MilletConfig";
        public final static String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
        public final static String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";
        public final static String DynamicTurboPowerHandler = "com.miui.powerkeeper.statemachine.DynamicTurboPowerHandler";
        public final static String SleepProcessHelper = "com.miui.powerkeeper.statemachine.SleepModeController$SleepProcessHelper";
        public final static String PowerCheckerController = "com.miui.powerkeeper.powerchecker.PowerCheckerController";
        public final static String ProcessManager = "miui.process.ProcessManager";
        public final static String ProcessConfig = "miui.process.ProcessConfig";
        public final static String ProcessList = "com.android.server.am.ProcessList";
    }

    public final static class Method {
        public final static String add = "add";
        public final static String deliverToRegisteredReceiverLocked = "deliverToRegisteredReceiverLocked";
        public final static String appNotResponding = "appNotResponding";
        public final static String startAnrConsumerIfNeeded = "startAnrConsumerIfNeeded";
        public final static String isAppForeground = "isAppForeground";
        public final static String processCurBroadcastLocked = "processCurBroadcastLocked";
        public final static String getEnable = "getEnable";
        public final static String clearAppWhenScreenOffTimeOutInNight = "clearAppWhenScreenOffTimeOutInNight";
        public final static String clearAppWhenScreenOffTimeOut = "clearAppWhenScreenOffTimeOut";
        public final static String clearUnactiveApps = "clearUnactiveApps";
        public final static String clearApp = "clearApp";
        public final static String kill = "kill";
        public final static String acquire = "acquire";
        public final static String acquireWakeLockInternal = "acquireWakeLockInternal";
        public final static String triggerAlarmsLocked = "triggerAlarmsLocked";
        public final static String dumpLruLocked = "dumpLruLocked";
        public final static String autoKillApp = "autoKillApp";
        public final static String killAppsInSleep = "killAppsInSleep";
        public final static String updateLruProcessLocked = "updateLruProcessLocked";
        public final static String updateLruProcessLSP = "updateLruProcessLSP";
        public final static String removeLruProcessLocked = "removeLruProcessLocked";
        public final static String setCurProcState = "setCurProcState";
        public final static String onCleanupApplicationRecordLSP = "onCleanupApplicationRecordLSP";
    }

    public final static class Field {
        public final static String packageName = "packageName";
        public final static String info = "info";
        public final static String callerPackage = "callerPackage";
        public final static String receiverList = "receiverList";
        public final static String app = "app";
        public final static String mAnrRecords = "mAnrRecords";
        public final static String owner = "owner";
        public final static String uid = "uid";
        public final static String callingPid = "callingPid";
        public final static String mPackageName = "mPackageName";
        public final static String mTag = "mTag";
        public final static String mProcessList = "mProcessList";
        public final static String mActiveUids = "mActiveUids";
        public final static String owningUid = "owningUid";
        public final static String callingUid = "callingUid";
        public final static String mLruProcesses = "mLruProcesses";
        public final static String mLruProcessActivityStart = "mLruProcessActivityStart";
    }

}
