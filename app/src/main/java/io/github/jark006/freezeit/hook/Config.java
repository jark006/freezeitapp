package io.github.jark006.freezeit.hook;

import java.util.HashMap;
import io.github.jark006.freezeit.hook.XpUtils.BucketSet;
import io.github.jark006.freezeit.hook.XpUtils.VectorSet;

public class Config {
    final int clusterBindIdx = 1;
    final int freezeTimeoutIdx = 2;
    final int wakeupTimeoutIdx = 3;
    final int terminateTimeoutIdx = 4;
    final int freezeModeIdx = 5;

    final int radicalFgIdx = 10;

    final int batteryIdx = 13;
    final int currentIdx = 14;
    final int breakNetworkIdx = 15;
    final int lmkIdx = 16;
    final int dozeIdx = 17;
    final int extendFgIdx = 18;

    public int[] settings = new int[256];

    public BucketSet managedApp = new BucketSet();// 受冻它管控的应用
    public BucketSet whitelist = new BucketSet(); // 白名单 自由后台和内置自由，若空白则说明冻它未启动
    public BucketSet tolerant = new BucketSet();  // 宽松前台
    public VectorSet foregroundUid = new VectorSet(20); // 实时 当前在前台(含宽松前台) 底层进程问询时才刷新
//    public HashSet<Integer> cacheEmptyPid = new HashSet<>();  // 缓存状态的空进程
    public HashMap<String, Integer> uidIndex = new HashMap<>(); // UID索引
    public HashMap<Integer, String> pkgIndex = new HashMap<>(); // 包名索引

    public boolean isExtendFg() {
        return settings[extendFgIdx] != 0;
    }
}
