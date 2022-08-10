package com.jark006.freezeit;


import static androidx.constraintlayout.widget.ConstraintLayoutStates.TAG;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class Utils {
    private static final String TAG = "Utils";

    // 获取信息 无附加数据 No additional data required
    public final static byte getStatus = 1;       // return string: "Freezeit is running"
    public final static byte getInfo = 2;         // return string: "ID\nName\nVersion\nVersionCode\nAuthor"
    public final static byte getChangelog = 3;    // return string: "changelog"
    public final static byte getLog = 4;          // return string: "log"
    public final static byte getWhiteList = 5;    // return string: "package\npackage\npackage####package\npackage\npackage" 内置白名单####自定义白名单
    public final static byte getRealTimeInfo = 6; // return bytes[variable]: (rawBitmap 内存 频率 使用率 电流)
    public final static byte getProcessInfo = 7;  // return string: "process cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\n..."
    public final static byte getSettings = 8;     // return bytes[256]: all settings parameter

    // 设置 需附加数据
    public final static byte setWhiteList = 21;   // send "package####label\npackage####label\npackage####label\n..."
    public final static byte setAppLabel = 22;    // send "package####label\npackage####label\npackage####label\n..."
    public final static byte setSettingsVar = 23; // send bytes[2]: [0]index [1]value

    // 进程管理 需附加数据
    public final static byte killPid = 41;        // send string: "1234"  //pid num
    public final static byte killApp = 42;        // send string: "packageName"
    public final static byte discharged = 43;     // send string: "packageName"  //临时放行后台

    // 其他命令 无附加数据 No additional data required
    public final static byte clearLog = 61;       // return string: "clear log" //清理并返回空log
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

            Log.i(TAG, "freezeitTask: additionalData bytes: " + sendLen);
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
//                receiveLen = is.read(responseBuf, 0, requireLen);
//                if (receiveLen != requireLen) {
//                    Log.e(TAG, "Receive data Fail, requireLen:" + requireLen + ", receiveLen:" + receiveLen);
//                    return;
//                }

                int readCnt = 0;
                while(readCnt < requireLen){ //欲求不满
                    int cnt = is.read(responseBuf, readCnt, requireLen-readCnt);
                    if(cnt < 0){
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


}
