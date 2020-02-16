package com.virjar.zelda.engine.fixer;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.engine.NativeEngine;
import com.virjar.zelda.engine.ZeldaEnvironment;
import com.virjar.zelda.engine.ZeldaRuntime;

import java.io.File;
import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import mirror.android.app.ActivityThread;

/**
 * 修复媒资管理器相关，涉及 ResourceManager 和 AssertManager
 */
public class AssertFixer {

    public static void fixApkSourceDir() throws IOException {
        String sourceAPKDir = ZeldaRuntime.originApplicationInfo.sourceDir;
        File file = ZeldaEnvironment.originApkDir();
        String sourceCanonicalPath = new File(sourceAPKDir).getCanonicalPath();
        String destCanonicalPath = file.getCanonicalPath();
        // /data/app/com.tencent.mm/base.apk -> /data/data/zelda.comtentcentmm/xxx/file/zelda_resource/origin_apk.apk
        // and
        // /data/app/zelda.comtentcentmm/base.apk -> /data/data/zelda.comtentcentmm/xxx/file/zelda_resource/origin_apk.apk
        NativeEngine.redirectFile(sourceCanonicalPath, destCanonicalPath);
        NativeEngine.redirectFile(sourceCanonicalPath.replace(ZeldaRuntime.nowPackageName, ZeldaRuntime.originPackageName), destCanonicalPath);

        Object mBoundApplication = ActivityThread.mBoundApplication.get(ZeldaRuntime.mainThread);
        if (mBoundApplication != null) {
            Object loadApk = XposedHelpers.getObjectField(mBoundApplication, "info");
            Resources resources = (Resources) XposedHelpers.getObjectField(loadApk, "mResources");
            final AssetManager assetManager = resources.getAssets();

            XposedHelpers.findAndHookMethod(AssetManager.class, "isUpToDate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject.equals(assetManager)) {
                        //prevent create new loadApk | application
                        param.setResult(true);
                    }
                }
            });
        } else {
            Log.w(Constants.TAG, "can not find mBoundApplication from ActivityThread!!");
        }
    }

}
