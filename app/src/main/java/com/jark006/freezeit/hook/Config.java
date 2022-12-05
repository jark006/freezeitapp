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
    public Set<Integer> tolerant = new HashSet<>(); // 宽容前台

    public Set<Integer> top = new HashSet<>();//实时 在前台或宽容前台

    public Config() {
        super(new File(configFilePath), CLOSE_WRITE);   // SDK >= 29

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

}


//
//
//public class Config extends FileObserver {
//    final static String TAG = "Freezeit[Config]:";
//
//    final static String configFilePath = "/data/system/freezeit.conf";
//    final File configFile = new File(configFilePath);
//
//    public int[] settings = new int[1024];
//    public Set<Integer> thirdApp = new HashSet<>();
//    public Set<Integer> whitelist = new HashSet<>();
//    public Set<Integer> tolerant = new HashSet<>(); // 宽容前台
//
//    public Set<Integer> top = new HashSet<>();//实时 在前台或宽容前台
//
//    public Config() {
//        super(new File(configFilePath), CLOSE_WRITE);   // SDK >= 29
//
//        if (!configFile.exists()) {
//            log(TAG + "File doesn't exists, try create:" + configFile);
//            try {
//                if (configFile.createNewFile())
//                    log(TAG + "File create success:" + configFile);
//                else
//                    log(TAG + "File create Fail:" + configFile);
//            } catch (Exception e) {
//                log(TAG + "File create Exception:" + e);
//            }
//            return;
//        }
//
//        parseFile(configFile);
//        startWatching();
//    }
//
//    @Override
//    public void onEvent(int event, String path) {
////        log(TAG + "onEvent: 0x" + Integer.toHexString(event) + ", File: " + path);
//        if ((event & CLOSE_WRITE) != 0)
//            parseFile(configFile);
//    }
//
//    /**
//     * 总共4行内容 第四行可能为空
//     * 第一行：冻它设置数据
//     * 第二行：第三方应用UID列表
//     * 第三行：自由后台(含内置)UID列表
//     * 第四行：宽容前台UID列表 (此行可能为空)
//     */
//    void parseFile(File file) {
//        if (!file.exists()) {
//            log(TAG + "File doesn't exists:" + configFile);
//            return;
//        }
//
//        try {
//            log(TAG + "Start parse: " + file);
//            int lineCnt = 0;
//            String line;
//            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
//
//            thirdApp.clear();
//            whitelist.clear();
//            tolerant.clear();
//
//            while (null != (line = bufferedReader.readLine())) {
//                // 旧版配置文件每行以 dynamic whitelist...开头，放弃解析
//                if (line.startsWith("dy") || line.startsWith("se") || line.startsWith("wh") || line.startsWith("th"))
//                    break;
//                String[] split = line.split(" ");
//                switch (lineCnt) {
//                    case 0:
//                        for (int i = 0; i < split.length; i++)
//                            settings[i] = Integer.parseInt(split[i]);
//                        log(TAG + "Parse settings " + split.length);
//                        break;
//                    case 1:
//                        for (String s : split)
//                            if (s.length() == 5) // UID 10XXX 长度是5
//                                thirdApp.add(Integer.parseInt(s));
//                        log(TAG + "Parse thirdApp " + thirdApp.size());
//                        break;
//                    case 2:
//                        for (String s : split)
//                            if (s.length() == 5)
//                                whitelist.add(Integer.parseInt(s));
//                        log(TAG + "Parse whitelist " + whitelist.size());
//                        break;
//                    case 3:
//                        for (String s : split)
//                            if (s.length() == 5)
//                                tolerant.add(Integer.parseInt(s));
//                        log(TAG + "Parse tolerant " + tolerant.size());
//                        break;
//                }
//                lineCnt++;
//            }
//            log(TAG + "Finish parse: " + file);
//        } catch (Exception e) {
//            log(TAG + "IOException in file: " + file + ": " + e);
//        }
//    }
//
//}
