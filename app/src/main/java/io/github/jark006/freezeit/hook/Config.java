package io.github.jark006.freezeit.hook;

import static de.robv.android.xposed.XposedBridge.log;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.util.HashSet;
import java.util.Set;


public class Config {
    final static String TAG = "Freezeit[Config]:";

    public int[] settings = new int[1024];
    public Set<Integer> thirdApp = new HashSet<>();  // 受冻它管控的应用
    public Set<Integer> whitelist = new HashSet<>(); // 白名单 自由后台和内置自由
    public Set<Integer> tolerant = new HashSet<>();  // 宽松前台
    public Set<Integer> top = new HashSet<>();       // 实时 当前在前台(含宽松前台) 底层进程问询时才刷新

    LocalSocketServer serverThread = new LocalSocketServer();

    public Config() {
        serverThread.start();
    }

    /**
     * 总共4行内容 第四行可能为空
     * 第一行：冻它设置数据
     * 第二行：第三方应用UID列表
     * 第三行：自由后台(含内置)UID列表
     * 第四行：宽松前台UID列表 (此行可能为空)
     */
    void parseString(String str) {
        if (str == null || str.length() == 0) {
            log(TAG + "skip parseString");
            return;
        }

        String[] splitLine = str.split("\n");
        if (splitLine.length < 3 || splitLine.length > 4) {
            log(TAG + "行数错误:" + str);
            log(TAG + "splitLine.length:" + splitLine.length);
            for (String line : splitLine)
                log(TAG + "START:" + line);

            return;
        }

        thirdApp.clear();
        whitelist.clear();
        tolerant.clear();

        log(TAG + "Start parse, line:"+splitLine.length);
        try {
            for (int lineIdx = 0; lineIdx < splitLine.length; lineIdx++) {
                String[] split = splitLine[lineIdx].split(" ");
                switch (lineIdx) {
                    case 0:
                        for (int i = 0; i < split.length; i++)
                            settings[i] = Integer.parseInt(split[i]);
                        log(TAG + "Parse settings  " + split.length);
                        break;
                    case 1:
                        for (String s : split)
                            if (s.length() == 5) // UID 10XXX 长度是5
                                thirdApp.add(Integer.parseInt(s));
                        log(TAG + "Parse thirdApp  " + thirdApp.size());
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
                        log(TAG + "Parse tolerant  " + tolerant.size());
                        break;
                }
            }
            log(TAG + "Finish parse");
        } catch (Exception e) {
            log(TAG + "IOException: [" + str + "]: \n" + e);
        }
    }

    // 问询制
    public class LocalSocketServer extends Thread {
        byte[] recvBuff = new byte[8 * 1024]; // 8 Kib

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                LocalServerSocket mSocketServer = new LocalServerSocket("FreezeitCfg");
                while (true) {
                    LocalSocket client = mSocketServer.accept();//堵塞,等待客户端
                    if (client == null) continue;

                    client.setSoTimeout(200);
                    // 头部前4字节的数值 是 后续数据载体的长度，这个长度值不包括头部4字节
                    int recvLen = client.getInputStream().read(recvBuff, 0, 4);
                    if (recvLen != 4) {
                        client.close();
                        log(TAG + "unknown header:" + recvLen);
                        continue;
                    }

                    // 小端
                    int payloadLen = Byte.toUnsignedInt(recvBuff[0]) |
                            (Byte.toUnsignedInt(recvBuff[1]) << 8) |
                            (Byte.toUnsignedInt(recvBuff[2]) << 16) |
                            (Byte.toUnsignedInt(recvBuff[3]) << 24);
                    if (payloadLen > recvBuff.length) {
                        client.close();
                        log(TAG + "payloadLen too much:" + payloadLen);
                        continue;
                    }

                    recvLen = client.getInputStream().read(recvBuff, 0, payloadLen);
                    client.close(); // 不考虑长连接，不用回复，用完就关

                    if (recvLen != payloadLen) {
                        log(TAG + "Not Match recvLen:" + recvLen + " payloadLen:" + payloadLen);
                        continue;
                    }

                    recvBuff[recvLen < recvBuff.length ? recvLen : recvBuff.length - 1] = 0;
                    parseString(new String(recvBuff,0, recvLen)); // StandardCharsets.UTF_8
                }
            } catch (Exception e) {
                log(TAG + e);
            }
        }
    }
}
