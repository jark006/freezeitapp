package io.github.jark006.freezeit.hook;

import java.util.HashMap;
import io.github.jark006.freezeit.hook.XpUtils.BucketSet;
import io.github.jark006.freezeit.hook.XpUtils.VectorSet;

public class Config {
    public int[] settings = new int[256];
    public BucketSet managedApp = new BucketSet();// 受冻它管控的应用 只含冻结配置和杀死后台 不含自由后台
    public BucketSet tolerant = new BucketSet();  // 宽松前台
    public VectorSet foregroundUid = new VectorSet(20); // 实时 当前在前台(含宽松前台) 底层进程问询时才刷新
    public HashMap<String, Integer> uidIndex = new HashMap<>(); // UID索引
    public HashMap<Integer, String> pkgIndex = new HashMap<>(); // 包名索引

    public boolean isExtendFg() {
        return settings[18] != 0;
    }
}
