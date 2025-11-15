package com.fox.ysmu.compat;

import org.joml.Quaternionf;
import org.lwjgl.util.vector.Quaternion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    public static Quaternion j2l(Quaternionf jomlQuat) {
        Quaternion lwjglQuat = new Quaternion();
        lwjglQuat.set(jomlQuat.x, jomlQuat.y, jomlQuat.z, jomlQuat.w);
        return lwjglQuat;
    }

    public static boolean isValidResourceLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return false;
        }
        try {
            new net.minecraft.util.ResourceLocation(locationString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 将指定的ZIP文件解压到目标目录。
     * @param zipFile 要解压的ZIP文件
     * @param destDir 解压的目标目录
     * @throws IOException 如果发生I/O错误
     */
    public static void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[4096];
        try (ZipInputStream zis = new ZipInputStream(new java.io.FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry); // 使用辅助方法防止Zip Slip漏洞
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}
