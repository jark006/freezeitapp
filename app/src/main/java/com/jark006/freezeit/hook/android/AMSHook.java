package com.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.jark006.freezeit.hook.Config;
import com.jark006.freezeit.hook.Enum;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AMSHook {
    final static String TAG = "Freezeit[AMSHook]:";
    Config config;

    Object mProcessList = null;
    ArrayList<?> mLruProcesses = null; //TODO 线程安全

    LocalSocketServer serverThread = new LocalSocketServer();

    // SDK30 A11_R48 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r48:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=11240
    // ActivityManagerService: boolean dumpLruLocked(PrintWriter pw, String dumpPackage, String prefix)

    // SDK29 A10_R47 https://cs.android.com/android/platform/superproject/+/android-10.0.0_r47:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=10477
    // ActivityManagerService: void dumpLruLocked(PrintWriter pw, String dumpPackage)
    public AMSHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;

        try {
            XposedHelpers.findAndHookConstructor(Enum.Class.ActivityManagerService, lpParam.classLoader,
                    Context.class, Enum.Class.ActivityTaskManagerService, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            log(TAG + "Hook Constructor");

                            mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
                            mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                            log(TAG + "Init mProcessList mLruProcesses");

                            serverThread.start();
                        }
                    });
            log(TAG + "hook AMSHook success");
        } catch (Exception e) {
            log(TAG + "hook AMSHook fail:" + e);
        }
    }

    // 采用问询制
    public class LocalSocketServer extends Thread {
        byte[] topBytes = new byte[32 << 2]; // 32*4 bytes

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                LocalServerSocket mSocketServer = new LocalServerSocket("JARK006TOP");
                while (true) {
                    LocalSocket client = mSocketServer.accept();//堵塞,等待客户端
                    if (client == null) continue;

                    config.top.clear();
                    int mLruProcessActivityStart = XposedHelpers.getIntField(mProcessList, Enum.Field.mLruProcessActivityStart);

                    // TODO: mLruProcesses 线程安全?
                    for (int i = mLruProcesses.size() - 1; i >= mLruProcessActivityStart; i--) {
                        Object processRecord = mLruProcesses.get(i);
                        int uid = XposedHelpers.getIntField(processRecord, "uid");
                        if (!config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                            continue;

                        Object mState = XposedHelpers.getObjectField(processRecord, "mState");
                        int mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");

                        // 在顶层 或 绑定了顶层应用 或 有前台服务的宽松前台
                        // ProcessStateEnum: https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/ProcessStateEnum.java;l=10
                        if (mCurProcState <= 3 || (mCurProcState <= 6 && config.tolerant.contains(uid)))
                            config.top.add(uid);
                    }

                    // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
                    int byteLen = 4;
                    for (int uid : config.top) {
                        topBytes[byteLen++] = (byte) uid;
                        topBytes[byteLen++] = (byte) (uid >> 8);
                        topBytes[byteLen++] = (byte) (uid >> 16);
                        topBytes[byteLen++] = (byte) (uid >> 24);
                    }

                    // 头4字节 放置长度
                    int UidLen = (byteLen / 4) - 1; // config.top.size()
                    topBytes[0] = (byte) UidLen;
                    topBytes[1] = (byte) (UidLen >> 8);
                    topBytes[2] = (byte) (UidLen >> 16);
                    topBytes[3] = (byte) (UidLen >> 24);

                    client.getOutputStream().write(topBytes, 0, byteLen);
//                    client.shutdownOutput(); // 不需要
                    client.close(); // 不考虑长连接，用完就关
                }
            } catch (Exception e) {
                log(TAG + e);
            }
        }
    }


}
