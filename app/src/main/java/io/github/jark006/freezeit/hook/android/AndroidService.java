package io.github.jark006.freezeit.hook.android;

import static io.github.jark006.freezeit.hook.XpUtils.log;

import android.content.Context;
import android.net.LocalServerSocket;
import android.os.Build;
import android.os.Handler;

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
import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class AndroidService {
    final static String TAG = "Freezeit[AndroidService]:";

    final static String CFG_TAG = "Freezeit[CFG]:";
    final static String AMS_TAG = "Freezeit[AMS]:";
    final static String WIN_TAG = "Freezeit[WIN]:";
    final static String NMS_TAG = "Freezeit[NMS]:";
    final static String WAK_TAG = "Freezeit[WAK]:";
    final static String DPC_TAG = "Freezeit[DPC]:";

    final int REPLY_SUCCESS = 2;
    final int REPLY_FAILURE = 0;

    Config config;

    ArrayList<?> mLruProcesses;

    int mScreenState = 0;

    Object appOps;
    Method setUidModeMethod;

    // 实为 WindowManagerService 的 mRoot
    // 引用到 ActivityTaskManagerService 的 mRootWindowContainer
    Object mRootWindowContainer;

    //A12+ https://cs.android.com/android/platform/superproject/+/android-mainline-12.0.0_r100:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java;l=2522
    //A11- https://cs.android.com/android/platform/superproject/+/android-mainline-11.0.0_r19:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java;l=2501
    Method windowsStackMethod;

    Object mNetdService;
    Class<?> UidRangeParcelClazz;

    LocalSocketServer serverThread = new LocalSocketServer();

    ClassLoader classLoader;

    public AndroidService(Config config, ClassLoader classLoader) {
        this.config = config;
        this.classLoader = classLoader;

        // A10-13 ActivityManagerService
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
        XpUtils.hookConstructor(AMS_TAG, classLoader, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
//                Object mProcessList = XpUtils.getObject(param.thisObject, Enum.Field.mProcessList);
                Object mProcessList = XposedHelpers.getObjectField(param.thisObject, Enum.Field.mProcessList);
                mLruProcesses = mProcessList == null ? null :
                        (ArrayList<?>) XposedHelpers.getObjectField(mProcessList, Enum.Field.mLruProcesses);
                log(AMS_TAG, "Init mLruProcesses " + ((mLruProcesses == null ? "fail" : "success")));
            }
        }, Enum.Class.ActivityManagerService, Context.class, Enum.Class.ActivityTaskManagerService);

        windowsStackMethod = XposedHelpers.findMethodExactIfExists(
                Enum.Class.RootWindowContainer, classLoader,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                        Enum.Method.getAllRootTaskInfos : Enum.Method.getAllStackInfos, int.class);
        if (windowsStackMethod == null) {
            log(WIN_TAG, "Init windowsStackMethod fail");
        } else {
            windowsStackMethod.setAccessible(true);
            log(WIN_TAG, "Init windowsStackMethod success");
        }

        // A13 RootWindowContainer
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/wm/RootWindowContainer.java
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


        // AppOpsService
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java;l=1776
        setUidModeMethod = XposedHelpers.findMethodExactIfExists(
                Enum.Class.AppOpsService, classLoader, Enum.Method.setUidMode,
                int.class, int.class, int.class);
        log(WAK_TAG, "Init setUidModeMethod " + ((setUidModeMethod == null ? "fail" : "success")));
        XC_MethodHook AppOpsHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                appOps = param.thisObject;
                log(WAK_TAG, "Init appOps " + ((appOps == null ? "fail" : "success")));
            }
        };

        // A11-13
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
        final int SET_CONFIG = baseCode + 20;
        final int SET_WAKEUP_LOCK = baseCode + 21; // 设置唤醒锁权限
        final int BREAK_NETWORK = baseCode + 41;

        // 有效命令集
        final Set<Integer> requestCodeSet = Set.of(
                GET_FOREGROUND,
                GET_SCREEN_STATE,
                SET_CONFIG,
                SET_WAKEUP_LOCK,
                BREAK_NETWORK
        );

        byte[] buff = new byte[64 * 1024];// 64 KiB

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

                    final int recvLen = is.read(buff, 0, 8);
                    if (recvLen != 8) {
                        log(TAG, "非法连接 接收长度 " + recvLen);
                        is.close();
                        client.close();
                        continue;
                    }

                    // 前4字节是请求码，后4字节是附加数据长度
                    final int requestCode = Utils.Byte2Int(buff, 0);
                    if (!requestCodeSet.contains(requestCode)) {
                        log(TAG, "非法请求码 " + requestCode);
                        is.close();
                        client.close();
                        continue;
                    }

                    final int payloadLen = Utils.Byte2Int(buff, 4);
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
                                log(TAG, "接收完毕或错误 " + cnt);
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

    boolean isInitWindows() {
        return windowsStackMethod != null && mRootWindowContainer != null;
    }

    void handleForeground(OutputStream os, byte[] replyBuff) throws Exception {
        config.foregroundUid.clear();
        try {
            for (int i = (mLruProcesses == null || !config.isCurProcStateInitialized()) ?
                    0 : mLruProcesses.size() - 1; i > 10; i--) { //逆序, 最近活跃应用在最后
                var processRecord = mLruProcesses.get(i); // IndexOutOfBoundsException
                if (processRecord == null) continue;

                final int uid = config.getProcessRecordUid(processRecord);// processRecord
                if (!config.managedApp.contains(uid))
                    continue;

                int mCurProcState;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var mState = config.getProcessRecordState(processRecord);
                    if (mState == null) continue;
                    mCurProcState = config.getCurProcState(mState);
                } else {
                    mCurProcState = config.getCurProcState(processRecord);
                }

                // 2在顶层 3绑定了顶层应用, 有前台服务:4常驻状态栏 6悬浮窗
                // ProcessStateEnum: https://cs.android.com/android/platform/superproject/+/master:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/ProcessStateEnum.java;l=10
                if (mCurProcState == 2 || mCurProcState == 3 ||
                        (4 <= mCurProcState && mCurProcState <= 6 && config.tolerant.contains(uid)))
                    config.foregroundUid.add(uid);
            }
        } catch (Exception ignore) {
        }

        // 某些系统(COS11/12)及Thanox的后台保护机制，会把某些应用或游戏的 mCurProcState 设为 0(系统常驻进程专有状态)
        // 此时只能到窗口管理器获取有前台窗口的应用
        if (config.isExtendFg() && isInitWindows()) {
            List<?> rootTaskInfoList;
            try {
                rootTaskInfoList = (List<?>) windowsStackMethod.invoke(mRootWindowContainer, -1);
            } catch (Exception ignore) {
                rootTaskInfoList = null;
            }
            if (rootTaskInfoList != null) {
                // A12 https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/ActivityTaskManager.java;l=503
                // A11 https://cs.android.com/android/platform/superproject/+/android-mainline-11.0.0_r13:frameworks/base/core/java/android/app/ActivityManager.java;l=2816
                for (Object info : rootTaskInfoList) {
                    boolean visible = config.getTaskInfoVisible(info);
                    if (!visible) continue;

                    int uid = -1;
                    var childTaskNames = config.getTaskInfoTaskNames(info);
                    if (childTaskNames != null && childTaskNames.length > 0) {
                        int pkgEndIdx = childTaskNames[0].indexOf('/'); // 只取首个 taskId
                        if (pkgEndIdx > 0) {
                            final String pkg = childTaskNames[0].substring(0, pkgEndIdx);
                            final Integer value = config.uidIndex.get(pkg);
                            if (value != null) uid = value;
                        }
                    }

                    if (config.managedApp.contains(uid)) {
                        config.foregroundUid.add(uid);
//                        log(WIN_TAG, "窗口前台 " + config.pkgIndex.getOrDefault(uid, "" + uid));
                    }
                }
            }
        }

        // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
        int payloadBytes = (config.foregroundUid.size() + 1) * 4;
        Utils.Int2Byte(config.foregroundUid.size(), replyBuff, 0);
        config.foregroundUid.toBytes(replyBuff, 4);

        os.write(replyBuff, 0, payloadBytes);
        os.close();
    }


    // 0未知 1息屏 2亮屏 3Doze...
    void handleScreen(OutputStream os, byte[] replyBuff) throws Exception {
/*
        https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/Display.java;l=387
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
     * 总共 2或3 行内容
     * 第一行：冻它设置数据
     * 第二行：受冻它管控的应用 只含杀死后台和冻结配置， 不含自由后台、白名单
     * 第三行：宽松前台UID列表 只含杀死后台和冻结配置， 不含自由后台、白名单 (此行可能为空)
     */
    void handleConfig(OutputStream os, byte[] buff, int recvLen) throws Exception {
        var splitLine = new String(buff, 0, recvLen).split("\n");
        if (splitLine.length != 2 && splitLine.length != 3) {
            log(CFG_TAG, "Fail splitLine.length:" + splitLine.length);
            for (String line : splitLine)
                log(CFG_TAG, "START:" + line);

            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
            os.write(buff, 0, 4);
            os.close();
            return;
        }

        config.managedApp.clear();
        config.uidIndex.clear();
        config.pkgIndex.clear();
        config.tolerant.clear();

        try {
            StringBuilder tmp = new StringBuilder("Parse:");

            String[] elementList = splitLine[0].split(" ");
            for (int i = 0; i < elementList.length; i++)
                config.settings[i] = Integer.parseInt(elementList[i]);
            tmp.append(" settings:").append(elementList.length);

            elementList = splitLine[1].split(" ");
            for (String element : elementList) {
                if (element.length() <= 5)
                    continue;
                // element: "10xxxpackName"
                final int uid = Integer.parseInt(element.substring(0, 5));
                final String packName = element.substring(5);
                config.managedApp.add(uid);
                config.uidIndex.put(packName, uid);
                config.pkgIndex.put(uid, packName);
            }
            tmp.append(" managedApp:").append(config.managedApp.size());
            tmp.append(" uidIndex:").append(config.uidIndex.size());
            tmp.append(" pkgIndex:").append(config.pkgIndex.size());
            config.pkgIndex.put(1000, "AndroidSystem");
            config.pkgIndex.put(-1, "Unknown");

            if (splitLine.length == 3) {
                elementList = splitLine[2].split(" ");
                for (String uidStr : elementList)
                    config.tolerant.add(Integer.parseInt(uidStr));
            }
            tmp.append(" tolerant:").append(config.tolerant.size());

            log(CFG_TAG, tmp.toString());
            Utils.Int2Byte(REPLY_SUCCESS, buff, 0);
        } catch (Exception e) {
            log(CFG_TAG, "Exception: [" + Arrays.toString(splitLine) + "]: \n" + e);
            Utils.Int2Byte(REPLY_FAILURE, buff, 0);
        }

        os.write(buff, 0, 4);
        os.close();

        if (!config.initField) {
            config.Init(classLoader);
            log("Freezeit[InitField]:", config.Init(classLoader));
        }
    }


    /**
     * <a href="https://cs.android.com/android/platform/superproject/+/android-mainline-10.0.0_r9:out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/xref35/srcjars.xref/android/app/AppProtoEnums.java;l=84">...</a>
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
            if (setUidModeMethod == null || appOps == null) {
                log(WAK_TAG, "未初始化 setUidModeMethod appOps");
                throw null;
            }

            if (recvLen <= 8 || recvLen % 4 != 0) {
                log(WAK_TAG, "非法recvLen " + recvLen);
                throw null;
            }

            final int uidLen = Utils.Byte2Int(buff, 0);
            if (recvLen != (uidLen + 2) * 4) {
                log(WAK_TAG, "非法recvLen " + recvLen + " uidLen " + uidLen);
                throw null;
            }

            final int mode = Utils.Byte2Int(buff, 4); // 1:ignore  3:default
            if (mode != WAKEUP_LOCK_IGNORE && mode != WAKEUP_LOCK_DEFAULT) {
                log(WAK_TAG, "非法mode:" + mode);
                throw null;
            }

            int[] uidList = new int[uidLen];
            Utils.Byte2Int(buff, 8, recvLen - 8, uidList, 0);
            for (int uid : uidList) {
                if (config.managedApp.contains(uid))
                    setUidModeMethod.invoke(appOps, WAKEUP_LOCK_CODE, uid, mode);
                else
                    log(WAK_TAG, "非法UID:" + uid);
            }

            if (XpUtils.DEBUG_WAKEUP_LOCK) {
                var tmp = new StringBuilder(mode == WAKEUP_LOCK_IGNORE ? "禁止 WakeLock: " : "恢复 WakeLock: ");
                for (int uid : uidList)
                    tmp.append(config.pkgIndex.getOrDefault(uid, String.valueOf(uid))).append(", ");
                XpUtils.log(TAG, tmp.toString());
            }

            if (mode == WAKEUP_LOCK_IGNORE) // 此操作会在息屏超时后触发
                config.foregroundUid.clear();

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

            final int uid = Utils.Byte2Int(buff, 0);
            if (!config.managedApp.contains(uid)) {
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