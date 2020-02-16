package com.virjar.zelda.engine;


import android.os.Build;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.engine.util.BuildCompat;

import java.io.File;

public class NativeEngine {
    private static final String TAG = NativeEngine.class.getSimpleName();


    static {
        try {
            System.loadLibrary(Constants.ZELDA_NATIVE_LIB_NAME);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }


    public static String getRedirectedPath(String origPath) {
        try {
            return nativeGetRedirectedPath(origPath);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
        return origPath;
    }

    public static String resverseRedirectedPath(String origPath) {
        try {
            return nativeReverseRedirectedPath(origPath);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
        return origPath;
    }

    public static void redirectDirectory(String origPath, String newPath) {
        if (!origPath.endsWith("/")) {
            origPath = origPath + "/";
        }
        if (!newPath.endsWith("/")) {
            newPath = newPath + "/";
        }
        try {
            nativeIORedirect(origPath, newPath);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }

    public static void redirectFile(String origPath, String newPath) {
        if (origPath.endsWith("/")) {
            origPath = origPath.substring(0, origPath.length() - 1);
        }
        if (newPath.endsWith("/")) {
            newPath = newPath.substring(0, newPath.length() - 1);
        }

        try {
            nativeIORedirect(origPath, newPath);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }

    public static void whitelist(String path) {
        try {
            nativeIOWhitelist(path);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }

    public static void forbid(String path) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        try {
            nativeIOForbid(path);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }

    public static void enableIORedirect() {
        try {
            String soPath = new File(ZeldaRuntime.originApplicationInfo.nativeLibraryDir, "lib" + Constants.ZELDA_NATIVE_LIB_NAME + ".so").getAbsolutePath();
            if (!new File(soPath).exists()) {
                throw new RuntimeException("Unable to find the so.");
            }
            nativeEnableIORedirect(soPath, Build.VERSION.SDK_INT, BuildCompat.getPreviewSDKInt(), ZeldaRuntime.originPackageName, ZeldaRuntime.nowPackageName);
        } catch (Throwable e) {
            Log.e(TAG, "error", e);
        }
    }


    public static void onKillProcess(int pid, int signal) {
        Log.e(TAG, "killProcess: pid = " + pid + ", signal = " + signal + ".");
        if (pid == android.os.Process.myPid()) {
            Log.e(TAG, "stack trace", new Throwable());
        }
    }


    private static native String nativeReverseRedirectedPath(String redirectedPath);

    private static native String nativeGetRedirectedPath(String orgPath);

    private static native void nativeIORedirect(String origPath, String newPath);

    private static native void nativeIOWhitelist(String path);

    private static native void nativeIOForbid(String path);

    private static native void nativeEnableIORedirect(String selfSoPath, int apiLevel,
                                                      int previewApiLevel,
                                                      String originPkg, String nowPkg);


}
