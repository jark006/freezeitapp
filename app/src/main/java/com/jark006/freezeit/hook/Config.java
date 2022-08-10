package com.jark006.freezeit.hook;

import android.os.FileObserver;

import com.jark006.freezeit.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;


public class Config extends FileObserver {
    final static String TAG = "Freezeit FileObserver:";
    final boolean LOG2UDP = false;

    final static String configDirPath = "/data/system/freezeit/";
    final static String configFile = "xposed.conf";

    public Set<String> whiteListDynamic = new HashSet<>();
    public Set<String> whiteListForce = new HashSet<>();
    public Set<String> whiteListConf = new HashSet<>();
    public Set<String> thirdApp = new HashSet<>();

    public int[] settings = new int[256];

    public Config() {
        // deprecation in SDK29, use FileObserver(File) instead
        // now minsdk=26
        super(configDirPath, CLOSE_WRITE);

        log("WhiteList: Init begin");

        File configDir = new File(configDirPath);
        if (!configDir.exists()) {
            boolean mkdir = configDir.mkdir();
            if (!mkdir) {
                log("configDir.mkdir fail");
                return;
            }
            log("configDir.mkdir success");
        }
        parseFile(new File(configDirPath, configFile));

        log("WhiteList: Init over");
    }

    @Override
    public void startWatching() {
        super.startWatching();
        log("startWatching " + configDirPath);
    }

    @Override
    public void onEvent(int event, String path) {
        log("onEvent: 0x" + Integer.toHexString(event) + ", File: " + path);

        if (event == FileObserver.CLOSE_WRITE) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log("onEvent Thread.sleep(500) InterruptedException:" + e);
            }
            parseFile(new File(configDirPath, configFile));
        }
    }

    boolean isEnableDynamicWhiteList() {
        return settings[8] != 0;
    }

    /**
     * File content text: (5 lines in total, "packageName" may not exist in the 3th/4th line)
     * whiteListDynamic packageName packageName packageName ...
     * whiteListForce packageName packageName packageName ...
     * whiteListConf packageName packageName packageName ...
     * thirdPackage packageName packageName packageName ...
     * settings 0 1 6 ...
     */
    void parseFile(File file) {

        if (!file.exists()) {
            log("File doesn't exists, skip:" + file);
            return;
        }

        log("Start parse: " + file);
        int tryTimes = 1;
        while (true) {
            try {
                HashSet<String> result = new HashSet<>();
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.length() < 2) continue;

                    String[] packageNameList = line.split(" ");
                    if (packageNameList.length == 0) continue;

                    result.clear();
                    String target = packageNameList[0];
                    if (packageNameList.length >= 2)
                        result.addAll(Arrays.asList(packageNameList).subList(1, packageNameList.length));

                    switch (target) {
                        case "whiteListDynamic":
                            whiteListDynamic.clear();
                            whiteListDynamic.addAll(result);
                            break;
                        case "whiteListForce":
                            whiteListForce.clear();
                            whiteListForce.addAll(result);
                            break;
                        case "whiteListConf":
                            whiteListConf.clear();
                            whiteListConf.addAll(result);
                            break;
                        case "thirdPackage":
                            thirdApp.clear();
                            thirdApp.addAll(result);
                            break;
                        case "settings":
                            for (int i = 1; i < Math.min(packageNameList.length, settings.length); i++) {
                                settings[i - 1] = Integer.parseInt(packageNameList[i]);
                            }
                            break;
                        default:
                            log("Unknown key:" + target);
                            break;
                    }

                    log("parse " + target + ":" + (target.equals("settings") ?
                            Math.min(packageNameList.length, settings.length) : result.size()));
                }
                bufferedReader.close();
                log("Finish parse: " + file);
                return;
            } catch (IOException e) {
                log("Catch IOException in file: " + file + ": " + e);
            }

            log("Read fail in " + tryTimes + "th times, retry in 2 second later.");

            tryTimes++;
            if (tryTimes >= 3) {
                log("Reach max retry times:" + tryTimes);
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log("Exception Thread.sleep 1000:" + e);
            }
        }
    }

    void log(String str) {
        if (LOG2UDP) Utils.freezeitUDP_Log(str.getBytes(StandardCharsets.UTF_8));
        else XposedBridge.log(TAG + str);
    }

}
