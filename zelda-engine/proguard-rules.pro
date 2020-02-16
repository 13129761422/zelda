# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 这个暂时加上，测试没问题的时候关闭他
-keepattributes SourceFile,LineNumberTable

#这是因为，我们的class需要注入到apk中，如果单纯混淆，有一定可能导致和原生app的混淆重名，这会导致重复类发生
-flattenpackagehierarchy com.virjar.zelda.other

# lody的反射工具类，使用名称反射的方式。需要keep
-keepclassmembernames class mirror.** {
    public static <fields>;
}

-keepclassmembers class mirror.*{*;}

# xposed作为插件API，需要keep
-keep class de.robv.android.xposed.**{*;}

-keep class external.org.apache.commons.**{*;}


-keep class android.content.pm.**{*;}
-keep class com.virjar.zelda.engine.ZeldaApplication{*;}

-keep class com.swift.sandhook.**{*;}

#-keep class com.swift.sandhook.xposedcompat.hookstub.MethodHookerStubs32{*;}
#-keep class com.swift.sandhook.xposedcompat.hookstub.MethodHookerStubs64{*;}
#-keep class com.swift.sandhook.xposedcompat.hookstub.HookStubManager{*;}
#-keep class com.swift.sandhook.xposedcompat.RposedAdditionalHookInfo{*;}
#-keep class com.swift.sandhook.xposedcompat.utils.DexLog{*;}
#
#
#-keep class com.virjar.zelda.api.**{*;}
#
#
#-keepclassmembers class com.swift.sandhook.SandHook{
#    native <methods>;
#    public static int testAccessFlag;
#    public static long getThreadId();
#}
#-keepclassmembers class com.swift.sandhook.ClassNeverCall{*;}
#-keepclassmembers class com.swift.sandhook.ArtMethodSizeTest{*;}
#-keepclassmembers class com.swift.sandhook.SandHookMethodResolver{*;}
#-keepclassmembers class com.swift.sandhook.wrapper.BackupMethodStubs{*;}
#-keepclassmembers class com.swift.sandhook.SandHookConfig{
#    public volatile static boolean compiler;
#}
#-keepclassmembers class com.swift.sandhook.PendingHookHandler{
#    public static void onClassInit(long);
#}




#需要混淆
-keep class com.virjar.zelda.engine.NativeEngine{*;}






