package com.polarbear.apkinjection;

import com.android.apksigner.ApkSignerTool;
import com.polarbear.apkinjection.utils.ZipUtil;
import com.wind.meditor.core.FileProcesser;
import com.wind.meditor.property.AttributeItem;
import com.wind.meditor.property.ModificationProperty;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class HandleTask {

    private final String source;
    private final String target;
    private final String className;
    private final String outFileName;

    private final String sourceDirPath;
    private final String targetDirPath;
    private final String outUNSignFileName;
    private final String keyStoreFileName;
    private final String keyName;
    private final String keyPassword;

    HandleTask(String source, String target, String className, String keyName, String keyPassword, String keyStoreFileName) {
        this.source = source.trim();
        this.target = target.trim();
        this.className = className.trim();
        this.keyPassword = keyPassword.trim();
        this.keyName = keyName.trim();
        this.keyStoreFileName = keyStoreFileName.trim();
        File sourceFile = new File(source);
        sourceDirPath = sourceFile.getParent() + "/s_" + System.currentTimeMillis();
        targetDirPath = new File(target).getParent() + "/t_" + System.currentTimeMillis();
        outFileName = sourceFile.getParent() + "/out_" + System.currentTimeMillis() + ".apk";
        outUNSignFileName = sourceFile.getParent() + "/out_" + System.currentTimeMillis() + "_unsign.apk";
    }

    public void run() throws Exception {
        System.out.println("source: " + source);
        System.out.println("target: " + target);
        System.out.println("className: " + className);
        System.out.println("sourceDirPath: " + sourceDirPath);
        System.out.println("targetDirPath: " + targetDirPath);
        System.out.println("outFileName: " + outFileName);
        System.out.println("outUNSignFileName: " + outUNSignFileName);
        System.out.println("unzip ...");
        // 解压文件
        if (!ZipUtil.unzip(source, sourceDirPath)) {
            throw new RuntimeException("unzip source fail");
        }
        if (!ZipUtil.unzip(target, targetDirPath)) {
            throw new RuntimeException("unzip target fail");
        }
        // 修改源apk的AndroidManifest
        System.out.println("modify AndroidManifest ...");
        modifyAndroidManifest();
        // 复制dex
        int sourceDexCount = getSourceDexFileCount();
        System.out.println("source dex count " + sourceDexCount);
        System.out.println("copy dex ...");
        copyDex(sourceDexCount);
        System.out.println("copy assets ...");
        copyAssets();
        System.out.println("copy libs ...");
        copyLibs();
        FileUtils.deleteDirectory(new File(targetDirPath));
        System.out.println("delete apk sign ...");
        deleteSign();
        System.out.println("genzip apk ...");
        if (!ZipUtil.zip(sourceDirPath + "/", outUNSignFileName)) {
            throw new RuntimeException("genzip apk fail");
        }
        FileUtils.deleteDirectory(new File(sourceDirPath));
        System.out.println("sign apk ...");
        try {
            signApkUsingAndroidApksigner(outUNSignFileName, keyStoreFileName, outFileName, keyPassword, keyName);
            System.out.println("success!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void signApkUsingAndroidApksigner(String apkPath, String keyStorePath, String signedApkPath, String keyStorePassword, String keyName) throws Exception {
        ArrayList<String> commandList;
        commandList = new ArrayList<>();
        commandList.add("sign");
        commandList.add("--ks");
        commandList.add(keyStorePath);
        commandList.add("--ks-key-alias");
        commandList.add(keyName);
        commandList.add("--ks-pass");
        commandList.add("pass:" + keyStorePassword);
        commandList.add("--key-pass");
        commandList.add("pass:" + keyStorePassword);
        commandList.add("--out");
        commandList.add(signedApkPath);
        commandList.add("--v1-signing-enabled");
        commandList.add("true");
        commandList.add("--v2-signing-enabled"); // v2签名不兼容android 6
        commandList.add("false");
        commandList.add("--v3-signing-enabled"); // v3签名不兼容android 6
        commandList.add("false");
        commandList.add(apkPath);
        int size = commandList.size();
        ApkSignerTool.main(commandList.toArray(new String[size]));
    }

    public void deleteSign() throws IOException {
        File[] files = new File(sourceDirPath + "/META-INF").listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".RSA") || name.endsWith(".SF") || name.endsWith(".MF");
            }
        });
        assert files != null;
        for (File file : files) {
            FileUtils.delete(file);
        }
    }

    public void copyDex(int startPos) throws IOException {
        File[] dexs = new File(targetDirPath).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(".dex");
            }
        });
        for (File dex : dexs) {
            startPos++;
            FileUtils.copyFile(dex, new File(sourceDirPath + "/classes" + startPos + ".dex"));
        }
    }

    public void copyAssets() throws IOException {
        File assets = new File(targetDirPath + "/assets");
        if (!assets.isDirectory()) {
            return;
        }
        File[] files = assets.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.getName().contains("dexopt")) {
                continue;
            }
            if (file.isDirectory()) {
                FileUtils.copyDirectory(file, new File(sourceDirPath + "/assets/" + file.getName()));
            } else {
                FileUtils.copyFile(file, new File(sourceDirPath + "/assets/" + file.getName()));
            }
        }
    }

    public void copyLibs() throws IOException {
        File libs = new File(targetDirPath + "/lib");
        if (!libs.isDirectory()) {
            return;
        }
        File[] files = libs.listFiles();
        if (files == null) {
            return;
        }
        if (!new File(sourceDirPath + "/lib").isDirectory()) {
            if (!new File(sourceDirPath + "/lib").mkdirs()) {
                throw new RuntimeException("mkdirs " + sourceDirPath + "/lib fail");
            }
        }
        for (File file : files) {
            copyFiles(file, new File(sourceDirPath + "/lib/" + file.getName()));
        }
    }

    public void copyFiles(File source, File target) throws IOException {
        if (source.isDirectory()) {
            target.mkdirs();
            File[] files = source.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                copyFiles(file, new File(target.getAbsolutePath() + "/" + file.getName()));
            }
        } else {
            FileUtils.copyFile(source, target);
        }
    }

    public void modifyAndroidManifest() throws IOException {
        String oldManifest = sourceDirPath + "/AndroidManifest_old.xml";
        String newManifest = sourceDirPath + "/AndroidManifest.xml";
        // 复制文件
        FileUtils.copyFile(new File(newManifest), new File(oldManifest));
        FileUtils.delete(new File(newManifest));
        ModificationProperty property = new ModificationProperty();
        property.addApplicationAttribute(new AttributeItem("appComponentFactory", className));
        FileProcesser.processManifestFile(oldManifest, newManifest, property);
        FileUtils.delete(new File(oldManifest));
    }

    public int getSourceDexFileCount() {
        return Objects.requireNonNull(new File(sourceDirPath).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(".dex");
            }
        })).length;
    }


}
