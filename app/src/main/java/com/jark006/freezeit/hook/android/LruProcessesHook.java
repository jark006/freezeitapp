package com.jark006.freezeit.hook.android;

import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.io.PrintWriter;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LruProcessesHook {
    final static String TAG = "Freezeit[ProcessListHook]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

    // SDK31+ A12 https://cs.android.com/android/platform/superproject/+/android-12.1.0_r27:frameworks/base/services/core/java/com/android/server/am/ProcessList.java;drc=980f233d2d53512457583df7511e65a2a63269dd;l=4018
    // boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK30 A11- https://cs.android.com/android/platform/superproject/+/android-10.0.0_r41:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;drc=321b255d93d5950a48add7f92cffa40c8edd4a8a;l=10461
    // void dumpLruLocked(PrintWriter pw, String dumpPackage)
    public LruProcessesHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                XposedHelpers.findAndHookMethod(Enum.Class.ProcessList, lpParam.classLoader, Enum.Method.dumpLruLocked,
                        PrintWriter.class, String.class, String.class, dumpLruLockedReplacementS);
            }else{
                XposedHelpers.findAndHookMethod(Enum.Class.ActivityManagerService, lpParam.classLoader, Enum.Method.dumpLruLocked,
                        PrintWriter.class, String.class, String.class, dumpLruLockedReplacementR);
            }
            log("hook ProcessList success");
        } catch (Exception e) {
            log("hook ProcessList fail:" + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    // SDK31+
    XC_MethodReplacement dumpLruLockedReplacementS = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {

            // private final ArrayList<ProcessRecord> mLruProcesses = new ArrayList<ProcessRecord>();
            ArrayList<?> mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mLruProcesses);
            int mLruProcessActivityStart = (int) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mLruProcessActivityStart);
            final int lruSize = mLruProcesses.size();
            for (int i = lruSize - 1; i >= mLruProcessActivityStart; i--) {
                final Object r = mLruProcesses.get(i); //ProcessRecord

                int uid = (int) XposedHelpers.getObjectField(r, "uid");

                if(!config.thirdApp.contains(uid))continue;
                if(config.whitelist.contains(uid))continue;

                Object mState = XposedHelpers.getObjectField(r, "mState");
                int mCurProcState = (int) XposedHelpers.getObjectField(mState, "mCurProcState");
                PrintWriter pw = (PrintWriter) param.args[0];
                pw.print(uid);
                pw.print(' ');
                pw.println(mCurProcState);
            }
            return true;
        }
    };

    // SDK30-
    XC_MethodReplacement dumpLruLockedReplacementR = new XC_MethodReplacement() {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) {
            Object mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);

            ArrayList<?> mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
            int mLruProcessActivityStart = (int) XposedHelpers.getObjectField(param.thisObject, Enum.Field.mLruProcessActivityStart);
            final int lruSize = mLruProcesses.size();
            for (int i = lruSize - 1; i >= mLruProcessActivityStart; i--) {
                final Object r = mLruProcesses.get(i); //ProcessRecord

                int uid = (int) XposedHelpers.getObjectField(r, "uid");
                Object mState = XposedHelpers.getObjectField(r, "mState");
                int mCurProcState = (int) XposedHelpers.getObjectField(mState, "mCurProcState");
                PrintWriter pw = (PrintWriter) param.args[0];
                pw.print(uid);
                pw.print(' ');
                pw.println(mCurProcState);
            }
            return true;
        }
    };
}
