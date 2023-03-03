package io.github.jark006.freezeit.hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.jark006.freezeit.Utils;

public class XpUtils {
    public final static boolean DEBUG_WAKEUP_LOCK = false;
    public final static boolean DEBUG_BROADCAST = false;
    public final static boolean DEBUG_ALARM = false;
    public final static boolean DEBUG_ANR = false;

    public static void hookMethod(String TAG, ClassLoader classLoader, XC_MethodHook callback,
                                  String className, String methodName, Object... parameterTypes) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        Method method = clazz == null ? null :
                XposedHelpers.findMethodExactIfExists(clazz, methodName, parameterTypes);
        if (method == null) {
            log(TAG, "HookMethod Not support: " + methodName);
            return;
        }
        XposedBridge.hookMethod(method, callback);
        log(TAG, "HookMethod success: " + methodName);
    }

    public static void hookConstructor(String TAG, ClassLoader classLoader, XC_MethodHook callback,
                                       String className, Object... parameterTypes) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        Constructor<?> constructor = clazz == null ? null :
                XposedHelpers.findConstructorExact(clazz, parameterTypes);
        if (constructor == null) {
            log(TAG, "HookConstructor not support: " + className);
            return;
        }
        XposedBridge.hookMethod(constructor, callback);
        log(TAG, "HookConstructor success: " + className);
    }

    public static Field getField(final Object obj, final String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            XpUtils.log("Freezeit[getInt]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);
            return null;
        }
    }

    public static int getInt(final Object obj, final String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(obj);
        } catch (Exception e) {
            XpUtils.log("Freezeit[getInt]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);
            return -1;
        }
    }

    public static boolean getBoolean(final Object obj, final String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(obj);
        } catch (Exception e) {
            XpUtils.log("Freezeit[getBoolean]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);
            return false;
        }
    }

    public static String getString(final Object obj, final String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(obj);
        } catch (Exception e) {
            XpUtils.log("Freezeit[getString]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);
            return "null";
        }
    }

    public static Object getObject(final Object obj, final String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            XpUtils.log("Freezeit[getObject]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);
            return null;
        }
    }

    public static void log(String TAG, String content) {
        XposedBridge.log(TAG + content);
    }

    // 少量元素(0-10)时，clear,add,contain 性能均优于 HashSet, TreeSet
    public static class VectorSet {
        int size = 0, maxSize;
        int[] number;

        public VectorSet(int maxSize) {
            this.maxSize = maxSize;
            number = new int[maxSize];
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            size = 0;
        }

        public void add(final int n) {
            for (int i = 0; i < size; i++) {
                if (number[i] == n) return;
            }
            if (size < maxSize)
                number[size++] = n;
        }

        public void erase(final int n) {
            for (int i = 0; i < size; i++) {
                if (number[i] == n) {
                    number[i] = number[--size];
                    return;
                }
            }
        }

        // 顺序查找
        public boolean contains(final int n) {
            if (n < 10000) return false;
            for (int i = 0; i < size; i++) {
                if (number[i] == n)
                    return true;
            }
            return false;
        }

        public void toBytes(byte[] bytes, int byteOffset) {
            if (size > 0)
                Utils.Int2Byte(number, 0, size, bytes, byteOffset);
        }
    }

    // 造轮子：常见UID位于 10000 ~ 12000
    // 在 APP UID 范围, 性能均优于HashSet
    public static class BucketSet {

        int size = 0;
        byte[] bucket = new byte[2000];// 默认最多两千个应用

        public BucketSet() {
            clear();
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            size = 0;
            Arrays.fill(bucket, (byte) 0);
        }

        public void add(final int n) {
            if (n < 10000 || 12000 <= n)
                return;
            if (bucket[n - 10000] == 0) {
                bucket[n - 10000] = 1;
                size++;
            }
        }

        public void erase(final int n) {
            if (n < 10000 || 12000 <= n)
                return;
            if (bucket[n - 10000] != 0) {
                bucket[n - 10000] = 0;
                size--;
            }
        }

        public boolean contains(final int n) {
            if (n < 10000 || 12000 <= n)
                return false;
            return bucket[n - 10000] != 0;
        }
    }

    // bitmap
    public static class BucketBitSet {

        int size = 0;
        final int[] bitMask = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80};
        byte[] bucketBit = new byte[250]; // 默认最多两千个应用 250 * 8 Bit

        public BucketBitSet() {
            clear();
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            size = 0;
            Arrays.fill(bucketBit, (byte) 0);
        }

        public void add(final int n) {
            if (n < 10000 || 12000 <= n)
                return;

            final int byteIdx = (n - 10000) >> 3;
            final int mask = bitMask[(n - 10000) & 0b111];
            if ((bucketBit[byteIdx] & mask) == 0) {
                bucketBit[byteIdx] |= mask;
                size++;
            }
        }

        public void erase(final int n) {
            if (n < 10000 || 12000 <= n)
                return;

            final int byteIdx = (n - 10000) >> 3;
            final int mask = bitMask[(n - 10000) & 0b111];
            if ((bucketBit[byteIdx] & mask) != 0) {
                bucketBit[byteIdx] &= ~mask;
                size--;
            }
        }

        public boolean contains(final int n) {
            if (n < 10000 || 12000 <= n)
                return false;

            final int byteIdx = (n - 10000) >> 3;
            final int mask = bitMask[(n - 10000) & 0b111];
            return (bucketBit[byteIdx] & mask) != 0;
        }
    }
}
