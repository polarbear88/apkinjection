package com.polarbear.apkinjection.utils;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static boolean zip(String sourceDir, String outputFile) throws IOException {
        ZipOutputStream zipFile = new ZipOutputStream(Files.newOutputStream(Paths.get(outputFile)));
        compressDirectoryToZipfile(sourceDir, sourceDir, zipFile);
        IOUtils.closeQuietly(zipFile);
        return true;
    }

    private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException {
        for (File file : Objects.requireNonNull(new File(sourceDir).listFiles())) {
            if (file.isDirectory()) {
                //如果是一个空文件夹
                if (Objects.requireNonNull(file.listFiles()).length == 0) {
                    ZipEntry zipEntry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName() + "/");

                    out.putNextEntry(zipEntry);
                    out.closeEntry();
                } else {
                    compressDirectoryToZipfile(rootDir, sourceDir + file.getName() + File.separator, out);
                }
            } else {
                FileInputStream in = new FileInputStream(sourceDir + file.getName());
                ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
                out.putNextEntry(entry);
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(in);
            }
        }
    }

    public static boolean unzip(String zipPath, String targetPath) {
        File pathFile = new File(targetPath);
        if (!pathFile.exists()) {
            pathFile.mkdirs();
        } else {
            System.out.println("targetPath 已存在");
            return false;
        }
        try {
            //指定编码
            try (ZipFile zipFile = new ZipFile(zipPath)) {
                //遍历里面的文件及文件夹
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String zipEntryName = entry.getName();
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        String outpath = (targetPath + File.separator + zipEntryName);
                        //判断路径是否存在，不存在则创建文件路径
                        File file = new File(outpath.substring(0, outpath.lastIndexOf(File.separator)));
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        //判断文件全路径是否为文件夹
                        if (new File(outpath).isDirectory()) continue;
                        try (OutputStream out = Files.newOutputStream(Paths.get(outpath))) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
