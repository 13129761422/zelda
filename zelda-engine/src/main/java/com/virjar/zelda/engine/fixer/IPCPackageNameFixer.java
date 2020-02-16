package com.virjar.zelda.engine.fixer;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.engine.ZeldaRuntime;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class IPCPackageNameFixer {

    public static void fixIpcTransact() {
        //android.os.Parcel#nativeWriteString
        XposedHelpers.findAndHookMethod(Parcel.class, "nativeWriteString", long.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String str = (String) param.args[1];
                if (str == null) {
                    return;
                }

                if (str.contains(ZeldaRuntime.sufferKey)) {
                    //需要被保留，不能被替换的场景 TODO 未来还需要优化
                    return;
                }
                if (ZeldaRuntime.declaredComponentClassNames.contains(str)) {
                    //此时不能替换，因为是className
                } else if (str.contains(ZeldaRuntime.originPackageName)) {
                    param.args[1] = str.replace(ZeldaRuntime.originPackageName, ZeldaRuntime.nowPackageName);
                    if (ZeldaRuntime.isZeldaDebugBuild) {
                        Log.i(Constants.TAG, "replace string from: " + str + "  to: " + param.args[1]);
                    }
                } else if (str.startsWith(Constants.zeldaKeep)) {
                    param.args[1] = str.substring(Constants.zeldaKeep.length());
                    if (ZeldaRuntime.isZeldaDebugBuild) {
                        Log.i(Constants.TAG, "replace string from: " + str + "  to: " + param.args[1]);
                    }
                }
            }
        });

        //android.os.Parcel.readString
        //android.os.Parcel#nativeReadString
        XC_MethodHook readStringReplacePackage = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String result = (String) param.getResult();
                if (result == null) {
                    return;
                }
                if (result.contains(ZeldaRuntime.nowPackageName)) {
                    param.setResult(result.replace(ZeldaRuntime.nowPackageName, ZeldaRuntime.originPackageName));
                }

            }
        };
        // RposedHelpers.findAndHookMethod(Parcel.class, "readString", readStringReplacePackage);
        XposedHelpers.findAndHookMethod(Parcel.class, "nativeReadString", long.class, readStringReplacePackage);


    }

    private static void backup() {
        //android.content.Intent.writeToParcel(Parcel out, int flags) {
        XposedHelpers.findAndHookMethod(Intent.class, "writeToParcel", Parcel.class, int.class, new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.thisObject;
                ComponentName component = intent.getComponent();
                if (component == null) {
                    return;
                }
                Log.i(Constants.TAG, "Intent.writeToParcel component: " + component);
                String className = component.getClassName();
                if (className.startsWith(".")) {
                    //reset to full className
                    className = component.getPackageName() + className;
                }
                ComponentName newComponent = new ComponentName(ZeldaRuntime.nowPackageName, Constants.zeldaKeep + className);
                intent.setComponent(newComponent);

                XposedBridge.invokeOriginalMethod(param.method, intent, param.args);
                intent.setComponent(component);
            }
        });
        //TODO hook不生效？还是就没有调用这个方法
        //android.content.ComponentName.writeToParcel(android.os.Parcel, int)
        //some relative path for intent component
        XposedHelpers.findAndHookMethod(ComponentName.class, "writeToParcel", Parcel.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ComponentName componentName = (ComponentName) param.thisObject;

                Parcel out = (Parcel) param.args[0];
                out.writeString(componentName.getPackageName());
                String className = componentName.getClassName();
                Log.i(Constants.TAG, "ComponentName.writeToParcel packageName:" + componentName.getPackageName() + " className: " + className);
                if (className.startsWith(".")) {
                    out.writeString(ZeldaRuntime.originPackageName + className);
                } else {
                    out.writeString(className);
                }

                param.setResult(true);
            }
        });
    }

}
