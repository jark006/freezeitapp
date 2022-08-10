package com.jark006.freezeit.hook;

import android.os.FileObserver;

import com.jark006.freezeit.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.XposedBridge;

public class Sound extends FileObserver {
    final static String TAG = "Freezeit Sound:";
    final static String sndPath = "/dev/snd/";

    public int playCount = 0;

    public Sound() {
        // deprecation in SDK29, use FileObserver(File) instead
        // now minsdk=26
        super(sndPath, OPEN | CLOSE_WRITE | CLOSE_NOWRITE);
    }

    @Override
    public void startWatching() {
        super.startWatching();
        log("startWatching Sound");
    }

    @Override
    public void onEvent(int event, String path) {
        if (path == null || path.length() == 0 || path.charAt(path.length() - 1) != 'p')
            return;

        if ((event & (CLOSE_WRITE | CLOSE_NOWRITE)) != 0) {
            if (playCount > 0)
                playCount--;
        } else if ((event & (OPEN)) != 0) {
            playCount++;
        }

//        log("onEvent: 0x" + Integer.toHexString(event) + ", File: " + path + " playCount:" + playCount);
//        log("playCount:" + playCount);
    }

    void log(String str) {
        XposedBridge.log(TAG + str);
    }
}
