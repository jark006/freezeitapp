package com.jark006.freezeit.hook;

import android.os.FileObserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;


public class Config extends FileObserver {
    final static String TAG = "Freezeit FileObserver:";

    final static String configFilePath = "/data/system/freezeit.conf";
    final File configFile = new File(configFilePath);

    public int[] settings = new int[1024];
    public Set<Integer> thirdApp = new HashSet<>();
    public Set<Integer> whitelist = new HashSet<>();
    public Set<Integer> dynamic = new HashSet<>();

    public Set<Integer> visibleOnTop = new HashSet<>(); //uid ，在前台的[非自由后台]应用

    public Config() {
        super(configFilePath, CLOSE_WRITE); // deprecation in SDK29, use FileObserver(File) instead
//        super(configFile, CLOSE_WRITE);   // SDK >= 29

        parseFile(configFile);
        startWatching();

        UDPServerThread st = new UDPServerThread();
        st.start();
    }

    @Override
    public void onEvent(int event, String path) {
//        log("onEvent: 0x" + Integer.toHexString(event) + ", File: " + path);
        if ((event & CLOSE_WRITE) != 0) {
            mySleep(100);
            parseFile(configFile);
        }
    }

    /**
     * 总共4行内容
     * 第一行：设置数据
     * 第二行：第三方应用UID列表
     * 第三行：自由后台(含内置)UID列表
     * 第四行：播放中不冻结UID列表 (此行可能为空)
     */
    void parseFile(File file) {

        if (!file.exists()) {
            log("File doesn't exists, try create:" + file);
            try {
                if (file.createNewFile())
                    log("File create success:" + file);
                else
                    log("File create Fail:" + file);
            } catch (Exception e) {
                log("File create Exception:" + e);
            }
            return;
        }

        try {
            log("Start parse: " + file);
            int idx = 0;
            String line;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while (null != (line = bufferedReader.readLine())) {
                // 旧版配置文件每行以 dynamic whitelist...开头，放弃解析
                if (line.startsWith("dy") || line.startsWith("se") || line.startsWith("wh") || line.startsWith("th"))
                    break;
                String[] split = line.split(" ");
                switch (idx) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            settings[i] = Integer.parseInt(split[i]);
                        log("Parse settings " + split.length);
                        break;
                    case 1:
                        thirdApp.clear();
                        for (String s : split)
                            if (s.length() == 5)
                                thirdApp.add(Integer.parseInt(s));
                        log("Parse thirdApp " + split.length);
                        break;
                    case 2:
                        whitelist.clear();
                        for (String s : split)
                            if (s.length() == 5)
                                whitelist.add(Integer.parseInt(s));
                        log("Parse whitelist " + split.length);
                        break;
                    case 3:
                        dynamic.clear();
                        for (String s : split)
                            if (s.length() == 5)
                                dynamic.add(Integer.parseInt(s));
                        log("Parse dynamic " + split.length);
                        break;
                }
                idx++;
            }
            log("Finish parse: " + file);
        } catch (Exception e) {
            log("IOException in file: " + file + ": " + e);
        }
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }

    void mySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {
        }
    }

    // 接收前台窗口的应用
    public class UDPServerThread extends Thread {

        public void run() {
            byte[] receiveBuff = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveBuff, receiveBuff.length);
            DatagramSocket socket;
            int failCnt = 0;
            log("Listening visibleApp...");
            while (true) {
                try {
                    socket = new DatagramSocket(61995, InetAddress.getLoopbackAddress());
                } catch (SocketException e) {
                    log("UDP Init Fail:" + e);
                    if (++failCnt > 30) {
                        log("达最大异常次数，已关闭UDP");
                        return;
                    }
                    mySleep(1000);
                    continue;
                }

                while (true) {
                    try {
                        socket.receive(packet);// 阻塞
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    int len = packet.getLength();
                    if (len == 0 || len > 10 || (len & 1) == 1) { //正常是偶数长度
                        log("长度不正确：" + len);
                        continue;
                    }

                    byte XOR = 0x5A; // 0B 0101 1010
                    for (int i = 2; i < len; i++)
                        XOR ^= receiveBuff[i];
                    if (XOR != receiveBuff[0] || XOR != ~receiveBuff[1]) {
                        log("校验不通过");
                        continue;
                    }

                    visibleOnTop.clear();
                    for (int i = 2; i < len; i += 2) {
                        int uid = (Byte.toUnsignedInt(receiveBuff[i]) << 8) | Byte.toUnsignedInt(receiveBuff[i + 1]);
                        if (uid < 10000 || uid > 12000)
                            continue;
                        visibleOnTop.add(uid);
                    }
                }
                visibleOnTop.clear();
                log("UDP异常" + failCnt + "次，清理实时前台");
            }
        }
    }
}
