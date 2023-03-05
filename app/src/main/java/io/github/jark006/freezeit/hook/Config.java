package io.github.jark006.freezeit.hook;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.HashMap;

import io.github.jark006.freezeit.hook.XpUtils.BucketSet;
import io.github.jark006.freezeit.hook.XpUtils.VectorSet;

public class Config {
    public int[] settings = new int[256];
    public BucketSet managedApp = new BucketSet();// 受冻它管控的应用 只含冻结配置和杀死后台 不含自由后台
    public BucketSet tolerant = new BucketSet();  // 宽松前台
    public VectorSet foregroundUid = new VectorSet(20); // 实时 当前在前台(含宽松前台) 底层进程问询时才刷新
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
            taskInfoVisibleField,
            taskInfoTaskNamesField;

    public boolean initField = false;

    public final boolean isExtendFg() {
        return settings[18] != 0 && taskInfoVisibleField != null && taskInfoTaskNamesField != null;
    }

    public final boolean isCurProcStateInitialized() {
        return mCurProcStateField != null && (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R || processRecordStateField != null);
    }

    @SuppressLint("PrivateApi")
    public String Init(ClassLoader classLoader) {
        try {
            // 需进入桌面后才能初始化
            mCurProcStateField = Class.forName(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    Enum.Class.ProcessStateRecord : Enum.Class.ProcessRecord, true, classLoader).getDeclaredField(Enum.Field.mCurProcState);
            processRecordUidField = Class.forName(Enum.Class.ProcessRecord, true, classLoader).getDeclaredField(Enum.Field.uid);
            broadcastFilterOwningUidField = Class.forName(Enum.Class.BroadcastFilter, true, classLoader).getDeclaredField(Enum.Field.owningUid);
            broadcastRecordCallingUidField = Class.forName(Enum.Class.BroadcastRecord, true, classLoader).getDeclaredField(Enum.Field.callingUid);
            broadcastRecordDeliveryField = Class.forName(Enum.Class.BroadcastRecord, true, classLoader).getDeclaredField(Enum.Field.delivery);
            serviceRecordDefiningUidField = Class.forName(Enum.Class.ServiceRecord, true, classLoader).getDeclaredField(Enum.Field.definingUid);
            alarmUidField = Class.forName(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    Enum.Class.AlarmS : Enum.Class.AlarmR, true, classLoader).getDeclaredField(Enum.Field.uid);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                processRecordStateField = Class.forName(Enum.Class.ProcessRecord, true, classLoader).getDeclaredField(Enum.Field.mState);
                processRecordStateField.setAccessible(true);
            }
            taskInfoVisibleField = Class.forName(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    Enum.Class.RootTaskInfoS : Enum.Class.StackInfo, true, classLoader).getDeclaredField(Enum.Field.visible);

            taskInfoTaskNamesField = Class.forName(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    Enum.Class.RootTaskInfoS : Enum.Class.StackInfo, true, classLoader).getDeclaredField(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? Enum.Field.childTaskNames : Enum.Field.taskNames);


            mCurProcStateField.setAccessible(true);
            processRecordUidField.setAccessible(true);
            broadcastFilterOwningUidField.setAccessible(true);
            broadcastRecordCallingUidField.setAccessible(true);
            broadcastRecordDeliveryField.setAccessible(true);
            serviceRecordDefiningUidField.setAccessible(true);
            alarmUidField.setAccessible(true);
            taskInfoVisibleField.setAccessible(true);
            taskInfoTaskNamesField.setAccessible(true);


            initField = true;
            return "[SUCCESS]";
        } catch (Exception e) {
            initField = false;

            var initInfo = new StringBuilder("\n[ !!! FAIL !!! ]\n[ !!! 失败 !!! ]\n[ !!! FAIL !!! ]\n");
            initInfo.append(mCurProcStateField != null ? 'O' : 'X');
            initInfo.append(processRecordUidField != null ? 'O' : 'X');
            initInfo.append(broadcastFilterOwningUidField != null ? 'O' : 'X');
            initInfo.append(broadcastRecordCallingUidField != null ? 'O' : 'X');
            initInfo.append(broadcastRecordDeliveryField != null ? 'O' : 'X');
            initInfo.append(serviceRecordDefiningUidField != null ? 'O' : 'X');
            initInfo.append(alarmUidField != null ? 'O' : 'X');
            initInfo.append(taskInfoVisibleField != null ? 'O' : 'X');
            initInfo.append(taskInfoTaskNamesField != null ? 'O' : 'X');
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                initInfo.append(processRecordStateField != null ? 'O' : 'X');

            initInfo.append(e);
            return initInfo.toString();
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

    public final boolean getTaskInfoVisible(@NonNull Object obj) {
        try {
            return taskInfoVisibleField.getBoolean(obj); // isExtendFg() 已判空
        } catch (Exception e) {
            return false;
        }
    }

    public final String[] getTaskInfoTaskNames(@NonNull Object obj) {
        try {
            return (String[]) taskInfoTaskNamesField.get(obj); // isExtendFg() 已判空
        } catch (Exception e) {
            return null;
        }
    }
}
