package com.jark006.freezeit.hook;

public class Enum {

    public final static class Class{
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

        public final static String MilletConfig = "com.miui.powerkeeper.millet.MilletConfig";
        public final static String PowerStateMachine = "com.miui.powerkeeper.statemachine.PowerStateMachine";
        public final static String SleepModeControllerNew = "com.miui.powerkeeper.statemachine.SleepModeControllerNew";

        public final static String ActivityManagerService = "com.android.server.am.ActivityManagerService";
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
        public final static String clearApp = "clearApp";
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
    }

}