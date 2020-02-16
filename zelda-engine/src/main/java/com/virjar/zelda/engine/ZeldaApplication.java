package com.virjar.zelda.engine;

import android.app.Application;
import android.content.Context;

public class ZeldaApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
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
        ZeldaRuntime.callApplicationOnCreate();
    }
}
