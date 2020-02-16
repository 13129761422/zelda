package com.virjar.zelda.buildsrc;

public class Constants {

    public static String TAG = "Zelda";

    public static String ZeldaApplicationClassName = "com.virjar.zelda.engine.ZeldaApplication";
    public static String ORIGIN_APPLICATION_CLASS_NAME = "ZELDA_ORIGIN_APPLICATION_CLASS_NAME";
    public static String zeldaConfigFileName = "zeldaConfig.properties";

    public static String KEY_ORIGIN_PKG_NAME = "originPackageName";
    public static String KEY_ORIGIN_APPLICATION_NAME = "originApplicationName";
    public static String KEY_NEW_PKG_NAME = "ZeldaNewPackageName";
    public static String kEY_SUFFER_KEY = "zeldaSufferKey";


    public static String ZELDA_ENGINE_RESOURCE_APK_NAME = "zelda-engine.apk";
    public static String ZELDA_ORIGIN_APK_NAME = "zelda-origin.apk";
    public static String zeldaPrefix = "zelda_";
    public static String KEY_ZELDA_BUILD_SERIAL = zeldaPrefix + "serialNo";
    public static String KEY_ZELDA_BUILD_TIMESTAMP = zeldaPrefix + "buildTimestamp";

    public static String zeldaDefaultApkSignatureKey = "hermes_key";

    public static String ZELDA_NATIVE_LIB_NAME = "zeldanative";


    public static String CONSTANTS_RESOURCES_ARSC = "resources.arsc";

    public static String manifestFileName = "AndroidManifest.xml";
    public static String nativeCacheDir = "nativeCache";

    /**
     * 是否是开发模式，此时不会进行代码优化、代码混淆、c++符号保留，生成的app的sufferKey为固定值等
     */
    public static boolean devBuild = false;


    public static final String ZELDA_CONFIG_PROPERTIES = "zelda_config.properties";

    public static final String ZELDA_CONSTANTS_PREFIX = "zelda_constant.";
    public static String sandHookCache = "sandHookCache";

    public static final String zeldaKeep = "zeldaKeep.";

    public static final String declaredComponentListConfig = "zelda.declaredComponent.txt";
}
