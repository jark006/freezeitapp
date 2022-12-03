package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.os.Build;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.io.IOException;
import java.util.Arrays;

public class AMSHook {
    final static String TAG = "Freezeit[AMSHook]:";
    Config config;

    Object mProcessList = null;
    ArrayList<?> mLruProcesses = null;

//    ServerThread serverThread = new ServerThread();

//    LocalSocketServer serverThread = new LocalSocketServer();

    // SDK30 A11_R48 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=11240
    // ActivityManagerService: boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK29 A10_R47 https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=10477
    // ActivityManagerService: void dumpLruLocked(PrintWriter pw, String dumpPackage)
    public AMSHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;

        try {
            XposedHelpers.findAndHookConstructor(Enum.Class.ActivityManagerService, lpParam.classLoader,
                    Context.class, Enum.Class.ActivityTaskManagerService, AMSHookMethod);
            log(TAG + "hook AMSHook success");
        } catch (Exception e) {
            log(TAG + "hook AMSHook fail:" + e);
        }
    }

    XC_MethodHook AMSHookMethod = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            log(TAG + "啦啦啦 AMSHookMethod Constructor !!!");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mProcessList = param.thisObject;
                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                log(TAG + "Init SDK31+ mProcessList mLruProcesses");
            } else {
                mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                log(TAG + "Init SDK29-30 mProcessList mLruProcesses");
            }

//            serverThread.start();
        }
    };

//    static class ServerThread extends Thread{
//        @Override
//        public void run(){
//
//            log(TAG + "receiveRunning Init...");
//
//        }
//    }





}
