package io.github.jark006.freezeit;

public class ManagerCmd {


    // 获取信息 无附加数据 No additional data required
    public final static byte getPropInfo = 2;     // return string: "ID\nName\nVersion\nVersionCode\nAuthor\nClusterNum"
    public final static byte getChangelog = 3;    // return string: "changelog"
    public final static byte getLog = 4;          // return string: "log"
    public final static byte getAppCfg = 5;       // return string: "uid cfg isPermissive\n..."   "包名 配置 宽松\n..." <uid, <cfg, isPermissive>>
    public final static byte getRealTimeInfo = 6; // return bytes[rawBitmap+String]: (rawBitmap+内存 频率 使用率 电流)
    public final static byte getSettings = 8;     // return bytes[256]: all settings parameter
    public final static byte getUidTime = 9;      // return "uid x x x x\n..."
    public final static byte getXpLog = 10;

    // 设置 需附加数据
    public final static byte setAppCfg = 21;      // send "uid cfg isPermissive\n..." see CMD:getAppCfg
    public final static byte setAppLabel = 22;    // send "uid label\n..."
    public final static byte setSettingsVar = 23; // send bytes[2]: [0]index [1]value

    // 其他命令 无附加数据 No additional data required
    public final static byte clearLog = 61;         // return string: "log" //清理日志
    public final static byte printFreezerProc = 62; // return string: "log" //打印冻结状态进程并返回log
}
