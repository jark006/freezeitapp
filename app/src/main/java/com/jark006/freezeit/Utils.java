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
import java.net.InetSocketAddress;
import java.net.Socket;

public class Utils {

    // 获取信息 无附加数据
    public static byte getStatus = 1;     // return "Freezeit is running"
    public static byte getInfo = 2;       // return "moduleID\nmoduleName\nmoduleVersion\nmoduleVersionCode\nmoduleAuthor"
    public static byte getchangelog = 3;  // return "changelog"
    public static byte getlog = 4;        // return "log"
    public static byte getWhiteList = 5;  // return "packageName\npackageName\npackageName"

    // 依次为 物理内存, 虚拟内存(ZRAM/SWAP), 的 "全部 已用 剩余" in bytes
    public static byte getMemoryInfo = 6;  // return "xxx xxx xxx \nxxx xxx xxx\n"
    public static byte getProcessInfo = 7; // return "process cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\nprocess cpu(%) mem(MB)\n"

    // 设置 需附加数据  xxx:包名 NAME:应用名称
    public static byte setWhiteList = 11; // send "xxx####NAME\nxxx####NAME\nxxx####NAME\n"
    public static byte setAppName = 12;   // send "xxx####NAME\nxxx####NAME\nxxx####NAME\n"

    // 进程管理 需附加数据
    public static byte kill_logd = 21;    // kill logd, needn't
    public static byte kill_pid = 22;     // kill pid, need String(pid num)
    public static byte kill_process = 23; // kill app, need String(packageName)
    public static byte tempAuthorization = 24; //临时授权后台 need String(packageName)


    static public void freezeitTask(byte command, byte[] AdditionalData, Handler handler) {
        final String hostname = "127.0.0.1";
        final int port = 50000;

        // 前4字节代表附带数据大小(unsigned int32 大端),最后的0是附带数据的异或校验值
        // 第五位命令：2:获取模块信息, 3:获取更新日志, 4:获取运行日志, 5:获取自定义白名单
        byte[] cmd = {0, 0, 0, 0, command, 0};
        byte[] responseBuf = null;
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(hostname, port), 1000);
            InputStream is = s.getInputStream();
            OutputStream os = s.getOutputStream();


            if(AdditionalData != null && AdditionalData.length > 0) {
                byte XOR = 0;
                for (byte b : AdditionalData) {
                    XOR ^= b;
                }
                int len = AdditionalData.length;
                for (int i = 3; i >= 0; i--) {
                    cmd[i] = (byte)(len&0xff);
                    len >>= 8;
                }
                cmd[5] = XOR;
            }

            Log.i(TAG, "freezeitTask: "+(AdditionalData==null?"null":String.valueOf(AdditionalData.length)));
            os.write(cmd);

            if(AdditionalData != null && AdditionalData.length > 0)
                os.write(AdditionalData);

            os.flush();

            byte[] dataHeader = new byte[6];
            int readCnt = is.read(dataHeader);
            if(readCnt != dataHeader.length){
                return;
            }
            int wholeLen=0;
            for (int i = 0; i < 4; i++) {
                wholeLen = (wholeLen<<8) | Byte.toUnsignedInt(dataHeader[i]);
            }
            responseBuf = new byte[wholeLen];
//            readCnt = is.read(responseBuf, 0, wholeLen);//若无意外，这行应该够了

            readCnt = 0;
            while(readCnt < wholeLen){ //欲求不满
                int cnt = is.read(responseBuf, readCnt, wholeLen-readCnt);
                if(cnt < 0){
                    Log.e(TAG, "Get Content Fail");
                    return;
                }
                readCnt += cnt;
            }

            is.close();
            os.close();
            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        Message msg = new Message();
        Bundle data = new Bundle();
        data.putByteArray("response", responseBuf);
        msg.setData(data);
        handler.sendMessage(msg);
    }

    public static Drawable convertToGrayscale(Drawable drawable)
    {
        Drawable newDrawable = drawable.getConstantState().newDrawable().mutate();
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        newDrawable.setColorFilter(filter);
        return newDrawable;
    }
}
