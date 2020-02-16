package com.virjar.zelda.engine;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.swift.sandhook.HookLog;
import com.swift.sandhook.SandHook;
import com.swift.sandhook.xposedcompat.XposedCompat;
import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.core.BuildConfig;
import com.virjar.zelda.engine.fixer.AppBindDataFixer;
import com.virjar.zelda.engine.fixer.AssertFixer;
import com.virjar.zelda.engine.fixer.IPCPackageNameFixer;
import com.virjar.zelda.engine.fixer.SignatureFixer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;
import mirror.dalvik.system.VMRuntime;

public class ZeldaRuntime {

    @SuppressLint("StaticFieldLeak")
    public static Context sContext;

    /**
     * call not use{@link Build.VERSION.SDK_INT} directly, android stupid proguard flows will remove this code
     */
    private static final int SDK_INIT = getSdkInt();

    public static boolean isZeldaDebugBuild = BuildConfig.DEBUG;

    /**
     * ActivityThread instance
     */
    public static Object mainThread;

    public static String processName;

    public static String originPackageName;

    public static String originApplicationName;

    public static String nowPackageName;

    public static String sufferKey;

    public static Object theLoadApk = null;

    public static Application realApplication = null;

    public static ApplicationInfo originApplicationInfo;

    public static boolean isHostPkgDebug = false;
    public static Set<String> declaredComponentClassNames = new HashSet<>();

    public static void callApplicationAttach(Context context) throws Exception {
        Log.i(Constants.TAG, "begin of zelda engine");
        if (SDK_INIT < SDK_VERSION_CODES.LOLLIPOP) {
            throw new RuntimeException("zelda framework now support before android 4.4 now");
        }

        if (SDK_INIT >= Build.VERSION_CODES.P) {
            HiddenAPIEnforcementPolicyUtils.passApiCheck();
        }

        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Log.e(Constants.TAG, "error for thread: " + t, e);
                if (defaultUncaughtExceptionHandler != null) {
                    defaultUncaughtExceptionHandler.uncaughtException(t, e);
                }
            }
        });

        sContext = context;

        originApplicationInfo = context.getApplicationInfo();

        //TODO 修正标记
        /**
         * Android O 及以上的 debug 模式会强制走解释器模式
         * 当 ART 发现你的方法已经被编译的时候，就不会走 CodeEntry
         * ArtInterpreterToInterpreterBridge 直接解释 CodeItem
         * 如果是debug模式，需要将method设置为native，关闭解释模式
         * ArtMethod::disableInterpreterForO
         */
        isHostPkgDebug = (originApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        // some resource need copy from apk assets to app runtime file directory
        ZeldaEnvironment.releaseZeldaResources();
        // some configuration data produced zelda framework work lifecycle,such as [zelda compile]、[zelda build]、[app compile],[app build]
        ZeldaConfig.init();
        restoreDeclaredComponent();

        //  Debug.waitForDebugger();
        Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", ClassLoader.getSystemClassLoader());

        mainThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");


        initSandHook();

        AssertFixer.fixApkSourceDir();

        //这是一个很重要的分割线
        NativeEngine.enableIORedirect();


        //这之后可以开始hook，和加载插件
        APIInitializer.initAPIConstants();


        //这之后所有文件系统目录结构进行了替换
        AppBindDataFixer.fixAppBindData();
        IPCPackageNameFixer.fixIpcTransact();
        SignatureFixer.fixSignature();


        //android.os.Debug.waitForDebugger();
        //TODO load xposed module


        if (SDK_INIT >= Build.VERSION_CODES.P) {
            HiddenAPIEnforcementPolicyUtils.reverseApiCheck();
        }

        Log.i(Constants.TAG, "zelda engine startup finish,call origin application");

        // create a new application instance
        if (originApplicationName != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Application> realClass =
                        (Class<? extends Application>) XposedHelpers.findClass(originApplicationName, context.getClassLoader());
                Constructor<? extends Application> constructor = realClass.getConstructor();
                realApplication = constructor.newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            realApplication = new Application();
        }

        XposedHelpers.setObjectField(mainThread, "mInitialApplication", realApplication);
        XposedHelpers.callMethod(realApplication, "attachBaseContext", context);


        // Debug.waitForDebugger();
        Log.i(Constants.TAG, "zelda attachBaseContext finished!");


    }

    private static void initSandHook() {
        SandHook.disableVMInline();
        //SandHook.tryDisableProfile(ZeldaRuntime.nowPackageName);
        SandHook.disableDex2oatInline(false);
        HookLog.DEBUG = false;
        XposedCompat.cacheDir = ZeldaEnvironment.sandHookCacheDir();
        XposedCompat.classLoader = sContext.getClassLoader();
    }

    public static void callApplicationOnCreate() {
        //mApplication的覆盖动作，必须放到onCreate调用的时候，因为 application的attach之后，才会对loadApk里面的mApplication赋值。
        XposedHelpers.setObjectField(theLoadApk, "mApplication", realApplication);

        Instrumentation instrumentation = (Instrumentation) XposedHelpers.getObjectField(
                mainThread, "mInstrumentation");
        instrumentation.callApplicationOnCreate(realApplication);
    }


    private static int getSdkInt() {
        try {
            Field sdkIntFiled = Build.VERSION.class.getField("SDK_INT");
            return (int) sdkIntFiled.get(null);
        } catch (Throwable throwable) {
            //ignore
            //throwable.printStackTrace();
            return Build.VERSION.SDK_INT;
        }

    }

    public static boolean is64bit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Process.is64Bit();
        }
        return VMRuntime.is64Bit.call(VMRuntime.getRuntime.call());

    }

    public static void restoreDeclaredComponent() {
        AssetManager assets = sContext.getAssets();
        InputStream inputStream;
        try {
            try {
                inputStream = assets.open(Constants.declaredComponentListConfig);
            } catch (FileNotFoundException fie) {
                //ignore
                return;
            }
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                declaredComponentClassNames.add(line);
            }
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e) {
            Log.e(Constants.TAG, "copy assets resource failed!!", e);
            throw new IllegalStateException(e);
        }
    }

}
