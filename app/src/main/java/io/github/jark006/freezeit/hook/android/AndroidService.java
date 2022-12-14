package io.github.jark006.freezeit.hook.android;

import static io.github.jark006.freezeit.hook.XpUtils.log;

import android.content.Context;
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
    final static String NMS_TAG = "Freezeit[NMS]:";
    final static String OPS_TAG = "Freezeit[OPS]:";
    final static String BSS_TAG = "Freezeit[BSS]:";
    final static String DPC_TAG = "Freezeit[DPC]:";

    final int REPLY_SUCCESS_POSITIVE = 2; // 执行成功，结果积极
    final int REPLY_SUCCESS_NEGATIVE = 1; // 执行成功，结果消极
    final int REPLY_FAILURE = 0;          // 执行失败

    Config config;
    ClassLoader classLoader;

    Object mProcessList;
    ArrayList<?> mLruProcesses;

    //    Object screen_mStats;
    int mScreenState = 0;

    Object appOps;
    Method setUidModeMethod;

    Object mNetdService;
    Class<?> UidRangeParcel;


    LocalSocketServer serverThread = new LocalSocketServer();

    public AndroidService(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.classLoader = lpParam.classLoader;

        // A10-13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
        XpUtils.hookConstructor(AMS_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
                mLruProcesses = (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                log(AMS_TAG, "Init mProcessList mLruProcesses");
            }
        }, Enum.Class.ActivityManagerService, Context.class, Enum.Class.ActivityTaskManagerService);

        // A10-A13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/NetworkManagementService.java
        UidRangeParcel = XposedHelpers.findClassIfExists(Enum.Class.UidRangeParcel, classLoader);
        log(NMS_TAG, "Init UidRangeParcel " + ((UidRangeParcel == null ? "fail" : "success")));
        XpUtils.hookMethod(NMS_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                mNetdService = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mNetdService);
                log(NMS_TAG, "Init mNetdService " + ((mNetdService == null ? "fail" : "success")));
            }
        }, Enum.Class.NetworkManagementService, Enum.Method.connectNativeNetdService);

        // A11-13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java;l=1776
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XpUtils.hookConstructor(OPS_TAG, classLoader, AppOpsHook, Enum.Class.AppOpsService,
                    File.class, Handler.class, Context.class);
        } else { // A10
            XpUtils.hookConstructor(OPS_TAG, classLoader, AppOpsHook, Enum.Class.AppOpsService,
                    File.class, Handler.class);
        }

        // A10-13
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/Display.java;drc=fc06fc5cb18df9fa16b7a34bb0a8e6749d2e0bca;l=387
        // https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex-intdefs/android_common/xref35/srcjars.xref/android/view/ViewProtoEnums.java;l=10
//        XpUtils.hookConstructor(BSS_TAG, classLoader, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
//                screen_mStats = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mStats);
//                log(TAG, "Init BatteryStatsService mStats");
//            }
//        }, Enum.Class.BatteryStatsService, Context.class, File.class, Handler.class);

        // DisplayPowerController.java  setScreenState(int state, boolean reportOnly)
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/display/DisplayPowerController.java;l=1927
        XpUtils.hookMethod(DPC_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) {
                mScreenState = (int) param.args[0];
            }
        }, Enum.Class.DisplayPowerController, Enum.Method.setScreenState, int.class, boolean.class);

        serverThread.start();
    }


    XC_MethodHook AppOpsHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            appOps = param.thisObject;
            setUidModeMethod = XposedHelpers.findMethodExactIfExists(
                    appOps.getClass(), Enum.Method.setUidMode,
                    int.class, int.class, int.class);
            log(NMS_TAG, "Init setUidModeMethod " + ((setUidModeMethod == null ? "fail" : "success")));
        }
    };

    public class LocalSocketServer extends Thread {
        // 冻它命令识别码, 1359322925 是字符串"Freezeit"的10进制CRC32值
        final int GET_FOREGROUND = 1359322925 + 1;
        final int GET_SCREEN_STATE = 1359322925 + 2;

        final int SET_CONFIG = 1359322925 + 20;
        final int SET_WAKEUP_LOCK = 1359322925 + 21; // 设置唤醒锁权限，针对[宽松]应用，[严格]应用已禁止申请唤醒锁

        final int BREAK_NETWORK = 1359322925 + 41; //TODO 给某应用断网，针对已接入推送的QQ/TIM:msf

        // 有效命令集
        final Set<Integer> requestCodeSet = Set.of(
                GET_FOREGROUND,
                GET_SCREEN_STATE,
                SET_CONFIG,
                SET_WAKEUP_LOCK,
                BREAK_NETWORK
        );

        final int buffSize = 16 * 1024; // 16Kib
        byte[] recvBuff = new byte[buffSize];
        byte[] replyBuff = new byte[buffSize];

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                var mSocketServer = new LocalServerSocket("FreezeitServer");
                while (true) {
                    var client = mSocketServer.accept();//堵塞,单线程处理
                    if (client == null) continue;

                    client.setSoTimeout(100);
                    var is = client.getInputStream();

                    int recvLen = is.read(recvBuff, 0, 8);
                    if (recvLen != 8) {
                        log(TAG, "非法连接 接收长度 " + recvLen);
                        is.close();
                        client.close();
                        continue;
                    }

                    // 前4字节是请求码，后4字节是附加数据长度
                    int requestCode = Utils.Byte2Int(recvBuff, 0);
                    if (!requestCodeSet.contains(requestCode)) {
                        log(TAG, "非法请求码 " + requestCode);
                        is.close();
                        client.close();
                        continue;
                    }

                    int payloadLen = Utils.Byte2Int(recvBuff, 4);
                    if (buffSize <= payloadLen) {
                        log(TAG, "数据量超过承载范围 " + payloadLen);
                        is.close();
                        client.close();
                        continue;
                    }
                    if (payloadLen > 0) {
                        int readCnt = 0;
                        while (readCnt < payloadLen) { //欲求不满
                            int cnt = is.read(recvBuff, readCnt, payloadLen - readCnt);
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
                            handleForeground(os, replyBuff);
                            break;
                        case GET_SCREEN_STATE:
                            handleScreen(os, replyBuff);
                            break;
                        case SET_CONFIG:
                            handleConfig(os, recvBuff, payloadLen, replyBuff);
                            break;
                        case SET_WAKEUP_LOCK:
                            handleWakeupLock(os, recvBuff, payloadLen, replyBuff);
                            break;
                        case BREAK_NETWORK:
                            handleBreakNetwork(os, recvBuff, payloadLen, replyBuff);
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
            for (int i = mLruProcesses.size() - 1; i > 0; i--) { //逆序, 最近活跃应用在最后
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

                // 在顶层 或 绑定了顶层应用 或 有前台服务的宽松前台
                // ProcessStateEnum: https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/ProcessStateEnum.java;l=10
                if (mCurProcState <= 3 || (mCurProcState <= 6 && config.tolerant.contains(uid)))
                    config.top.add(uid);
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

    // 0获取失败 1息屏 2亮屏
    void handleScreen(OutputStream os, byte[] replyBuff) throws Exception {
//        if ( == null) {
//            log(TAG, "mStats 未初始化");
//            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
//        } else {
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
//            int mScreenState = XposedHelpers.getIntField(screen_mStats, Enum.Field.mScreenState);
//            boolean isScreenOn = (mScreenState == 2) || (mScreenState == 5) || (mScreenState == 6) || (mScreenState == 0);

        Utils.Int2Byte(mScreenState, replyBuff, 0);

//            if (mScreenState != 1 && mScreenState != 2) // 其他状态打一下日志
//                log(TAG, "mScreenState:" + mScreenState);
//        }

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
    void handleConfig(OutputStream os, byte[] recvBuff, int recvLen, byte[] replyBuff) throws Exception {
        var splitLine = new String(recvBuff, 0, recvLen).split("\n");
        if (splitLine.length < 3 || splitLine.length > 4) {
            log(CFG_TAG, "Fail splitLine.length:" + splitLine.length);
            for (String line : splitLine)
                log(CFG_TAG, "START:" + line);

            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
            os.write(replyBuff, 0, 4);
            os.close();
            return;
        }

        config.thirdApp.clear();
        config.whitelist.clear();
        config.tolerant.clear();

        log(CFG_TAG, "Start parse, line:" + splitLine.length);
        try {
            for (int lineIdx = 0; lineIdx < splitLine.length; lineIdx++) {
                var split = splitLine[lineIdx].split(" ");
                switch (lineIdx) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            config.settings[i] = Integer.parseInt(split[i]);
                        log(CFG_TAG, "Parse settings  " + split.length);
                        break;
                    case 1:
                        for (String s : split)
                            if (s.length() == 5) // UID 10XXX 长度是5
                                config.thirdApp.add(Integer.parseInt(s));
                        log(CFG_TAG, "Parse thirdApp  " + config.thirdApp.size());
                        break;
                    case 2:
                        for (String s : split)
                            if (s.length() == 5)
                                config.whitelist.add(Integer.parseInt(s));
                        log(CFG_TAG, "Parse whitelist " + config.whitelist.size());
                        break;
                    case 3:
                        for (String s : split)
                            if (s.length() == 5)
                                config.tolerant.add(Integer.parseInt(s));
                        log(CFG_TAG, "Parse tolerant  " + config.tolerant.size());
                        break;
                }
            }
            log(CFG_TAG, "Finish parse");
            Utils.Int2Byte(REPLY_SUCCESS_POSITIVE, replyBuff, 0);
        } catch (Exception e) {
            log(CFG_TAG, "IOException: [" + Arrays.toString(splitLine) + "]: \n" + e);
            Utils.Int2Byte(REPLY_SUCCESS_NEGATIVE, replyBuff, 0);
        }

        os.write(replyBuff, 0, 4);
        os.close();
    }


    void handleWakeupLock(OutputStream os, byte[] recvBuff, int recvLen, byte[] replyBuff) throws Exception {
        // https://cs.android.com/android/platform/superproject/+/android-mainline-10.0.0_r9:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/AppProtoEnums.java;l=84
        // WAKEUP_LOCK CODE is [40] A10-A13
        // public static final String[] MODE_NAMES = new String[] {
        //            "allow",        // MODE_ALLOWED
        //            "ignore",       // MODE_IGNORED
        //            "deny",         // MODE_ERRORED
        //            "default",      // MODE_DEFAULT
        //            "foreground",   // MODE_FOREGROUND
        //    };

        final int WAKEUP_LOCK_IGNORE = 1;
        final int WAKEUP_LOCK_DEFAULT = 3;
        final int WAKEUP_LOCK_CODE = 40;

        try {
            if (recvLen != 8) {
                log(OPS_TAG, "非法数据长度" + recvLen);
                throw null;
            }

            int uid = Utils.Byte2Int(recvBuff, 0);
            if (!config.thirdApp.contains(uid)) {
                log(OPS_TAG, "非法UID" + uid);
                throw null;
            }

            int mode = Utils.Byte2Int(recvBuff, 4); // 1:ignore  3:default
            if (mode != WAKEUP_LOCK_IGNORE && mode != WAKEUP_LOCK_DEFAULT) {
                log(OPS_TAG, "非法mode" + mode);
                throw null;
            }
            if (setUidModeMethod == null) {
                log(OPS_TAG, "未初始化 setUidModeMethod");
                throw null;
            }

            setUidModeMethod.invoke(appOps, WAKEUP_LOCK_CODE, uid, mode);
//            XposedHelpers.callMethod(appOps, Enum.Method.setUidMode, WAKEUP_LOCK_CODE, uid, mode);
            Utils.Int2Byte(REPLY_SUCCESS_POSITIVE, replyBuff, 0);
        } catch (Exception e) {
            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
        }

        os.write(replyBuff, 0, 4);
        os.close();
    }

    void handleBreakNetwork(OutputStream os, byte[] recvBuff, int recvLen, byte[] replyBuff) throws Exception {
        int uid = Utils.Byte2Int(recvBuff, 0);
        try {
            if (!config.thirdApp.contains(uid)) {
                log(NMS_TAG, "非法UID" + uid);
                throw null;
            }
            if (mNetdService == null) {
                log(NMS_TAG, "mNetdService null");
                throw null;
            }
            if (UidRangeParcel == null) {
                log(NMS_TAG, "UidRangeParcel null");
                throw null;
            }

            Object uidRanges = Array.newInstance(UidRangeParcel, 1);
            Array.set(uidRanges, 0, XposedHelpers.newInstance(UidRangeParcel, uid, uid));
            XposedHelpers.callMethod(mNetdService, Enum.Method.socketDestroy, uidRanges, new int[0]);
            Utils.Int2Byte(REPLY_SUCCESS_POSITIVE, replyBuff, 0);
        } catch (Exception e) {
            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
        }

        os.write(replyBuff, 0, 4);
        os.close();
    }
}