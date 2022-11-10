package com.jark006.freezeit.hook.android;

import com.jark006.freezeit.hook.Config;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AlarmHook {
    final static String TAG = "Freezeit[AlarmHook]:";
    Config config;
    XC_LoadPackage.LoadPackageParam lpParam;

    // TODO
    public AlarmHook(Config config, XC_LoadPackage.LoadPackageParam lpParam) {
        this.config = config;
        this.lpParam = lpParam;

    }
}
