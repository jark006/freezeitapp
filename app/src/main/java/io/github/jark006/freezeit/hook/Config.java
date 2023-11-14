package io.github.jark006.freezeit.hook;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.HashMap;

import io.github.jark006.freezeit.hook.XpUtils.BucketSet;
import io.github.jark006.freezeit.hook.XpUtils.VectorSet;

public class Config {

    boolean mEnableModernQueue = false; // Android14+ SDK34+ 新广播机制

    public int[] settings = new int[256];
    public BucketSet managedApp = new BucketSet();// 受冻它管控的应用 只含冻结配置和杀死后台 不含自由后台
    public BucketSet permissive = new BucketSet();  // 宽松前台
    public VectorSet foregroundUid = new VectorSet(64); // 当前在前台(含宽松前台) 底层进程问询时才刷新
    public VectorSet pendingUid = new VectorSet(64);    // 切到后台暂未冻结的应用
    public HashMap<String, Integer> uidIndex = new HashMap<>(512); // UID索引
    public HashMap<Integer, String> pkgIndex = new HashMap<>(512); // 包名索引

    Field processRecordUidField,
            mCurProcStateField,
            broadcastFilterOwningUidField,
            broadcastRecordCallingUidField,
            broadcastRecordDeliveryField,
            serviceRecordDefiningUidField,
            alarmUidField,
            processRecordStateField,
            mScreenStateField;

    public boolean initField = false;

    public final boolean isCurProcStateInitialized() {
        return mCurProcStateField != null && processRecordStateField != null;
    }

    @SuppressLint("PrivateApi")
    public String Init(ClassLoader classLoader) {
        try {
            // 需进入桌面后才能初始化
            mCurProcStateField = Class.forName(Enum.Class.ProcessStateRecord, true, classLoader).getDeclaredField(Enum.Field.mCurProcState);
            processRecordUidField = Class.forName(Enum.Class.ProcessRecord, true, classLoader).getDeclaredField(Enum.Field.uid);
            broadcastFilterOwningUidField = Class.forName(Enum.Class.BroadcastFilter, true, classLoader).getDeclaredField(Enum.Field.owningUid);
            broadcastRecordCallingUidField = Class.forName(Enum.Class.BroadcastRecord, true, classLoader).getDeclaredField(Enum.Field.callingUid);
            broadcastRecordDeliveryField = Class.forName(Enum.Class.BroadcastRecord, true, classLoader).getDeclaredField(Enum.Field.delivery);
            serviceRecordDefiningUidField = Class.forName(Enum.Class.ServiceRecord, true, classLoader).getDeclaredField(Enum.Field.definingUid);
            alarmUidField = Class.forName(Enum.Class.AlarmS, true, classLoader).getDeclaredField(Enum.Field.uid);
            processRecordStateField = Class.forName(Enum.Class.ProcessRecord, true, classLoader).getDeclaredField(Enum.Field.mState);
            mScreenStateField = Class.forName(Enum.Class.DisplayPowerState, true, classLoader).getDeclaredField(Enum.Field.mScreenState);

            mCurProcStateField.setAccessible(true);
            processRecordUidField.setAccessible(true);
            broadcastFilterOwningUidField.setAccessible(true);
            broadcastRecordCallingUidField.setAccessible(true);
            broadcastRecordDeliveryField.setAccessible(true);
            serviceRecordDefiningUidField.setAccessible(true);
            alarmUidField.setAccessible(true);
            processRecordStateField.setAccessible(true);
            mScreenStateField.setAccessible(true);

            initField = true;
            return "[SUCCESS]";
        } catch (Exception e) {
            initField = false;

            return "\n[ !!! FAIL !!! ]\n[ !!! 失败 !!! ]\n[ !!! FAIL !!! ]\n" +
                    (mCurProcStateField != null ? 'O' : 'X') +
                    (processRecordUidField != null ? 'O' : 'X') +
                    (broadcastFilterOwningUidField != null ? 'O' : 'X') +
                    (broadcastRecordCallingUidField != null ? 'O' : 'X') +
                    (broadcastRecordDeliveryField != null ? 'O' : 'X') +
                    (serviceRecordDefiningUidField != null ? 'O' : 'X') +
                    (alarmUidField != null ? 'O' : 'X') +
                    (processRecordStateField != null ? 'O' : 'X') +
                    (mScreenStateField != null ? 'O' : 'X') +
                    e;
        }
    }

    public final int getProcessRecordUid(@NonNull Object obj) {
        try {
            return processRecordUidField == null ? -1 : processRecordUidField.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    public final Object getProcessRecordState(@NonNull Object obj) {
        try {
            return processRecordStateField.get(obj); // isCurProcStateInitialized() 已判空
        } catch (Exception e) {
            return null;
        }
    }

    public final int getCurProcState(@NonNull Object obj) {
        try {
            return mCurProcStateField.getInt(obj); // isCurProcStateInitialized() 已判空
        } catch (Exception e) {
            return -1;
        }
    }

    public final int getBroadcastFilterOwningUid(@NonNull Object obj) {
        try {
            return broadcastFilterOwningUidField == null ? -1 : broadcastFilterOwningUidField.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    public final int getBroadcastRecordCallingUid(@NonNull Object obj) {
        try {
            return broadcastRecordCallingUidField == null ? -1 : broadcastRecordCallingUidField.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    public final int[] getBroadcastRecordDelivery(@NonNull Object obj) {
        try {
            return broadcastRecordDeliveryField == null ? null : (int[]) broadcastRecordDeliveryField.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public final int getServiceRecordDefiningUid(@NonNull Object obj) {
        try {
            return serviceRecordDefiningUidField == null ? -1 : serviceRecordDefiningUidField.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    public final int getAlarmUid(@NonNull Object obj) {
        try {
            return alarmUidField == null ? -1 : alarmUidField.getInt(obj);
        } catch (Exception e) {
            return -1;
        }
    }

    public final int getScreenState(@NonNull Object obj) {
        try {
            return mScreenStateField == null ? 0 : mScreenStateField.getInt(obj);
        } catch (Exception e) {
            return 0;
        }
    }

}
