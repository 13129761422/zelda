package com.virjar.zelda.engine.fixer;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.virjar.zelda.engine.ZeldaRuntime;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedHelpers;
import mirror.android.ddm.DdmHandleAppName;

public class AppBindDataFixer {

    public static void fixAppBindData() {
        Object mBoundApplication = XposedHelpers.getObjectField(ZeldaRuntime.mainThread, "mBoundApplication");

        fixProcessName(mBoundApplication);

        fixLoadApk(mBoundApplication);

        fixApplicationInfo(mBoundApplication);

    }

    private static void fixObjectField(Object object, Class clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.isSynthetic()) {
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() == String.class) {
                String fieldValue = (String) XposedHelpers.getObjectField(object, field.getName());
                if (fieldValue == null) {
                    continue;
                }
                XposedHelpers.setObjectField(object, field.getName(), fieldValue.replace(ZeldaRuntime.nowPackageName, ZeldaRuntime.originPackageName));
            } else if (field.getType() == File.class) {
                File fieldValue = (File) XposedHelpers.getObjectField(object, field.getName());
                if (fieldValue == null) {
                    continue;
                }
                if (fieldValue.getAbsolutePath().contains(ZeldaRuntime.nowPackageName)) {
                    File newFile = new File(fieldValue.getAbsolutePath().replace(ZeldaRuntime.nowPackageName, ZeldaRuntime.originPackageName));
                    XposedHelpers.setObjectField(object, field.getName(), newFile);
                }
            }
        }
        Class superclass = clazz.getSuperclass();
        if (superclass != null && superclass.getClassLoader() == Application.class.getClassLoader()) {
            fixObjectField(object, superclass);
        }
    }

    private static void fixApplicationInfo(Object appBindData) {
        ApplicationInfo applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(appBindData, "appInfo");

        fixObjectField(applicationInfo, applicationInfo.getClass());

        if (!TextUtils.isEmpty(ZeldaRuntime.originApplicationName)) {
            applicationInfo.className = ZeldaRuntime.originApplicationName;
        } else {
            //the className value can not be com.virjar.zelda.engine.ZeldaApplication
            applicationInfo.className = "android.app.Application";
        }

    }

    private static void fixLoadApk(Object appBindData) {
        Object loadApk = XposedHelpers.getObjectField(appBindData, "info");
        ZeldaRuntime.theLoadApk = loadApk;
        fixObjectField(loadApk, loadApk.getClass());
        fixActivityThreadLoadApkCache();
    }

    @SuppressWarnings("unchecked")
    private static void fixActivityThreadLoadApkCache() {
        ArrayMap<String, WeakReference<Object>> mPackages = (ArrayMap<String, WeakReference<Object>>) XposedHelpers.getObjectField(ZeldaRuntime.mainThread, "mPackages");
        WeakReference<Object> objectWeakReference = mPackages.get(ZeldaRuntime.nowPackageName);
        if (objectWeakReference != null) {
            mPackages.put(ZeldaRuntime.originPackageName, objectWeakReference);
        }

        ArrayMap<String, WeakReference<Object>> mResourcePackages = (ArrayMap<String, WeakReference<Object>>) XposedHelpers.getObjectField(ZeldaRuntime.mainThread, "mResourcePackages");
        objectWeakReference = mResourcePackages.get(ZeldaRuntime.nowPackageName);
        if (objectWeakReference != null) {
            mPackages.put(ZeldaRuntime.originPackageName, objectWeakReference);
        }
    }


    private static void fixProcessName(Object appBindData) {
        ZeldaRuntime.processName = (String) XposedHelpers.getObjectField(appBindData, "processName");
        ZeldaRuntime.processName = ZeldaRuntime.processName.replace(ZeldaRuntime.nowPackageName, ZeldaRuntime.originPackageName);
        XposedHelpers.setObjectField(appBindData, "processName", ZeldaRuntime.processName);
        DdmHandleAppName.setAppName.call(ZeldaRuntime.processName, 0);
    }

}
