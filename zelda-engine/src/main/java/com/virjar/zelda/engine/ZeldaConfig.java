package com.virjar.zelda.engine;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import external.org.apache.commons.lang3.StringUtils;

public class ZeldaConfig {
    private static Properties properties = null;
    private static File theConfigFile = null;
    private static long lastUpdate = 0;

    public static void init() throws IOException {
        theConfigFile = ZeldaEnvironment.zeldaConfigFile();
        properties = new Properties();
        FileInputStream fileInputStream = new FileInputStream(theConfigFile);
        properties.load(fileInputStream);
        fileInputStream.close();
        lastUpdate = theConfigFile.lastModified();

        ZeldaRuntime.originPackageName = getConfig(Constants.KEY_ORIGIN_PKG_NAME);
        ZeldaRuntime.originApplicationName = getConfig(Constants.KEY_ORIGIN_APPLICATION_NAME);
        ZeldaRuntime.nowPackageName = getConfig(Constants.KEY_NEW_PKG_NAME);
        ZeldaRuntime.sufferKey = getConfig(Constants.kEY_SUFFER_KEY);
    }

    public static void loadConfig(ApplicationInfo applicationInfo) {
        Bundle metaData = applicationInfo.metaData;
        Set<String> strings = metaData.keySet();
        boolean hasSetValue = false;
        for (String key : strings) {
            if (properties.containsKey(key)) {
                //TODO log warning
                continue;
            }

            Object value = metaData.get(key);
            if (value instanceof String) {
                String propertiesValue = (String) value;
                properties.setProperty(key, propertiesValue);
            } else if (value == null) {
                properties.setProperty(key, null);
            } else if (value instanceof Number) {
                Number number = (Number) value;
                String propertiesValue = String.format(Locale.CHINESE, "%.0f", number.doubleValue());
                properties.setProperty(key, propertiesValue);
            } else {
                properties.setProperty(key, String.valueOf(value));
            }
            hasSetValue = true;
        }
        if (hasSetValue) {
            save();
        }
    }

    public static String getConfig(String key) {
        if (lastUpdate < theConfigFile.lastModified()) {
            reload();
        }
        return properties.getProperty(key);
    }

    private static void reload() {
        try (FileInputStream fileInputStream = new FileInputStream(theConfigFile)) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            Log.w(Constants.TAG, "reload properties failed ,read file: " + theConfigFile.getAbsolutePath(), e);
            throw new IllegalStateException(e);
        }
    }

    public static void setConfig(String key, String value) {
        if (StringUtils.equals(getConfig(key), value)) {
            return;
        }
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
        save();
    }

    private static void save() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(theConfigFile)) {
            properties.store(fileOutputStream, "auto saved by zelda");
        } catch (IOException e) {
            Log.e(Constants.TAG, "save config failed", e);
            throw new IllegalStateException(e);
        }
    }
}

