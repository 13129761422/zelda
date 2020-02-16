package com.virjar.zelda.engine;

import android.app.Application;
import android.content.Context;

public class ZeldaApplication extends Application {
    private static boolean callAttachBaseContext = false;
    private static boolean callApplicationOnCreate = false;

    @Override
    protected void attachBaseContext(Context base) {
        if (callAttachBaseContext) {
            return;
        }
        //the ZeldaApplication will be create again after apk sourceDir IO relocation,
        //and we only call ZeldaRuntime.callApplicationAttach one times
        callAttachBaseContext = true;
        try {
            ZeldaRuntime.callApplicationAttach(base);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (callApplicationOnCreate) {
            return;
        }
        callApplicationOnCreate = true;
        ZeldaRuntime.callApplicationOnCreate();
    }
}
