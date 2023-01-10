package io.github.jark006.freezeit.hook.android;

import static io.github.jark006.freezeit.hook.XpUtils.log;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class AndroidService {
    final static String TAG = "Freezeit[AndroidService]:";

    final static String CFG_TAG = "Freezeit[CFG]:";
    final static String FGD_TAG = "Freezeit[FGD]:";

    final static String AMS_TAG = "Freezeit[AMS]:";
    final static String WIN_TAG = "Freezeit[WIN]:";
    final static String NMS_TAG = "Freezeit[NMS]:";
    final static String WAK_TAG = "Freezeit[WAK]:";
    final static String DPC_TAG = "Freezeit[DPC]:";

    final int REPLY_SUCCESS = 2;
    final int REPLY_FAILURE = 0;

    Config config;
    ClassLoader classLoader;

    ArrayList<?> mLruProcesses;

    int mScreenState = 0;

    Object appOps;
    Method setUidModeMethod;

    // 实为 WindowManagerService 的 mRoot
    // 引用到 ActivityTaskManagerService 的 mRootWindowContainer
    Object mRootWindowContainer;
    //A12+ https://cs.android.com/android/platform/superproject/+/android-mainline-12.0.0_r100:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java;drc=8b7e0c52e883475bd78a4bd1e0ad05c2f3941704;l=2522
    Method getAllRootTaskInfosMethod; // ArrayList<RootTaskInfo> getAllRootTaskInfos(int displayId) {}

    //A11 https://cs.android.com/android/platform/superproject/+/android-mainline-11.0.0_r19:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java;drc=35cd1a6a87f9f8000f6167da9432a2a1132d29c5;l=2501
    Method getAllStackInfosMethod;    // ArrayList<ActivityManager.StackInfo> getAllStackInfos(int displayId){}

    Object mNetdService;
    Class<?> UidRangeParcelClazz;

    LocalSocketServer serverThread = new LocalSocketServer();

    public AndroidService(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.classLoader = lpParam.classLoader;

        // A10-13 ActivityManagerService
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
        XpUtils.hookConstructor(AMS_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Object mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                log(AMS_TAG, "Init mProcessList mLruProcesses");
            }
        }, Enum.Class.ActivityManagerService, Context.class, Enum.Class.ActivityTaskManagerService);

        // A13 RootWindowContainer
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java

        // A12-A13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getAllRootTaskInfosMethod = XposedHelpers.findMethodExactIfExists(
                    Enum.Class.RootWindowContainer, classLoader, Enum.Method.getAllRootTaskInfos, int.class);
            log(WIN_TAG, "Init getAllRootTaskInfosMethod " + ((getAllRootTaskInfosMethod == null ? "fail" : "success")));
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            getAllStackInfosMethod = XposedHelpers.findMethodExactIfExists(
                    Enum.Class.RootWindowContainer, classLoader, Enum.Method.getAllStackInfos, int.class);
            log(WIN_TAG, "Init getAllStackInfosMethod " + ((getAllStackInfosMethod == null ? "fail" : "success")));
        }

        XpUtils.hookConstructor(WIN_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                mRootWindowContainer = param.thisObject;
                log(WIN_TAG, "Init RootWindowContainer");
            }
        }, Enum.Class.RootWindowContainer, Enum.Class.WindowManagerService);

        // A10-A13 NetworkManagementService
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/NetworkManagementService.java
        UidRangeParcelClazz = XposedHelpers.findClassIfExists(Enum.Class.UidRangeParcel, classLoader);
        log(NMS_TAG, "Init UidRangeParcel " + ((UidRangeParcelClazz == null ? "fail" : "success")));
        XpUtils.hookMethod(NMS_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                mNetdService = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mNetdService);
                log(NMS_TAG, "Init mNetdService " + ((mNetdService == null ? "fail" : "success")));
            }
        }, Enum.Class.NetworkManagementService, Enum.Method.connectNativeNetdService);

        // A11-13 AppOpsService
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java;l=1776
        setUidModeMethod = XposedHelpers.findMethodExactIfExists(
                Enum.Class.AppOpsService, classLoader, Enum.Method.setUidMode,
                int.class, int.class, int.class);
        log(WAK_TAG, "Init setUidModeMethod " + ((setUidModeMethod == null ? "fail" : "success")));
        XC_MethodHook AppOpsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                appOps = param.thisObject;
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XpUtils.hookConstructor(WAK_TAG, classLoader, AppOpsHook, Enum.Class.AppOpsService,
                    File.class, Handler.class, Context.class);
        } else { // A10
            XpUtils.hookConstructor(WAK_TAG, classLoader, AppOpsHook, Enum.Class.AppOpsService,
                    File.class, Handler.class);
        }

        // A10-A13 DisplayPowerController
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/DisplayPowerController.java;l=1927
        XpUtils.hookMethod(DPC_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                mScreenState = (int) param.args[0];
            }
        }, Enum.Class.DisplayPowerController, Enum.Method.setScreenState, int.class, boolean.class);

        serverThread.start();
    }

    class LocalSocketServer extends Thread {
        // 冻它命令识别码, 1359322925 是字符串"Freezeit"的10进制CRC32值
        final int baseCode = 1359322925;
        final int GET_FOREGROUND = baseCode + 1;
        final int GET_SCREEN_STATE = baseCode + 2;
        final int GET_CACHE_EMPTY = baseCode + 3;
        final int SET_CONFIG = baseCode + 20;
        final int SET_WAKEUP_LOCK = baseCode + 21; // 设置唤醒锁权限，针对[宽松]应用，[严格]应用已禁止申请唤醒锁
        final int BREAK_NETWORK = baseCode + 41;

        // 有效命令集
        final Set<Integer> requestCodeSet = Set.of(
                GET_FOREGROUND,
                GET_SCREEN_STATE,
                GET_CACHE_EMPTY,
                SET_CONFIG,
                SET_WAKEUP_LOCK,
                BREAK_NETWORK
        );

        byte[] buff = new byte[16 * 1024];// 16Kib

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                var mSocketServer = new LocalServerSocket("FreezeitXposedServer");
                while (true) {
                    var client = mSocketServer.accept();//堵塞,单线程处理
                    if (client == null) continue;

                    client.setSoTimeout(100);
                    var is = client.getInputStream();

                    int recvLen = is.read(buff, 0, 8);
                    if (recvLen != 8) {
                        log(TAG, "非法连接 接收长度 " + recvLen);
                        is.close();
                        client.close();
                        continue;
                    }

                    // 前4字节是请求码，后4字节是附加数据长度
                    int requestCode = Utils.Byte2Int(buff, 0);
                    if (!requestCodeSet.contains(requestCode)) {
                        log(TAG, "非法请求码 " + requestCode);
                        is.close();
                        client.close();
                        continue;
                    }

                    int payloadLen = Utils.Byte2Int(buff, 4);
                    if (payloadLen > 0) {
                        if (buff.length <= payloadLen) {
                            log(TAG, "数据量超过承载范围 " + payloadLen);
                            is.close();
                            client.close();
                            continue;
                        }

                        int readCnt = 0;
                        while (readCnt < payloadLen) { //欲求不满
                            int cnt = is.read(buff, readCnt, payloadLen - readCnt);
                            if (cnt < 0) {
                                Log.e(TAG, "接收完毕或错误 " + cnt);
                                break;
                            }
                            readCnt += cnt;
                        }
                        if (payloadLen != readCnt) {
                            log(TAG, "接收错误 payloadLen" + payloadLen + " readCnt" + readCnt);
                            is.close();
                            client.close();
                            continue;
                        }
                    }

                    var os = client.getOutputStream();
                    switch (requestCode) {
                        case GET_FOREGROUND:
                            handleForeground(os, buff);
                            break;
                        case GET_CACHE_EMPTY:
                            handleCacheEmpty(os, buff);
                            break;
                        case GET_SCREEN_STATE:
                            handleScreen(os, buff);
                            break;
                        case SET_CONFIG:
                            handleConfig(os, buff, payloadLen);
                            break;
                        case SET_WAKEUP_LOCK:
                            handleWakeupLock(os, buff, payloadLen);
                            break;
                        case BREAK_NETWORK:
                            handleBreakNetwork(os, buff, payloadLen);
                            break;
                        default:
                            log(TAG, "请求码功能暂未实现TODO: " + requestCode);
                            break;
                    }
                    client.close();
                }
            } catch (Exception e) {
                log(TAG, e.toString());
            }
        }
    }


    void handleForeground(OutputStream os, byte[] replyBuff) throws Exception {
        config.top.clear();
        try {
            for (int i = mLruProcesses.size() - 1; i > 10; i--) { //逆序, 最近活跃应用在最后
                var processRecord = mLruProcesses.get(i);
                if (processRecord == null) continue;

                int uid = XposedHelpers.getIntField(processRecord, "uid");
                if (uid < 10000 || !config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                    continue;

                int mCurProcState;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mState = XposedHelpers.getObjectField(processRecord, "mState");
                    if (mState == null) continue;
                    mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");
                } else {
                    mCurProcState = XposedHelpers.getIntField(processRecord, "mCurProcState");
                }

                // 2在顶层 3绑定了顶层应用, 有前台服务:4常驻状态栏 6悬浮窗
                // ProcessStateEnum: https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/ProcessStateEnum.java;l=10
                if (mCurProcState == 2 || mCurProcState == 3 ||
                        (4 <= mCurProcState && mCurProcState <= 6 && config.tolerant.contains(uid)))
                    config.top.add(uid);
            }

            if (config.isExtendFg()) {
                // 某些系统(COS11/12)有特殊白名单，会把QQ或某些游戏的 mCurProcState 设为 0(系统常驻进程专有状态)，无论该应用在前/后台
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (getAllRootTaskInfosMethod != null && mRootWindowContainer != null) {
                        List<?> tasks = (List<?>) getAllRootTaskInfosMethod.invoke(mRootWindowContainer, -1);
                        if (tasks != null) {
                            for (Object info : tasks) { // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityTaskManager.java;l=503
                                boolean visible = XposedHelpers.getBooleanField(info, "visible");
                                if (!visible) continue;

                                Object topActivityInfo = XposedHelpers.getObjectField(info, "topActivityInfo");
                                ApplicationInfo applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(topActivityInfo, "applicationInfo");
                                int uid = applicationInfo.uid;
                                if (uid < 10000 || !config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                                    continue;
                                config.top.add(uid);
                            }
                        }
                    }
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    if (getAllStackInfosMethod != null && mRootWindowContainer != null) {
                        //https://cs.android.com/android/platform/superproject/+/android-mainline-11.0.0_r13:frameworks/base/core/java/android/app/ActivityManager.java;drc=ee8c25823de9cd16c91210ab2d13a3ec5b3b64b7;l=2816
                        List<?> tasks = (List<?>) getAllStackInfosMethod.invoke(mRootWindowContainer, -1);
                        if (tasks != null) {
                            for (Object info : tasks) {
                                boolean visible = XposedHelpers.getBooleanField(info, "visible");
                                if (!visible) continue;

                                ComponentName topActivity = (ComponentName) XposedHelpers.getObjectField(info, "topActivity");
                                Integer uid = config.uidIndex.get(topActivity.getPackageName());
                                if (uid == null || uid < 10000 || !config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                                    continue;
                                config.top.add(uid);
                            }
                        }
                    }
                } // TODO Android 10 暂时不搞
            }

        } catch (Exception e) {
            log(FGD_TAG, "前台服务错误:" + e);
        }

        // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
        Utils.Int2Byte(config.top.size(), replyBuff, 0);
        Utils.HashSet2Byte(config.top, replyBuff, 4);

        os.write(replyBuff, 0, (config.top.size() + 1) * 4);
        os.close();
    }


    void handleCacheEmpty(OutputStream os, byte[] replyBuff) throws Exception {
        config.cacheEmptyPid.clear();
        try {
            for (int i = mLruProcesses.size() - 1; i > 10; i--) { //逆序, 最近活跃应用在最后
                var processRecord = mLruProcesses.get(i);
                if (processRecord == null) continue;

                int uid = XposedHelpers.getIntField(processRecord, "uid");
                if (uid < 10000 || !config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                    continue;

                int mCurProcState;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mState = XposedHelpers.getObjectField(processRecord, "mState");
                    if (mState == null) continue;
                    mCurProcState = XposedHelpers.getIntField(mState, "mCurProcState");
                } else {
                    mCurProcState = XposedHelpers.getIntField(processRecord, "mCurProcState");
                }

                // 2在顶层 3绑定了顶层应用, 有前台服务:4常驻状态栏 6悬浮窗
                // ProcessStateEnum: https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/ProcessStateEnum.java;l=10
                if (mCurProcState == 19) {
                    String processName = (String) XposedHelpers.getObjectField(processRecord, "processName");
                    if (processName != null && processName.contains(":")) {
                        int mPid = XposedHelpers.getIntField(processRecord, "mPid");
                        config.cacheEmptyPid.add(mPid);
                    }
                }
            }

        } catch (Exception e) {
            log(FGD_TAG, "缓存进程服务错误:" + e);
        }

        // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
        Utils.Int2Byte(config.cacheEmptyPid.size(), replyBuff, 0);
        Utils.HashSet2Byte(config.cacheEmptyPid, replyBuff, 4);

        os.write(replyBuff, 0, (config.cacheEmptyPid.size() + 1) * 4);
        os.close();
    }


    // 0未知 1息屏 2亮屏 3Doze...
    void handleScreen(OutputStream os, byte[] replyBuff) throws Exception {
/*
        https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/Display.java;drc=fc06fc5cb18df9fa16b7a34bb0a8e6749d2e0bca;l=387
        enum DisplayStateEnum
        public static final int DISPLAY_STATE_UNKNOWN = 0;
        public static final int DISPLAY_STATE_OFF = 1;
        public static final int DISPLAY_STATE_ON = 2;
        public static final int DISPLAY_STATE_DOZE = 3; //亮屏但处于Doze的非交互状态状态
        public static final int DISPLAY_STATE_DOZE_SUSPEND = 4; // 同上，但CPU不控制显示，由协处理器或其他控制
        public static final int DISPLAY_STATE_VR = 5;
        public static final int DISPLAY_STATE_ON_SUSPEND = 6; //非Doze, 类似4
 */
        Utils.Int2Byte(mScreenState, replyBuff, 0);

        os.write(replyBuff, 0, 4);
        os.close();
    }

    /**
     * 总共4行内容 第四行可能为空
     * 第一行：冻它设置数据
     * 第二行：第三方应用UID列表
     * 第三行：自由后台(含内置)UID列表
     * 第四行：宽松前台UID列表 (此行可能为空)
     */
    void handleConfig(OutputStream os, byte[] buff, int recvLen) throws Exception {
        var splitLine = new String(buff, 0, recvLen).split("\n");
        if (splitLine.length < 3 || splitLine.length > 4) {
            log(CFG_TAG, "Fail splitLine.length:" + splitLine.length);
            for (String line : splitLine)
                log(CFG_TAG, "START:" + line);

            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
            os.write(buff, 0, 4);
            os.close();
            return;
        }

        config.thirdApp.clear();
        config.uidIndex.clear();
        config.whitelist.clear();
        config.tolerant.clear();

        try {
            StringBuilder sb = new StringBuilder("Parse: ");
            for (int lineIdx = 0; lineIdx < splitLine.length; lineIdx++) {
                var split = splitLine[lineIdx].split(" ");
                switch (lineIdx) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            config.settings[i] = Integer.parseInt(split[i]);
                        sb.append("settings:").append(split.length).append(" ");
                        break;
                    case 1:
                        for (String s : split) {
                            var spl = s.split("####");
                            if (spl.length == 2) {
                                int uid = Integer.parseInt(spl[0]);
                                config.thirdApp.add(uid);
                                config.uidIndex.put(spl[1], uid);
                            }
                        }
                        sb.append("thirdApp:").append(config.thirdApp.size()).append(" ");
                        sb.append("uidIndex:").append(config.uidIndex.size()).append(" ");
                        break;
                    case 2:
                        for (String s : split)
                            config.whitelist.add(Integer.parseInt(s));
                        sb.append("whitelist:").append(config.whitelist.size()).append(" ");
                        break;
                    case 3:
                        for (String s : split)
                            config.tolerant.add(Integer.parseInt(s));
                        sb.append("tolerant:").append(config.tolerant.size()).append(" ");
                        break;
                }
            }
            log(CFG_TAG, sb.toString());
            Utils.Int2Byte(REPLY_SUCCESS, buff, 0);
        } catch (Exception e) {
            log(CFG_TAG, "Exception: [" + Arrays.toString(splitLine) + "]: \n" + e);
            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
        }

        os.write(buff, 0, 4);
        os.close();
    }


    /**
     * https://cs.android.com/android/platform/superproject/+/android-mainline-10.0.0_r9:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/AppProtoEnums.java;l=84
     * WAKEUP_LOCK CODE is [40] A10-A13
     * public static final String[] MODE_NAMES = new String[] {
     * "allow",        // MODE_ALLOWED
     * "ignore",       // MODE_IGNORED
     * "deny",         // MODE_ERRORED
     * "default",      // MODE_DEFAULT
     * "foreground",   // MODE_FOREGROUND
     * };
     */
    void handleWakeupLock(OutputStream os, byte[] buff, int recvLen) throws Exception {
        final int WAKEUP_LOCK_IGNORE = 1;
        final int WAKEUP_LOCK_DEFAULT = 3;
        final int WAKEUP_LOCK_CODE = 40;

        try {
            if (recvLen != 8) {
                log(WAK_TAG, "非法数据长度" + recvLen);
                throw null;
            }

            int uid = Utils.Byte2Int(buff, 0);
            if (!config.thirdApp.contains(uid)) {
                log(WAK_TAG, "非法UID" + uid);
                throw null;
            }

            int mode = Utils.Byte2Int(buff, 4); // 1:ignore  3:default
            if (mode != WAKEUP_LOCK_IGNORE && mode != WAKEUP_LOCK_DEFAULT) {
                log(WAK_TAG, "非法mode" + mode);
                throw null;
            }
            if (setUidModeMethod == null) {
                log(WAK_TAG, "未初始化 setUidModeMethod");
                throw null;
            }

            setUidModeMethod.invoke(appOps, WAKEUP_LOCK_CODE, uid, mode);
//            XposedHelpers.callMethod(appOps, Enum.Method.setUidMode, WAKEUP_LOCK_CODE, uid, mode);
            Utils.Int2Byte(REPLY_SUCCESS, buff, 0);
        } catch (Exception e) {
            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
        }

        os.write(buff, 0, 4);
        os.close();
    }

    void handleBreakNetwork(OutputStream os, byte[] buff, int recvLen) throws Exception {
        try {
            if (recvLen != 4) {
                log(NMS_TAG, "非法数据长度" + recvLen);
                throw null;
            }

            int uid = Utils.Byte2Int(buff, 0);
            if (!config.thirdApp.contains(uid)) {
                log(NMS_TAG, "非法UID" + uid);
                throw null;
            }
            if (mNetdService == null) {
                log(NMS_TAG, "mNetdService null");
                throw null;
            }
            if (UidRangeParcelClazz == null) {
                log(NMS_TAG, "UidRangeParcelClazz null");
                throw null;
            }

            Object uidRanges = Array.newInstance(UidRangeParcelClazz, 1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Array.set(uidRanges, 0, XposedHelpers.newInstance(UidRangeParcelClazz, uid, uid));
            } else {
                Object uidRangeParcel = XposedHelpers.newInstance(UidRangeParcelClazz);
                XposedHelpers.setIntField(uidRangeParcel, "start", uid);
                XposedHelpers.setIntField(uidRangeParcel, "stop", uid);
                Array.set(uidRanges, 0, uidRangeParcel);
            }
            XposedHelpers.callMethod(mNetdService, Enum.Method.socketDestroy, uidRanges, new int[0]);
            Utils.Int2Byte(REPLY_SUCCESS, buff, 0);
        } catch (Exception e) {
            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
        }

        os.write(buff, 0, 4);
        os.close();
    }
}