package com.virjar.zelda.engine.fixer;

import com.virjar.zelda.engine.NativeEngine;
import com.virjar.zelda.engine.ZeldaEnvironment;
import com.virjar.zelda.engine.ZeldaRuntime;

import java.io.File;
import java.io.IOException;

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
    }

}
