package io.github.jark006.freezeit.hook;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XpUtils {
    public static void hookMethod(String TAG, ClassLoader classLoader, XC_MethodHook callback, String className, String methodName, Object... parameterTypes) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        Method method = clazz == null ? null : XposedHelpers.findMethodExactIfExists(clazz, methodName, parameterTypes);
        if (method == null) {
            log(TAG, "HookMethod Not support: " + methodName);
            return;
        }
        XposedBridge.hookMethod(method, callback);
        log(TAG, "HookMethod success: " + methodName);
    }

    public static void hookConstructor(String TAG, ClassLoader classLoader, XC_MethodHook callback, String className, Object... parameterTypes) {
        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
        Constructor<?> constructor = clazz == null ? null : XposedHelpers.findConstructorExact(clazz, parameterTypes);
        if (constructor == null) {
            log(TAG, "HookConstructor not support: " + className);
            return;
        }
        XposedBridge.hookMethod(constructor, callback);
        log(TAG, "HookConstructor success: " + className);
    }

    public static void log(String TAG, String content) {
        XposedBridge.log(TAG + content);
    }

}
