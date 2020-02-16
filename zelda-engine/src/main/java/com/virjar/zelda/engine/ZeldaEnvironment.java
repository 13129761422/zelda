package com.virjar.zelda.engine;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.engine.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * 各种目录定义
 */
public class ZeldaEnvironment {

    /**
     * zelda资源目录，存储和虚拟环境无关的，zelda框架资源。如zelda编译序列号。zelda配置文件，zelda颁发证书，mock签名
     * 指向: /data/data/pkg/app_zelda_resource
     *
     * @return zelda运行框架资源
     */
    public static File zeldaResourceDir() {
        return makeSureDirExist(ZeldaRuntime.sContext.getDir("zelda_resource", Context.MODE_PRIVATE));
    }


    public static File zeldaConfigFile() {
        return new File(zeldaResourceDir(), Constants.zeldaConfigFileName);
    }

    public static File originApkDir() {
        return new File(zeldaResourceDir(), Constants.ZELDA_ORIGIN_APK_NAME);
    }


    public static void releaseZeldaResources() {
        releaseAssetResource(Constants.zeldaConfigFileName);
        releaseAssetResource(Constants.ZELDA_ORIGIN_APK_NAME);
    }


    private static void releaseAssetResource(String name) {
        releaseAssetResource(name, ZeldaRuntime.isZeldaDebugBuild);
    }

    private static void releaseAssetResource(String name, boolean force) {
        File destinationFileName = new File(zeldaResourceDir(), name);
        //测试环境下，每次强刷代码，否则可能导致代码没有更新
        if (destinationFileName.exists()) {
            return;
        }
        File parentDir = destinationFileName.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        synchronized (ZeldaEnvironment.class) {
            if (destinationFileName.exists() && !force) {
                return;
            }

            AssetManager assets = ZeldaRuntime.sContext.getAssets();
            InputStream inputStream;
            try {
                try {
                    inputStream = assets.open(name);
                } catch (FileNotFoundException fie) {
                    //ignore
                    return;
                }
                if (ZeldaRuntime.isZeldaDebugBuild) {
                    Log.i(Constants.TAG, "copy resource: " + destinationFileName);
                }
                IOUtils.copy(inputStream, new FileOutputStream(destinationFileName));
            } catch (IOException e) {
                Log.e(Constants.TAG, "copy assets resource failed!!", e);
                throw new IllegalStateException(e);
            }
        }

    }

    private static File makeSureDirExist(File file) {
        if (file.exists() && file.isFile()) {
            return file;
        }
        file.mkdirs();
        return file;
    }


    public static File nativeCacheDir() {
        return makeSureDirExist(new File(zeldaResourceDir(), Constants.nativeCacheDir));
    }

    public static File sandHookCacheDir() {
        return makeSureDirExist(new File(zeldaResourceDir(), Constants.sandHookCache));
    }

    public static File originAPKSignatureFile() {
        return new File(zeldaResourceDir(), "signature.ini");
    }
}
