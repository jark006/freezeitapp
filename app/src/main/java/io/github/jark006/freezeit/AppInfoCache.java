package io.github.jark006.freezeit;

import static android.content.pm.ApplicationInfo.FLAG_SYSTEM;
import static android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

public class AppInfoCache {
    private static final String TAG = "Freezeit[AppInfoCache]";

    public static class Info {
        public Drawable icon;
        public String packName;
        public String label;
        public String forSearch;

        public Info(Drawable icon, String packName, String label) {
            this.icon = icon;
            this.packName = packName;
            this.label = label;
            this.forSearch = label.toLowerCase() + packName.toLowerCase();
        }
    }

    public static HashMap<Integer, Info> cacheInfo = new HashMap<>();

    public static void refreshCache(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> applicationList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationList = pm.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES));
        } else {
            applicationList = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        }

        HashMap<Integer, Info> newCacheInfo = new HashMap<>();
        for (ApplicationInfo appInfo : applicationList) {
            if (appInfo.uid < 10000)
                continue;
            if ((appInfo.flags & (FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP)) != 0)
                continue;

            String label = pm.getApplicationLabel(appInfo).toString();
            newCacheInfo.put(appInfo.uid, new Info(appInfo.loadIcon(pm), appInfo.packageName, label));
        }
        Log.d(TAG, "更新缓存" + newCacheInfo.size());
        cacheInfo = newCacheInfo;
    }

    public static Info get(int uid) {
        return cacheInfo.get(uid);
    }
}
