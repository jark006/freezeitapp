package com.jark006.freezeit;


import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

public class Utils {
    public final static int CFG_TERMINATE = 10, CFG_SIGSTOP = 20, CFG_FREEZER = 30, CFG_WHITELIST = 40, CFG_WHITEFORCE = 50;

    private static final String TAG = "Utils";

    // 获取信息 无附加数据 No additional data required
    public final static byte getStatus = 1;       // return string: "Freezeit is running"
    public final static byte getInfo = 2;         // return string: "ID\nName\nVersion\nVersionCode\nAuthor"
    public final static byte getChangelog = 3;    // return string: "changelog"
    public final static byte getLog = 4;          // return string: "log"
    public final static byte getAppCfg = 5;       // return string: "package x\n..."   "包名 配置号\n..."
    public final static byte getRealTimeInfo = 6; // return bytes[variable]: (rawBitmap 内存 频率 使用率 电流)
    public final static byte getProcessInfo = 7;  // return string: "process cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\n..."
    public final static byte getSettings = 8;     // return bytes[256]: all settings parameter
    public final static byte getUidTime = 9;      // return "uid time_micro_seconds increase_micro_seconds\n..."

    // 设置 需附加数据
    public final static byte setAppCfg = 21;      // send "package x\n..."   "包名 配置号\n..."
    public final static byte setAppLabel = 22;    // send "uid label\nuid label\nuid label\n..."
    public final static byte setSettingsVar = 23; // send bytes[2]: [0]index [1]value

    // 进程管理 需附加数据
    public final static byte killPid = 41;        // send string: "1234"  //pid num
    public final static byte killApp = 42;        // send string: "packageName"
    public final static byte discharged = 43;     // send string: "packageName"  //临时放行后台

    // 其他命令 无附加数据 No additional data required
    public final static byte clearLog = 61;         // return string: "log" //清理并返回log
    public final static byte printFreezerProc = 62; // return string: "log" //打印冻结状态进程并返回log

    public final static byte reboot = 81;
    public final static byte rebootRecovery = 82;
    public final static byte rebootBootloader = 83;
    public final static byte rebootEdl = 84;

    public static synchronized void freezeitTask(byte command, byte[] AdditionalData, Handler handler) {
        final String hostname = "127.0.0.1";
        final int port = 60613;

        // 前4字节代表附带数据大小(unsigned int32 大端),最后一个字节是附带数据的异或校验值
        // 第五位字节：2:获取模块信息, 3:获取更新日志, 4:获取运行日志, 5:获取白名单.  其他命令参考上面
        byte[] dataHeader = {0, 0, 0, 0, command, 0};
        byte[] responseBuf = null;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(hostname, port), 3000);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            int sendLen = 0;
            if (AdditionalData != null && AdditionalData.length > 0) {
                byte XOR = 0;
                for (byte b : AdditionalData) {
                    XOR ^= b;
                }
                dataHeader[5] = XOR;

                sendLen = AdditionalData.length;
                for (int i = 3; i >= 0; i--) {
                    dataHeader[i] = (byte) (sendLen & 0xff);
                    sendLen >>= 8;
                }
            }

            os.write(dataHeader);

            if (AdditionalData != null && AdditionalData.length > 0)
                os.write(AdditionalData);

            os.flush();

            if (handler != null) {
                int receiveLen = is.read(dataHeader, 0, 6);
                if (receiveLen != 6) {
                    Log.e(TAG, "Receive dataHeader Fail, receiveLen:" + receiveLen);
                    return;
                }

                int requireLen = (Byte.toUnsignedInt(dataHeader[0]) << 24) |
                        (Byte.toUnsignedInt(dataHeader[1]) << 16) |
                        (Byte.toUnsignedInt(dataHeader[2]) << 8) |
                        (Byte.toUnsignedInt(dataHeader[3]));

                responseBuf = new byte[requireLen];

                int readCnt = 0;
                while (readCnt < requireLen) { //欲求不满
                    int cnt = is.read(responseBuf, readCnt, requireLen - readCnt);
                    if (cnt < 0) {
                        Log.e(TAG, "Get Content Fail");
                        return;
                    }
                    readCnt += cnt;
                }
            }

            is.close();
            os.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (handler == null)
            return;

        Message msg = new Message();
        Bundle data = new Bundle();
        data.putByteArray("response", responseBuf);
        msg.setData(data);
        handler.sendMessage(msg);
    }

    public static synchronized void freezeitUDP_Log(byte[] Data) {
        final String targetIP = "192.168.0.120";
        final int targetPort = 8080;

        if (Data == null || Data.length == 0)
            return;

        try {
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket dp = new DatagramPacket(Data, Data.length, InetAddress.getByName(targetIP), targetPort);
            ds.send(dp);
            ds.close();
        } catch (Exception e) {
            Log.e(TAG, "freezeitUDP_Log: send Fail:" + Arrays.toString(Data));
            e.printStackTrace();
        }
    }

    public static Drawable convertToGrayscale(Drawable drawable) {
        Drawable newDrawable = drawable.getConstantState().newDrawable().mutate();
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        newDrawable.setColorFilter(filter);
        return newDrawable;
    }

    public static void getData(String link, Handler handler) {
        byte[] res = null;
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            int resCode = conn.getResponseCode();
            if (resCode != 200) {
                Log.i(ContentValues.TAG, "异常HTTP返回码[" + resCode + "]");
                return;
            }

            InputStream is = conn.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[204800];  //200kb
            while ((len = is.read(buffer)) != -1)
                os.write(buffer, 0, len);

            is.close();
            os.close();
            res = os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(ContentValues.TAG, "Network IO异常");
        }
        if (res == null)
            return;

        Message msg = new Message();
        Bundle data = new Bundle();
        data.putByteArray("response", res);
        msg.setData(data);
        handler.sendMessage(msg);
    }

    public static void imgDialog(Context context, int drawableID) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.img_dialog);
        ((ImageView) dialog.findViewById(R.id.img)).setImageResource(drawableID);
        dialog.show();
    }

    public static int getCpuCluster() {
        HashSet<String> partID = new HashSet<>();
        String line;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("CPU part")) partID.add(line);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return partID.size();
    }
}
