package com.fox.ysmu.util;

import com.fox.ysmu.model.ServerModelManager;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.zip.DataFormatException;

public final class YesModelUtils {
    /**
     * 二进制文件的头部幻数
     * YSGP 的 ASCII 码
     * YSGP 就是 Ying Su Group，映素小组的缩写
     */
    public static final int HEAD = 0x59_53_47_50;
    /**
     * 二进制文件的版本号
     */
    public static final int VERSION = 0x00_00_00_01;
    /**
     * 新版本号，用了更新的加密解密方法
     */
    public static final int VERSION_II = 0x00_00_00_02;
    /**
     * 加密方法
     */
    private static final String ENCRYPTION_METHOD = "AES";

    public static Map<String, byte[]> input(File ysmFile) throws IOException {
        String fileName = removeExtension(ysmFile.getName());
        if (!ResourceLocation.isValidResourceLocation(fileName)) {
            return Collections.emptyMap();
        }
        byte[] data = FileUtils.readFileToByteArray(ysmFile);
        int head = ByteInteger.bytes2Int(data, 0);
        int version = ByteInteger.bytes2Int(data, 4);
        if (head != HEAD) {
            return Collections.emptyMap();
        }
        if (version != VERSION && version != VERSION_II) {
            return Collections.emptyMap();
        }

        byte[] md5 = ByteArrays.copy(data, 8, 16);
        byte[] modelFilesData = ByteArrays.copy(data, 24, data.length - 24);
        if (!Arrays.equals(md5, Md5Utils.md5(modelFilesData))) {
            return Collections.emptyMap();
        }

        Map<String, byte[]> outputs = Maps.newHashMap();
        ByteArrayInputStream tmp = new ByteArrayInputStream(modelFilesData);
        while (tmp.available() > 0) {
            try {
                Pair<String, byte[]> ysmFileData;
                if (version == VERSION) {
                    ysmFileData = ysmToFile(tmp);
                } else {
                    ysmFileData = ysmToFileNew(tmp);
                }
                outputs.put(ysmFileData.left(), ysmFileData.right());
            } catch (GeneralSecurityException | DataFormatException e) {
                e.printStackTrace();
            }
        }
        return outputs;
    }

    @NotNull
    private static Pair<String, byte[]> ysmToFile(ByteArrayInputStream tmp) throws IOException, GeneralSecurityException, DataFormatException {
        String name = readString(tmp);
        int size = readInt(tmp);

        byte[] passwordBytes = new byte[16];
        byte[] ivBytes = new byte[16];
        tmp.read(passwordBytes);
        tmp.read(ivBytes);
        SecretKeySpec key = new SecretKeySpec(passwordBytes, ENCRYPTION_METHOD);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        byte[] fileData = new byte[size];
        tmp.read(fileData);

        ByteArrayOutputStream decryptData = AESUtil.decrypt(key, iv, fileData);
        byte[] rawData = DeflateUtil.decompressBytes(decryptData.toByteArray());

        return Pair.of(name, rawData);
    }

    @NotNull
    private static Pair<String, byte[]> ysmToFileNew(ByteArrayInputStream tmp) throws IOException, GeneralSecurityException, DataFormatException {
        String fileName = readBase64String(tmp);
        int fileSize = readInt(tmp);
        int cipherSecretKeySize = readInt(tmp);

        byte[] cipherSecretKey = new byte[cipherSecretKeySize];
        byte[] ivBytes = new byte[16];
        byte[] fileData = new byte[fileSize];
        tmp.read(cipherSecretKey);
        tmp.read(ivBytes);
        tmp.read(fileData);

        byte[] keyFromMd5 = getKeyFromMd5(fileData);
        SecretKey secretSecretKey = AESUtil.getKey(keyFromMd5);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        byte[] decryptSecretKey = AESUtil.decrypt(secretSecretKey, iv, cipherSecretKey).toByteArray();
        SecretKey key = AESUtil.getKey(decryptSecretKey);
        ByteArrayOutputStream decryptData = AESUtil.decrypt(key, iv, fileData);
        byte[] rawData = DeflateUtil.decompressBytes(decryptData.toByteArray());

        return Pair.of(fileName, rawData);
    }

    public static void export(File dir) throws IOException {
        String dirName = dir.getName();
        if (!ResourceLocation.isValidResourceLocation(dirName)) {
            return;
        }
        boolean noMainModelFile = true;
        boolean noArmModelFile = true;
        boolean noTextureFile = true;
        Collection<File> files = FileUtils.listFiles(dir, FileFileFilter.INSTANCE, null);
        for (File file : files) {
            String fileName = file.getName();
            if ("main.json".equals(fileName)) {
                noMainModelFile = false;
            }
            if ("arm.json".equals(fileName)) {
                noArmModelFile = false;
            }
            if (fileName.endsWith(".png")) {
                noTextureFile = false;
            }
        }
        if (noMainModelFile) {
            return;
        }
        if (noArmModelFile) {
            return;
        }
        if (noTextureFile) {
            return;
        }

        byte[] ysmData = filesToYsm(files);
        File outputFile = ServerModelManager.EXPORT.resolve(dirName + ".ysm").toFile();
        FileUtils.writeByteArrayToFile(outputFile, ysmData, false);
    }

    /**
     * 头部幻数
     * 版本号
     * 总 MD5
     * 各个文件
     */
    private static byte[] filesToYsm(Collection<File> files) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(ByteInteger.int2Bytes(HEAD));
        output.write(ByteInteger.int2Bytes(VERSION_II));

        byte[] filesData = filesToBytes(files);
        byte[] md5 = Md5Utils.md5(filesData);
        output.write(md5);
        output.write(filesData);
        return output.toByteArray();
    }

    private static byte[] filesToBytes(Collection<File> files) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        files.forEach(file -> {
            try {
                output.write(fileToBytes(file));
            } catch (IOException | GeneralSecurityException e) {
                e.printStackTrace();
            }
        });
        return output.toByteArray();
    }

    /**
     * 名称
     * 长度
     * 密码
     * 主体
     */
    private static byte[] fileToBytes(File file) throws IOException, GeneralSecurityException {
        byte[] rawData = FileUtils.readFileToByteArray(file);
        byte[] compressData = DeflateUtil.compressBytes(rawData);
        // 生成秘钥 1 和特征矩阵
        SecretKey secretKey = AESUtil.generateKey();
        IvParameterSpec iv = AESUtil.generateIv();
        // 将原文件 AES 加密
        ByteArrayOutputStream encryptData = AESUtil.encrypt(secretKey, iv, compressData);
        // 通过文件的 MD5 来生成特殊的秘钥 2
        byte[] keyFromMd5 = getKeyFromMd5(encryptData.toByteArray());
        // 用这个秘钥 2 来加密秘钥 1，获得密文秘钥
        ByteArrayOutputStream cipherSecretKey = AESUtil.encrypt(AESUtil.getKey(keyFromMd5), iv, secretKey.getEncoded());
        // 开始写文件
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        // 书写 Base64 加密过的字符串
        writeBase64String(output, file.getName());
        // 书写密文长度
        output.write(ByteInteger.int2Bytes(encryptData.size()));
        // 书写密文秘钥长度
        output.write(ByteInteger.int2Bytes(cipherSecretKey.size()));
        // 书写密文秘钥
        output.write(cipherSecretKey.toByteArray());
        // 书写特征矩阵
        output.write(iv.getIV());
        // 书写密文
        output.write(encryptData.toByteArray());
        return output.toByteArray();
    }

    private static byte[] getKeyFromMd5(byte[] fileData) {
        // 获取得到 MD5
        byte[] md5 = Md5Utils.md5(fileData);
        // 以 MD5 为种子，获取固定的随机数
        Random random = new Random(toLong(md5));
        // 得到固定的随机数秘钥
        byte[] keys = new byte[16];
        random.nextBytes(keys);
        return keys;
    }

    private static long toLong(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) + (b & 255);
        }
        return value;
    }

    private static void writeString(ByteArrayOutputStream stream, String string) throws IOException {
        byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        stream.write(ByteInteger.int2Bytes(stringBytes.length));
        stream.write(stringBytes);
    }

    private static void writeBase64String(ByteArrayOutputStream stream, String string) throws IOException {
        byte[] stringBytes = Base64.getEncoder().encode(string.getBytes(StandardCharsets.UTF_8));
        stream.write(ByteInteger.int2Bytes(stringBytes.length));
        stream.write(stringBytes);
    }

    private static String readString(ByteArrayInputStream stream) throws IOException {
        int size = readInt(stream);
        byte[] stringBytes = new byte[size];
        stream.read(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    private static String readBase64String(ByteArrayInputStream stream) throws IOException {
        int size = readInt(stream);
        byte[] stringBytes = new byte[size];
        stream.read(stringBytes);
        return new String(Base64.getDecoder().decode(stringBytes), StandardCharsets.UTF_8);
    }

    private static boolean readBoolean(ByteArrayInputStream stream) throws IOException {
        return readInt(stream) != 0;
    }

    @SuppressWarnings("all")
    private static int readInt(ByteArrayInputStream stream) throws IOException {
        byte[] sizeBytes = new byte[4];
        stream.read(sizeBytes);
        return ByteInteger.bytes2Int(sizeBytes, 0);
    }

    private static String removeExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex != -1) {
            fileName = fileName.substring(0, lastIndex);
        }
        return fileName;
    }
}
