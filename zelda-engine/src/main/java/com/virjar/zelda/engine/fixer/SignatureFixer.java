package com.virjar.zelda.engine.fixer;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;

import com.virjar.zelda.engine.ZeldaRuntime;
import com.virjar.zelda.engine.fixer.pm.PackageParserEx;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class SignatureFixer {
    /**
     * 考虑使用hook方案过签名检测，原因是packageManager非常敏感，切很容易检测到代理痕迹。
     *
     * @return 返回是拦截成功。一般来说不应该失败。失败后暂时使用代理的方式替换服务
     * @see <a href="https://bbs.pediy.com/thread-250871.htm">检测ActivityManagerNative</a>
     */
    @SuppressLint("PrivateApi")
    public static void fixSignature() {
        XposedBridge.hookAllMethods(ZeldaRuntime.sContext.getPackageManager().getClass(), "getPackageInfo", new XC_MethodHook() {


            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                PackageInfo result = (PackageInfo) param.getResult();
                if (result == null) {
                    return;
                }

                String packageName = (String) param.args[0];

                if (result.signatures != null && ZeldaRuntime.originPackageName.equals(packageName)) {
                    Signature[] fakeSignature = PackageParserEx.getFakeSignatureForOwner();
                    if (fakeSignature != null) {
                        result.signatures = fakeSignature;
                    }
                }
            }
        });

    }


}
