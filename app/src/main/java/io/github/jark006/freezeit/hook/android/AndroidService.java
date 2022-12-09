package io.github.jark006.freezeit.hook.android;

import static de.robv.android.xposed.XposedBridge.log;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import io.github.jark006.freezeit.Utils;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AndroidService {
    final static String TAG = "Freezeit[AndroidService]:";
    final int REPLY_POSITIVE = 2;
    final int REPLY_NEGATIVE = 1;
    final int REPLY_FAILURE = 0;

    Config config;

    Object mProcessList = null;
    ArrayList<?> mLruProcesses = null;

    LocalSocketServer serverThread = new LocalSocketServer();
    Object powerManager;

    // mainline https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
    public AndroidService(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;

        try {
            XposedHelpers.findAndHookConstructor(Enum.Class.ActivityManagerService, lpParam.classLoader,
                    Context.class, Enum.Class.ActivityTaskManagerService, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
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

        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/os/PowerManager.java;l=1730
        try {
            XposedHelpers.findAndHookConstructor(Enum.Class.PowerManager, lpParam.classLoader,
                    Context.class, Enum.Class.IPowerManager, Enum.Class.IThermalService, Handler.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            powerManager = param.thisObject;
                            log(TAG + "Init powerManager");
                            // TODO 有多次日志记录，不止一个实例， 4个？
                            // TODO Android 10-13 兼容性 待测
                        }
                    });
            log(TAG + "hook PowerManager success");
        } catch (Exception e) {
            log(TAG + "hook PowerManager fail:" + e);
        }
    }

    public class LocalSocketServer extends Thread {
        // 1359322925 是 "Freezeit" 的10进制CRC32值
        // 冻它命令识别码
        final int GET_TOP = 1359322925 + 1;
        final int GET_SCREEN = 1359322925 + 2;

        final int SET_CONFIG = 1359322925 + 20;
        final int SET_WAKEUP_LOCK = 1359322925 + 21;

        byte[] recvBuff = new byte[4096];
        byte[] replyBuff = new byte[4096];

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                LocalServerSocket mSocketServer = new LocalServerSocket("FreezeitServer");
                while (true) {
                    LocalSocket client = mSocketServer.accept();//堵塞,单线程处理
                    if (client == null) continue;

                    client.setSoTimeout(100);
                    InputStream is = client.getInputStream();

                    int recvLen = is.read(recvBuff, 0, 8);
                    if (recvLen != 8) {
                        log(TAG + "非法连接 接收长度 " + recvLen);
                        is.close();
                        client.close();
                        continue;
                    }

                    int requestCode = Utils.Byte2Int(recvBuff, 0);
                    int payloadLen = Utils.Byte2Int(recvBuff, 4);
                    if(payloadLen>0){
                        int readCnt = 0;
                        while (readCnt < payloadLen) { //欲求不满
                            int cnt = is.read(recvBuff, readCnt, payloadLen - readCnt);
                            if (cnt < 0) {
                                Log.e(TAG, "接收错误 cnt < 0");
                                break;
                            }
                            readCnt += cnt;
                        }
                        if (payloadLen != readCnt) {
                            log(TAG + "接收错误 payloadLen"+payloadLen+" readCnt" + readCnt);
                            is.close();
                            client.close();
                            continue;
                        }
                    }

                    OutputStream os = client.getOutputStream();
                    switch (requestCode) {
                        case GET_TOP:
                            handleTop(os, replyBuff);
                            break;
                        case GET_SCREEN:
                            handleScreen(os, replyBuff);
                            break;
                        case SET_CONFIG:
                            handleConfig(os, recvBuff, payloadLen, replyBuff);
                            break;
                        default:
                            log(TAG + "非法请求码 " + requestCode);
                            break;
                    }
                    client.close();
                }
            } catch (Exception e) {
                log(TAG + e);
            }
        }
    }


    void handleTop(OutputStream os, byte[] replyBuff) throws Exception {
        config.top.clear();
        try {
            synchronized (mLruProcesses) { // TODO 不确定这种上锁能否有效
                for (Object processRecord : mLruProcesses) {
                    if (processRecord == null) continue;

                    int uid = XposedHelpers.getIntField(processRecord, "uid");
                    if (uid < 10000 || !config.thirdApp.contains(uid) || config.whitelist.contains(uid))
                        continue;

                    int mCurProcState;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Object mState = XposedHelpers.getObjectField(processRecord, "mState");
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
            }
        } catch (Exception e) {
            log(TAG + "前台服务错误:" + e);
        }

        // 开头的4字节放置UID的个数，往后每4个字节放一个UID  [小端]
        Utils.Int2Byte(config.top.size(), replyBuff, 0);
        Utils.Hashset2Bytes(config.top, replyBuff, 4);

        os.write(replyBuff, 0, (config.top.size() + 1) * 4);
        os.close();
    }

    // 0获取失败 1息屏 2亮屏
    void handleScreen(OutputStream os, byte[] replyBuff) throws Exception {
        if (powerManager == null) {
            log(TAG + "powerManager 未初始化");
            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
        } else {
            boolean isInteractive = (boolean) XposedHelpers.callMethod(powerManager, Enum.Method.isInteractive);
            Utils.Int2Byte(isInteractive ? REPLY_POSITIVE : REPLY_NEGATIVE, replyBuff, 0);
            log(TAG + (isInteractive ? "交互中" : "息屏中"));
        }
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

        String[] splitLine = new String(recvBuff,0, recvLen).split("\n");
        if (splitLine.length < 3 || splitLine.length > 4) {
            log(TAG + "Fail splitLine.length:" + splitLine.length);
            for (String line : splitLine)
                log(TAG + "START:" + line);

            Utils.Int2Byte(REPLY_FAILURE, replyBuff, 0);
            os.write(replyBuff, 0, 4);
            os.close();
            return;
        }

        config.thirdApp.clear();
        config.whitelist.clear();
        config.tolerant.clear();

        log(TAG + "Start parse, line:"+splitLine.length);
        try {
            for (int lineIdx = 0; lineIdx < splitLine.length; lineIdx++) {
                String[] split = splitLine[lineIdx].split(" ");
                switch (lineIdx) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            config.settings[i] = Integer.parseInt(split[i]);
                        log(TAG + "Parse settings  " + split.length);
                        break;
                    case 1:
                        for (String s : split)
                            if (s.length() == 5) // UID 10XXX 长度是5
                                config.thirdApp.add(Integer.parseInt(s));
                        log(TAG + "Parse thirdApp  " + config.thirdApp.size());
                        break;
                    case 2:
                        for (String s : split)
                            if (s.length() == 5)
                                config.whitelist.add(Integer.parseInt(s));
                        log(TAG + "Parse whitelist " + config.whitelist.size());
                        break;
                    case 3:
                        for (String s : split)
                            if (s.length() == 5)
                                config.tolerant.add(Integer.parseInt(s));
                        log(TAG + "Parse tolerant  " + config.tolerant.size());
                        break;
                }
            }
            log(TAG + "Finish parse");
            Utils.Int2Byte(REPLY_POSITIVE, replyBuff, 0);
        } catch (Exception e) {
            log(TAG + "IOException: [" + Arrays.toString(splitLine) + "]: \n" + e);
            Utils.Int2Byte(REPLY_NEGATIVE, replyBuff, 0);
        }

        os.write(replyBuff, 0, 4);
        os.close();
    }

}