package com.jark006.freezeit.hook;

import static de.robv.android.xposed.XposedBridge.log;

import android.os.FileObserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;


public class Config extends FileObserver {
    final static String TAG = "Freezeit[Config]:";

    final static String configFilePath = "/data/system/freezeit.conf";
    final File configFile = new File(configFilePath);

    public int[] settings = new int[1024];
    public Set<Integer> thirdApp = new HashSet<>();
    public Set<Integer> whitelist = new HashSet<>();
    //    public Set<Integer> playingExcept = new HashSet<>();
    public Set<Integer> tolerant = new HashSet<>(); // 宽容前台

    //uid ，设为[冻结]和[播放中不冻结]，且[正在运行]的应用，包括在前台或暂未冻结的
    public Set<Integer> top = new HashSet<>();          // From Hook

    //    Thread serverThread;

    // 不考虑线程安全
//    public int[] top = new int[128]; // top[0] 是有效前台应用的个数，后面是uid列表

    // int[] top会快速频繁变化。 一般前台只有1 ~ 3个，线性查找和哈希表查找效率未知，不知哈希表有没有GC性能影响
//    public boolean inTop(int uid) {
//        int len = top[0];
//        for (int i = 1; i <= len; i++) {
//            if (uid == top[i]) return true;
//        }
//        return false;
//    }

    public Config() {
        super(new File(configFilePath), CLOSE_WRITE);   // SDK >= 29

//        for (int i = 0; i < 128; i++)
//            top[i] = 0;

        if (!configFile.exists()) {
            log(TAG + "File doesn't exists, try create:" + configFile);
            try {
                if (configFile.createNewFile())
                    log(TAG + "File create success:" + configFile);
                else
                    log(TAG + "File create Fail:" + configFile);
            } catch (Exception e) {
                log(TAG + "File create Exception:" + e);
            }
            return;
        }

        parseFile(configFile);
        startWatching();

//        serverThread = new Thread(serverRunnable);
//        serverThread.start();
    }

    @Override
    public void onEvent(int event, String path) {
//        log(TAG + "onEvent: 0x" + Integer.toHexString(event) + ", File: " + path);
        if ((event & CLOSE_WRITE) != 0)
            parseFile(configFile);
    }

    /**
     * 总共4行内容 第四行可能为空
     * 第一行：冻它设置数据
     * 第二行：第三方应用UID列表
     * 第三行：自由后台(含内置)UID列表
     * 第四行：宽容前台UID列表 (此行可能为空)
     */
    void parseFile(File file) {
        if (!file.exists()) {
            log(TAG + "File doesn't exists:" + configFile);
            return;
        }

        try {
            log(TAG + "Start parse: " + file);
            int lineCnt = 0;
            String line;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            thirdApp.clear();
            whitelist.clear();
            tolerant.clear();

            while (null != (line = bufferedReader.readLine())) {
                // 旧版配置文件每行以 dynamic whitelist...开头，放弃解析
                if (line.startsWith("dy") || line.startsWith("se") || line.startsWith("wh") || line.startsWith("th"))
                    break;
                String[] split = line.split(" ");
                switch (lineCnt) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            settings[i] = Integer.parseInt(split[i]);
                        log(TAG + "Parse settings " + split.length);
                        break;
                    case 1:
                        for (String s : split)
                            if (s.length() == 5) // UID 10XXX 长度是5
                                thirdApp.add(Integer.parseInt(s));
                        log(TAG + "Parse thirdApp " + thirdApp.size());
                        break;
                    case 2:
                        for (String s : split)
                            if (s.length() == 5)
                                whitelist.add(Integer.parseInt(s));
                        log(TAG + "Parse whitelist " + whitelist.size());
                        break;
                    case 3:
                        for (String s : split)
                            if (s.length() == 5)
                                tolerant.add(Integer.parseInt(s));
                        log(TAG + "Parse tolerant " + tolerant.size());
                        break;
                }
                lineCnt++;
            }
            log(TAG + "Finish parse: " + file);
        } catch (Exception e) {
            log(TAG + "IOException in file: " + file + ": " + e);
        }
    }


//    void sleep1000ms() {
//        try {
//            Thread.sleep(1000);
//        } catch (Exception ignored) {
//        }
//    }

//
//    // 接收当前运行中的应用，包括当前顶层前台等暂未冻结的应用
//    Runnable serverRunnable = () -> {
//        log(TAG + "receiveRunning Init...");
//        byte[] receiveBuff = new byte[1024];
//        DatagramPacket packet = new DatagramPacket(receiveBuff, receiveBuff.length);
//        DatagramSocket socket;
//        int failCnt = 0;
//        while (true) {
//            try {
//                socket = new DatagramSocket(61995, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
//            } catch (Exception e) {
//                log(TAG + "UDP Init Fail:" + e);
//                if (++failCnt > 30) {
//                    log(TAG + "UDP Init异常达最大次数，即将退出");
//                    topOrRunning.clear();
//                    break;
//                }
//                sleep1000ms();
//                continue;
//            }
//
//            int receiveFailCnt = 0;
//            log(TAG + "receiveRunning Listening...");
//            while (true) {
//                try {
//                    socket.receive(packet);// 阻塞
//                } catch (IOException e) {
//                    log(TAG + "socket.receive IOException:" + e);
//                    if (++receiveFailCnt > 10) {
//                        log(TAG + "socket.receive连续异常达最大次数，重启socketServer");
//                        topOrRunning.clear();
//                        break;
//                    }
//                    continue;
//                }
//                receiveFailCnt = 0;
//
//                // 字节数据格式：[0]数据长度，[1]后续数据的异或校验码，后续每两个字节为一个UID
//                int receiveLen = packet.getLength();
//                if (receiveLen == 0 || receiveLen != Byte.toUnsignedInt(receiveBuff[0])) {
//                    log(TAG + "长度不正确：" + receiveLen);
//                    continue;
//                }
//                byte XOR = 0x5A; // 0B 0101 1010
//                for (int i = 2; i < receiveLen; i++)
//                    XOR ^= receiveBuff[i];
//                if (XOR != receiveBuff[1]) {
//                    log(TAG + "校验不通过");
//                    continue;
//                }
//
//                topOrRunning.clear();
//                for (int i = 2; i < receiveLen; i += 2) {
//                    int uid = (Byte.toUnsignedInt(receiveBuff[i]) << 8) | Byte.toUnsignedInt(receiveBuff[i + 1]);
//                    if (uid < 10000 || uid > 12000)
//                        continue;
//                    topOrRunning.add(uid);
//                }
//            }
//        }
//    };

}
