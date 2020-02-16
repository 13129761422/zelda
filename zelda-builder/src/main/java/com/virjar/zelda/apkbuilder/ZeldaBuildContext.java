package com.virjar.zelda.apkbuilder;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.virjar.zelda.buildsrc.Constants;

import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ZeldaBuildContext {
    File targetApkFile;
    ApkMeta apkMeta;
    ZipOutputStream zipOutputStream;
    ZipFile targetApkZipFile;

    int dexAppendIndex = -1;

    CommandLine cmd;

    String newPkgName = null;
    String originApplicationClass;
    Document originApplicationManifestDoc;

    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

    Set<String> orignApkSupportArch;
    String sufferKey;
    Set<String> declaredComponentClassNames = new HashSet<>();

    public static ZeldaBuildContext from(List<String> argList) throws Exception {
        if (argList.size() == 0) {
            throw new IllegalStateException("need pass apk file");
        }
        if (argList.size() != 1) {
            throw new IllegalStateException("can not recognize args: " + StringUtils.join(argList));
        }
        ApkMeta originApkMeta;
        ZeldaBuildContext zeldaBuildContext = new ZeldaBuildContext();
        File apkFileFile = new File(argList.get(0));
        try (ApkFile apkFile = new ApkFile(apkFileFile)) {
            originApkMeta = apkFile.getApkMeta();

            String originAPKManifestXml = apkFile.getManifestXml();
            zeldaBuildContext.originApplicationManifestDoc = loadDocument(new ByteArrayInputStream(originAPKManifestXml.getBytes()));
        }


        zeldaBuildContext.targetApkFile = apkFileFile;
        zeldaBuildContext.apkMeta = originApkMeta;

        zeldaBuildContext.targetApkZipFile = new ZipFile(apkFileFile);

        return zeldaBuildContext;
    }

    void genMeta() throws IOException {
        if (cmd.hasOption('p')) {
            newPkgName = cmd.getOptionValue('p');
        }
        if (apkMeta.getPackageName().equals(newPkgName)) {
            throw new IllegalStateException("you should set a new package name,not: " + apkMeta.getPackageName());
        }
        if (Constants.devBuild) {
            sufferKey = "debug";
        } else {
            sufferKey = Util.randomChars(8);
        }
        sufferKey = "zElDa." + sufferKey;
        if (StringUtils.isBlank(newPkgName)) {
            newPkgName = "virjar.zelda."
                    + StringUtils.substring(apkMeta.getPackageName().replaceAll("\\.", ""), 0, 20)
                    + "."
                    + sufferKey;
        }

        //解析原始apk的applicationName
        Element applicationElement = (Element) originApplicationManifestDoc.getElementsByTagName("application").item(0);
        originApplicationClass = applicationElement.getAttribute("android:name");
        if (StringUtils.isBlank(originApplicationClass)) {
            originApplicationClass = applicationElement.getAttribute("name");
        }
        if (StringUtils.startsWith(originApplicationClass, ".")) {
            originApplicationClass = apkMeta.getPackageName() + originApplicationClass;
        }
        if ("android.app.Application".equals(originApplicationClass)) {
            //reset if use android default application class
            originApplicationClass = null;
        }

        orignApkSupportArch = calculateAPKSupportArch(targetApkFile);
    }

    private static Set<String> calculateAPKSupportArch(File originAPK) throws IOException {
        Set<String> ret = Sets.newHashSet();
        //ret.add("armeabi");
        //ret.add("armeabi-v7a");

        ZipFile zipFile = new ZipFile(originAPK);
        Enumeration<ZipEntry> entries = zipFile.getEntries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.getName().startsWith("lib/")) {
                List<String> pathSegment = Splitter.on("/").splitToList(zipEntry.getName());
                ret.add(pathSegment.get(1));
            }

        }
        zipFile.close();

        return ret;
    }

    private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
    private static final String FEATURE_LOAD_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String FEATURE_DISABLE_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";


    public static Document loadDocument(InputStream file)
            throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setFeature(FEATURE_DISABLE_DOCTYPE_DECL, true);
        docFactory.setFeature(FEATURE_LOAD_DTD, false);

        try {
            docFactory.setAttribute(ACCESS_EXTERNAL_DTD, " ");
            docFactory.setAttribute(ACCESS_EXTERNAL_SCHEMA, " ");
        } catch (IllegalArgumentException ex) {
            System.out.println("JAXP 1.5 Support is required to validate XML");
        }

        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        // Not using the parse(File) method on purpose, so that we can control when
        // to close it. Somehow parse(File) does not seem to close the file in all cases.
        try {
            return docBuilder.parse(file);
        } finally {
            file.close();
        }
    }

    public void storeZeldaRPKGMeta() throws IOException {
        //store build config data
        Properties zeldaBuildProperties = new Properties();
        zeldaBuildProperties.setProperty("supportArch", Joiner.on(",").join(orignApkSupportArch));
        //add build serial number
        String serialNo = Constants.zeldaPrefix + UUID.randomUUID().toString();
        System.out.println("build serialNo: " + serialNo);

        zeldaBuildProperties.setProperty(Constants.KEY_ZELDA_BUILD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        zeldaBuildProperties.setProperty(Constants.KEY_ZELDA_BUILD_SERIAL, serialNo);

        zeldaBuildProperties.setProperty(Constants.KEY_ORIGIN_PKG_NAME, apkMeta.getPackageName());
        zeldaBuildProperties.setProperty(Constants.KEY_ORIGIN_APPLICATION_NAME, originApplicationClass);
        zeldaBuildProperties.setProperty(Constants.KEY_NEW_PKG_NAME, newPkgName);
        zeldaBuildProperties.setProperty(Constants.kEY_SUFFER_KEY, sufferKey);


        zipOutputStream.putNextEntry(new ZipEntry("assets/" + Constants.zeldaConfigFileName));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        zeldaBuildProperties.store(byteArrayOutputStream, "auto generated by zelda repakcage builder");
        zipOutputStream.write(byteArrayOutputStream.toByteArray());


        //save declared component
        zipOutputStream.putNextEntry(new ZipEntry("assets/" + Constants.declaredComponentListConfig));
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(zipOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        for (String str : declaredComponentClassNames) {
            bufferedWriter.write(str);
            bufferedWriter.newLine();
        }
        bufferedWriter.close();
        outputStreamWriter.close();
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
}
