package io.github.jark006.freezeit.hook;

import java.util.HashSet;

public class Config {
    public int[] settings = new int[256];
    public HashSet<Integer> thirdApp = new HashSet<>();  // 受冻它管控的应用
    public HashSet<Integer> whitelist = new HashSet<>(); // 白名单 自由后台和内置自由
    public HashSet<Integer> tolerant = new HashSet<>();  // 宽松前台
    public HashSet<Integer> top = new HashSet<>();       // 实时 当前在前台(含宽松前台) 底层进程问询时才刷新
}
