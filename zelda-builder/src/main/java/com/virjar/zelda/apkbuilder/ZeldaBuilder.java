package com.virjar.zelda.apkbuilder;

import com.android.apksig.ApkSigner;
import com.android.apksig.apk.MinSdkVersionException;
import com.android.apksigner.PasswordRetriever;
import com.android.apksigner.SignerParams;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.virjar.zelda.buildsrc.Constants;

import net.dongliu.apk.parser.struct.ChunkType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.jf.pxb.android.axml.AxmlReader;
import org.jf.pxb.android.axml.AxmlVisitor;
import org.jf.pxb.android.axml.AxmlWriter;
import org.jf.pxb.googlecode.dex2jar.reader.io.ArrayDataIn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;

public class ZeldaBuilder {
    public static void main(String[] args) throws Exception {
        restoreConstants();
        Options options = new Options();
        options.addOption(new Option("w", "workdir", true, "set a zelda working dir"));
        options.addOption(new Option("t", "tell", false, "tell me the output apk file path"));
        options.addOption(new Option("h", "help", false, "print help message"));
        options.addOption(new Option("o", "output", true, "the output apk path or output project path"));
        options.addOption(new Option("s", "signature", false, "signature apk with zelda default KeyStore"));
        options.addOption(new Option("p", "package", true, "the new package name for output apk"));
        options.addOption(new Option("d", "debug", false, "add debuggable flag on androidManifest.xml "));

        DefaultParser parser = new DefaultParser();

        CommandLine cmd = parser.parse(options, args, false);

        if (cmd.hasOption('h')) {
            HelpFormatter hf = new HelpFormatter();
            hf.setWidth(110);
            hf.printHelp("Zelda", options);
            return;
        }

        if (cmd.hasOption('w')) {
            theWorkDir = new File(cmd.getOptionValue('w'));
        }


        ZeldaBuildContext zeldaBuildContext = ZeldaBuildContext.from(cmd.getArgList());

        //工作目录准备
        File outFile;
        if (cmd.hasOption('o')) {
            outFile = new File(cmd.getOptionValue('o'));
        } else {
            outFile = new File(
                    zeldaBuildContext.apkMeta.getPackageName() + "_" + zeldaBuildContext.apkMeta.getVersionName() + "_" + zeldaBuildContext.apkMeta.getVersionCode() + "_zelda.apk");
        }

        if (cmd.hasOption('t')) {
            System.out.println(outFile.getAbsolutePath());
            return;
        }


        System.out.println("zelda build param: " + Joiner.on(" ").join(args));

        zeldaBuildContext.zipOutputStream = new ZipOutputStream(outFile);
        zeldaBuildContext.cmd = cmd;
        zeldaBuildContext.genMeta();

        cleanWorkDir();

        File zeldaEngineApk = new File(theWorkDir, Constants.ZELDA_ENGINE_RESOURCE_APK_NAME);
        System.out.println("release zelda engine apk into: " + zeldaEngineApk.getAbsolutePath());
        copyAndClose(ZeldaBuilder.class.getClassLoader().getResourceAsStream(Constants.ZELDA_ENGINE_RESOURCE_APK_NAME), new FileOutputStream(zeldaEngineApk));


        //需要迁移的资源文件，这些需要在apk被安装到系统之前被系统识别，包括代码，lib，logo,apkName
        migrateBasicFile(zeldaBuildContext);

        //manifest 文件需要修改
        editManifest(zeldaBuildContext);

        //resources.arsc的package字段需要修复
        repairARSCPackageName(zeldaBuildContext);

        //植入代码，zelda外置so文件
        injectZeldaResource(zeldaBuildContext);

        //相关构建信息存储到特殊文件
        zeldaBuildContext.storeZeldaRPKGMeta();

        zeldaBuildContext.zipOutputStream.close();

        //签名
        System.out.println("the new apk file ：" + outFile.getAbsolutePath());

        if (cmd.hasOption('s')) {
            File signatureKeyFile = new File(theWorkDir, Constants.zeldaDefaultApkSignatureKey);
            System.out.println("release zelda default apk signature key into : " + signatureKeyFile.getAbsolutePath());
            copyAndClose(ZeldaBuilder.class.getClassLoader().getResourceAsStream(Constants.zeldaDefaultApkSignatureKey), new FileOutputStream(signatureKeyFile));

            //do not need signature apk if create a decompile project
            try {
                zipalign(outFile, theWorkDir);
                signatureApk(outFile, signatureKeyFile);
            } catch (Exception e) {
                // we need remove final output apk if sign failed,because of out shell think a illegal apk format file as task success flag,
                // android system think signed apk as really right apk,we can not install this apk on the android cell phone if rebuild success but sign failed
                FileUtils.forceDelete(outFile);
                throw e;
            }
        }

        System.out.println("clean working directory..");
        FileUtils.deleteDirectory(theWorkDir);
    }

    private static void zipalign(File outApk, File theWorkDir) throws IOException, InterruptedException {
        System.out.println("zip align output apk: " + outApk);
        //use different executed binary file with certain OS platforms
        String osName = System.getProperty("os.name").toLowerCase();
        String zipalignBinPath;
        boolean isLinux = false;
        if (osName.startsWith("Mac OS".toLowerCase())) {
            zipalignBinPath = "zipalign/mac/zipalign";
        } else if (osName.startsWith("Windows".toLowerCase())) {
            zipalignBinPath = "zipalign/windows/zipalign.exe";
        } else {
            zipalignBinPath = "zipalign/linux/zipalign";
            isLinux = true;
        }
        File unzipDestFile = new File(theWorkDir, zipalignBinPath);
        unzipDestFile.getParentFile().mkdirs();
        copyAndClose(ZeldaBuilder.class.getClassLoader().getResourceAsStream(zipalignBinPath), new FileOutputStream(unzipDestFile));
        if (isLinux) {
            String libCPlusPlusPath = "zipalign/linux/lib64/libc++.so";
            File libCPlusPlusFile = new File(theWorkDir, libCPlusPlusPath);
            libCPlusPlusFile.getParentFile().mkdirs();
            copyAndClose(ZeldaBuilder.class.getClassLoader().getResourceAsStream(libCPlusPlusPath), new FileOutputStream(libCPlusPlusFile));
        }

        unzipDestFile.setExecutable(true);

        File tmpOutputApk = File.createTempFile("zipalign", ".apk");
        tmpOutputApk.deleteOnExit();

        String command = unzipDestFile.getAbsolutePath() + " -f  4 " + outApk.getAbsolutePath() + " " + tmpOutputApk.getAbsolutePath();
        System.out.println("zip align apk with command: " + command);

        String[] envp = new String[]{"LANG=zh_CN.UTF-8", "LANGUAGE=zh_CN.UTF-8"};
        Process process = Runtime.getRuntime().exec(command, envp, null);
        autoFillBuildLog(process.getInputStream(), "zipalign-stand");
        autoFillBuildLog(process.getErrorStream(), "zipalign-error");

        process.waitFor();

        Files.move(
                tmpOutputApk.toPath(), outApk.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println(outApk.getAbsolutePath() + " has been zipalign ");
    }

    private static void autoFillBuildLog(InputStream inputStream, String type) {
        new Thread("read-" + type) {
            @Override
            public void run() {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(type + " : " + line);
                    }
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private static void signatureApk(File outApk, File keyStoreFile) throws Exception {
        System.out.println("auto sign apk with zelda KeyStore");
        File tmpOutputApk = File.createTempFile("apksigner", ".apk");
        tmpOutputApk.deleteOnExit();

        SignerParams signerParams = new SignerParams();
        signerParams.setKeystoreFile(keyStoreFile.getAbsolutePath());
        signerParams.setKeystoreKeyAlias("hermes");
        signerParams.setKeystorePasswordSpec("pass:hermes");
        //signerParams.setKeyPasswordSpec("hermes");


        signerParams.setName("zeldaSign");
        PasswordRetriever passwordRetriever = new PasswordRetriever();
        signerParams.loadPrivateKeyAndCerts(passwordRetriever);

        String v1SigBasename;
        if (signerParams.getV1SigFileBasename() != null) {
            v1SigBasename = signerParams.getV1SigFileBasename();
        } else if (signerParams.getKeystoreKeyAlias() != null) {
            v1SigBasename = signerParams.getKeystoreKeyAlias();
        } else if (signerParams.getKeyFile() != null) {
            String keyFileName = new File(signerParams.getKeyFile()).getName();
            int delimiterIndex = keyFileName.indexOf('.');
            if (delimiterIndex == -1) {
                v1SigBasename = keyFileName;
            } else {
                v1SigBasename = keyFileName.substring(0, delimiterIndex);
            }
        } else {
            throw new RuntimeException(
                    "Neither KeyStore key alias nor private key file available");
        }

        ApkSigner.SignerConfig signerConfig =
                new ApkSigner.SignerConfig.Builder(
                        v1SigBasename, signerParams.getPrivateKey(), signerParams.getCerts())
                        .build();

        ApkSigner.Builder apkSignerBuilder =
                new ApkSigner.Builder(Lists.newArrayList(signerConfig))
                        .setInputApk(outApk)
                        .setOutputApk(tmpOutputApk)
                        .setOtherSignersSignaturesPreserved(false)
                        .setV1SigningEnabled(true)
                        .setV2SigningEnabled(true)
                        .setV3SigningEnabled(true)
                        //这个需要设置为true，否则debug版本的apk无法签名
                        .setDebuggableApkPermitted(true);
        // .setSigningCertificateLineage(lineage);
        ApkSigner apkSigner = apkSignerBuilder.build();
        try {
            apkSigner.sign();
        } catch (MinSdkVersionException e) {
            System.out.println("Failed to determine APK's minimum supported platform version,use default skd api level 21");
            apkSigner = apkSignerBuilder.setMinSdkVersion(21).build();
            apkSigner.sign();
        }

        Files.move(
                tmpOutputApk.toPath(), outApk.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println(outApk.getAbsolutePath() + " has been Signed");
    }


    public static void injectZeldaResource(ZeldaBuildContext zeldaBuildContext) throws IOException {

        ZipFile zeldaEngineApkFile = new ZipFile(new File(theWorkDir, Constants.ZELDA_ENGINE_RESOURCE_APK_NAME));
        Enumeration<ZipEntry> entries = zeldaEngineApkFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String entryName = zipEntry.getName();

            if (entryName.startsWith("classes") && (Util.classesIndexPattern.matcher(entryName).matches() || entryName.equals("classes.dex"))) {
                // copy dex
                zeldaBuildContext.zipOutputStream.putNextEntry(new ReNameEntry(zipEntry, "classes" + zeldaBuildContext.dexAppendIndex + ".dex"));
                zeldaBuildContext.zipOutputStream.write(IOUtils.toByteArray(zeldaEngineApkFile.getInputStream(zipEntry)));
                zeldaBuildContext.dexAppendIndex++;
            } else if (entryName.startsWith("lib/")) {
                List<String> pathSegment = Splitter.on("/").splitToList(entryName);
                String arch = pathSegment.get(1);
                if (zeldaBuildContext.orignApkSupportArch.contains(arch) || zeldaBuildContext.orignApkSupportArch.isEmpty()) {
                    zeldaBuildContext.zipOutputStream.putNextEntry(new ZipEntry(zipEntry));
                    zeldaBuildContext.zipOutputStream.write(IOUtils.toByteArray(zeldaEngineApkFile.getInputStream(zipEntry)));
                } else if (zeldaBuildContext.orignApkSupportArch.contains("armeabi") && arch.equals("armeabi-v7a")) {
                    // 如果没有armeabi-v7a，那么转化到armeabi上面
                    zeldaBuildContext.zipOutputStream.putNextEntry(new ZipEntry("lib/armeabi/" + pathSegment.get(2)));
                    zeldaBuildContext.zipOutputStream.write(IOUtils.toByteArray(zeldaEngineApkFile.getInputStream(zipEntry)));
                }
            }


        }
        zeldaEngineApkFile.close();
    }

    private static final int RES_TABLE_PACKAGE_TYPE = 0x0200;

    private static void repairARSCPackageName(ZeldaBuildContext zeldaBuildContext) throws IOException {
        ZipEntry zipEntry = zeldaBuildContext.targetApkZipFile.getEntry(Constants.CONSTANTS_RESOURCES_ARSC);

        zeldaBuildContext.zipOutputStream.putNextEntry(new ZipEntry(zipEntry));
        System.out.println("edit resources.arsc entry");
        byte[] bytes = IOUtils.toByteArray(zeldaBuildContext.targetApkZipFile.getInputStream(zipEntry));
        ArrayDataIn dataIn = ArrayDataIn.le(bytes);

        while (true) {
            int currentPosition = dataIn.getCurrentPosition();

            int type = dataIn.readUShortx();
            int headerSize = dataIn.readUShortx();
            int chunkSize = dataIn.readUIntx();

            if (type == ChunkType.TABLE) {
                //对于table类型的chunk来说，chunkSize代表了整个文件大小，也就是说ChunkType.TABLE类型的chunk是一个chunk容器
                dataIn.move(currentPosition + headerSize);
                continue;
            }

            if (type != RES_TABLE_PACKAGE_TYPE) {
                dataIn.move(currentPosition + chunkSize);
                continue;
            }


            //TODO 如果有多个package
            //一般为 127
            int packageId = dataIn.readUIntx();

            int packageBeginOffset = dataIn.getCurrentPosition();
            byte[] newPackageData = zeldaBuildContext.newPkgName.getBytes(StandardCharsets.UTF_16LE);
            if (newPackageData.length > 254) {
                throw new IllegalStateException("package too long: " + zeldaBuildContext.newPkgName);
            }

            System.arraycopy(newPackageData, 0, bytes, packageBeginOffset, newPackageData.length);

            for (int i = newPackageData.length; i < 256; i++) {
                if (bytes[packageBeginOffset + i] != 0) {
                    bytes[packageBeginOffset + i] = 0;
                } else {
                    break;
                }
            }
            zeldaBuildContext.zipOutputStream.write(bytes);
            break;
        }

    }

    private static void editManifest(ZeldaBuildContext zeldaBuildContext) throws IOException {
        ZipEntry zipEntry = zeldaBuildContext.targetApkZipFile.getEntry(Constants.manifestFileName);

        zeldaBuildContext.zipOutputStream.putNextEntry(new ZipEntry(zipEntry));
        System.out.println("edit androidManifest.xml entry");

        zeldaBuildContext.zipOutputStream.write(
                editManifestWithAXmlEditor(
                        IOUtils.toByteArray(zeldaBuildContext.targetApkZipFile.getInputStream(zipEntry))
                        , zeldaBuildContext)
        );

    }

    private static byte[] editManifestWithAXmlEditor(byte[] manifestFileData, ZeldaBuildContext zeldaBuildContext) throws IOException {

        AxmlReader rd = new AxmlReader(manifestFileData);
        AxmlWriter wr = new AxmlWriter();

        AxmlVisitor axmlVisitor = wr;

        if (zeldaBuildContext.cmd.hasOption('d')) {
            axmlVisitor = new ManifestHandlers.EnableDebug(axmlVisitor);
        }
        axmlVisitor = new ManifestHandlers.ReplaceApplication(axmlVisitor, zeldaBuildContext);
        axmlVisitor = new ManifestHandlers.FixRelativeClassName(axmlVisitor, zeldaBuildContext);
        axmlVisitor = new ManifestHandlers.ReNameProviderAuthorities(axmlVisitor, zeldaBuildContext);
        axmlVisitor = new ManifestHandlers.ReNamePermissionDeclare(axmlVisitor, zeldaBuildContext);

        rd.accept(axmlVisitor);
        return wr.toByteArray();
    }

    private static void migrateBasicFile(ZeldaBuildContext zeldaBuildContext) throws IOException {

        Enumeration<ZipEntry> entries = zeldaBuildContext.targetApkZipFile.getEntries();
        int maxIndex = 1;

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String entryName = zipEntry.getName();

            if (entryName.startsWith("classes") && entryName.endsWith(".dex") && !entryName.contains("/")) {
                if (!entryName.equals("classes.dex")) {
                    Matcher matcher = Util.classesIndexPattern.matcher(entryName);
                    if (!matcher.matches()) {
                        continue;
                    }
                    int nowIndex = NumberUtils.toInt(matcher.group(1));
                    if (nowIndex > maxIndex) {
                        maxIndex = nowIndex;
                    }
                }

                //dex
                copyEntry(zipEntry, zeldaBuildContext.targetApkZipFile, zeldaBuildContext.zipOutputStream, zeldaBuildContext.buffer);
            } else if (!entryName.startsWith("META-INF/") && !entryName.equals(Constants.manifestFileName) && !entryName.equals(Constants.CONSTANTS_RESOURCES_ARSC)) {
                copyEntry(zipEntry, zeldaBuildContext.targetApkZipFile, zeldaBuildContext.zipOutputStream, zeldaBuildContext.buffer);
            }
        }
        maxIndex++;
        zeldaBuildContext.dexAppendIndex = maxIndex;

        Util.copyAssets(zeldaBuildContext.zipOutputStream, zeldaBuildContext.targetApkFile, Constants.ZELDA_ORIGIN_APK_NAME);
    }


    private static void copyEntry(ZipEntry zipEntry, ZipFile zipFile, ZipOutputStream zipOutputStream, byte[] buffer) throws IOException {
        zipOutputStream.putNextEntry(zipEntry);
        IOUtils.copyLarge(
                zipFile.getInputStream(zipEntry),
                zipOutputStream,
                buffer
        );
    }


    private static File theWorkDir = null;

    private static File workDir() {
        if (theWorkDir == null) {
            theWorkDir = new File("zelda_work_dir");
        }
        return theWorkDir;
    }

    private static void cleanWorkDir() {
        File workDir = workDir();
        FileUtils.deleteQuietly(workDir);
        try {
            FileUtils.forceMkdir(workDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyAndClose(final InputStream input, final OutputStream output) throws IOException {
        IOUtils.copy(input, output);
        input.close();
        output.close();
    }


    private static void restoreConstants() throws Exception {
        InputStream inputStream = ZeldaBuilder.class.getClassLoader().getResourceAsStream(Constants.ZELDA_CONFIG_PROPERTIES);
        if (inputStream == null) {
            return;
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        inputStream.close();

        for (Field field : Constants.class.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            String value = properties.getProperty(Constants.ZELDA_CONSTANTS_PREFIX + field.getName());
            if (value == null) {
                continue;
            }

            Object castValue = Util.primitiveCast(value, field.getType());

            if (castValue == null) {
                continue;
            }

            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            field.set(null, castValue);
        }
    }
}
