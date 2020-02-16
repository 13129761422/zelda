package com.virjar.zelda.engine.fixer.pm;


import android.content.pm.PackageParser;
import android.content.pm.Signature;
import android.os.Parcel;
import android.util.Log;

import com.virjar.zelda.buildsrc.Constants;
import com.virjar.zelda.engine.ZeldaEnvironment;
import com.virjar.zelda.engine.util.BuildCompat;
import com.virjar.zelda.engine.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * @author Lody
 */

public class PackageParserEx {

    private static final String TAG = Constants.TAG;
    private static Signature[] mSignature = null;


    public static Signature[] getFakeSignatureForOwner() {
        Signature[] signatures = mSignature;
        if (signatures != null) {
            return signatures;
        }
        signatures = readSignature();
        if (signatures != null) {
            return signatures;
        }

        try {
            Signature[] fakeSignatureInternal = getFakeSignatureInternal(ZeldaEnvironment.originApkDir());
            mSignature = fakeSignatureInternal;
            savePackageCache(fakeSignatureInternal);
            return fakeSignatureInternal;
        } catch (Throwable throwable) {
            Log.e(TAG, "签名读取失败", throwable);
            throw new RuntimeException(throwable);
        }
    }

    private static Signature[] getFakeSignatureInternal(File packageFile) throws Throwable {
        PackageParser parser = PackageParserCompat.createParser(packageFile);
        PackageParser.Package p = PackageParserCompat.parsePackage(parser, packageFile, 0);
        if (p.requestedPermissions.contains("android.permission.FAKE_PACKAGE_SIGNATURE")
                && p.mAppMetaData != null
                && p.mAppMetaData.containsKey("fake-signature")) {
            String sig = p.mAppMetaData.getString("fake-signature");
            p.mSignatures = new Signature[]{new Signature(sig)};
            Log.i(TAG, "Using fake-signature feature on : " + p.packageName);
        } else {
            PackageParserCompat.collectCertificates(parser, p, PackageParser.PARSE_IS_SYSTEM);
        }
        if (BuildCompat.isPie()) {
            return p.mSigningDetails.signatures;
        } else {
            return p.mSignatures;
        }
    }


    private static Signature[] readSignature() {
        File signatureFile = ZeldaEnvironment.originAPKSignatureFile();
        if (!signatureFile.exists()) {
            return null;
        }
        Parcel p = Parcel.obtain();
        try {
            FileInputStream fis = new FileInputStream(signatureFile);
            byte[] bytes = IOUtils.toByteArray(fis);
            fis.close();
            p.unmarshall(bytes, 0, bytes.length);
            p.setDataPosition(0);
            return p.createTypedArray(Signature.CREATOR);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }
        return null;
    }


    private static void savePackageCache(Signature[] signatures) {
        if (signatures == null) {
            return;
        }
        File signatureFile = ZeldaEnvironment.originAPKSignatureFile();
        if (signatureFile.exists() && !signatureFile.delete()) {
            Log.w(TAG, "Unable to delete the signatures  file:  " + signatureFile);
        }
        Parcel p = Parcel.obtain();
        try {
            p.writeTypedArray(signatures, 0);
            writeParcelToFile(p, signatureFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            p.recycle();
        }

    }

    private static void writeParcelToFile(Parcel p, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(p.marshall());
        fos.close();
    }


}
