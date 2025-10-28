package com.fox.ysmu.data;

import com.fox.ysmu.model.format.Type;
import com.fox.ysmu.util.AESUtil;
import com.fox.ysmu.util.ByteInteger;
import com.fox.ysmu.util.DeflateUtil;
import com.fox.ysmu.util.Md5Utils;
import com.fox.ysmu.ysmu;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.bytes.ByteArrays;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class EncryptTools {
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
     * 加密方法
     */
    private static final String ENCRYPTION_METHOD = "AES";

    /**
     * 二进制密码文件长度
     */
    private static final int PASSWORD_SIZE = 40;

    /**
     * 模型包密码
     */
    private static SecretKey SECRET_KEY;

    /**
     * 模型包特征矩阵（还是密码）
     */
    private static IvParameterSpec IV;

    public static void createRandomPassword() {
        SECRET_KEY = AESUtil.generateKey();
        IV = AESUtil.generateIv();
    }

    public static void readPassword(byte[] fileBytes) {
        if (fileBytes.length != PASSWORD_SIZE) {
            return;
        }
        int head = ByteInteger.bytes2Int(fileBytes, 0);
        int version = ByteInteger.bytes2Int(fileBytes, 4);
        if (head != HEAD) {
            return;
        }
        if (version != VERSION) {
            return;
        }
        byte[] password = ByteArrays.copy(fileBytes, 8, 16);
        byte[] iv = ByteArrays.copy(fileBytes, 24, 16);
        SECRET_KEY = new SecretKeySpec(password, ENCRYPTION_METHOD);
        IV = new IvParameterSpec(iv);
    }

    public static byte[] writePassword() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ByteInteger.int2Bytes(HEAD));
        stream.write(ByteInteger.int2Bytes(VERSION));
        stream.write(SECRET_KEY.getEncoded());
        stream.write(IV.getIV());
        return stream.toByteArray();
    }

    /**
     * 将附加信息和加密文件块组合成二进制文件
     * -----------------------------------------
     * 59 53 47 50                        幻数
     * 00 00 00 01                      版本号
     * 00 00 00 00 00 00 00 00
     * 00 00 00 00 00 00 00 00        文件 MD5
     * -----------------------------------------
     * 加密文件块
     */
    public static byte[] assembleEncryptModels(ModelData data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ByteInteger.int2Bytes(HEAD));
        stream.write(ByteInteger.int2Bytes(VERSION));
        byte[] encryptModelBytes = encryptModel(data);
        byte[] md5 = Md5Utils.md5(encryptModelBytes);
        stream.write(md5);
        stream.write(encryptModelBytes);
        return stream.toByteArray();
    }


    /**
     * 将模型、材质、动画组合成加密、压缩、二进制数据
     * -----------------------------------------
     * 7F FF FF FF 模型名称字符串
     * <p>
     * 7F FF FF FF 模型文件数量
     * 7F FF FF FF 模型文件名称
     * 7F FF FF FF 模型文件大小
     * 7F FF FF FF 模型文件名称
     * 7F FF FF FF 模型文件大小
     * <p>
     * 7F FF FF FF 材质文件大小
     * 7F FF FF FF 动画文件大小
     * 模型二进制文件块
     * 材质二进制文件块
     * 动画二进制文件块
     * -----------------------------------------
     * 进行一次 tar.gz 压缩
     * 进行一次 AES 加密
     */
    private static byte[] encryptModel(ModelData data) {
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();

            writeString(tmp, data.getModelId());
            writeBoolean(tmp, data.isAuth());

            writeMapDataInfo(tmp, data.getModel());
            writeMapDataInfo(tmp, data.getTexture());
            writeMapDataInfo(tmp, data.getAnimation());

            writeMapData(tmp, data.getModel());
            writeMapData(tmp, data.getTexture());
            writeMapData(tmp, data.getAnimation());

            byte[] output = DeflateUtil.compressBytes(tmp.toByteArray());
            return AESUtil.encrypt(SECRET_KEY, IV, output).toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ByteArrays.EMPTY_ARRAY;
    }

    private static void writeString(ByteArrayOutputStream stream, String string) throws IOException {
        byte[] stringBytes = string.getBytes(StandardCharsets.UTF_8);
        stream.write(ByteInteger.int2Bytes(stringBytes.length));
        stream.write(stringBytes);
    }

    private static void writeBoolean(ByteArrayOutputStream stream, boolean value) throws IOException {
        stream.write(ByteInteger.int2Bytes(value ? 1 : 0));
    }

    private static void writeFileInfo(ByteArrayOutputStream stream, String name, byte[] input) throws IOException {
        writeString(stream, name);
        stream.write(ByteInteger.int2Bytes(input.length));
    }

    private static void writeMapDataInfo(ByteArrayOutputStream stream, Map<String, byte[]> input) throws IOException {
        stream.write(ByteInteger.int2Bytes(input.size()));
        for (String name : input.keySet()) {
            writeFileInfo(stream, name, input.get(name));
        }
    }

    private static void writeMapData(ByteArrayOutputStream stream, Map<String, byte[]> input) throws IOException {
        for (byte[] data : input.values()) {
            stream.write(data);
        }
    }

    public static byte[] encryptPassword(byte[] uuid, byte[] input) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(uuid, ENCRYPTION_METHOD);
        IvParameterSpec iv = new IvParameterSpec(uuid);
        return AESUtil.encrypt(secretKey, iv, input).toByteArray();
    }

    private static byte[] decryptPassword(byte[] uuid, byte[] input) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(uuid, ENCRYPTION_METHOD);
        IvParameterSpec iv = new IvParameterSpec(uuid);
        byte[] rawPassword = AESUtil.decrypt(secretKey, iv, input).toByteArray();
        if (ByteInteger.bytes2Int(rawPassword, 0) != HEAD) {
            return ByteArrays.EMPTY_ARRAY;
        }
        if (ByteInteger.bytes2Int(rawPassword, 4) != VERSION) {
            return ByteArrays.EMPTY_ARRAY;
        }
        return rawPassword;
    }

    @Nullable
    public static ModelData decryptModel(byte[] uuid, byte[] password, byte[] modelRawData) {
        try {
            byte[] rawPassword = decryptPassword(uuid, password);
            if (rawPassword.length == 0) {
                return null;
            }
            if (ByteInteger.bytes2Int(modelRawData, 0) != HEAD) {
                return null;
            }
            if (ByteInteger.bytes2Int(modelRawData, 4) != VERSION) {
                return null;
            }

            String md5 = Md5Utils.toHexString(ByteArrays.copy(modelRawData, 8, 16));
            byte[] encryptModelData = ByteArrays.copy(modelRawData, 24, modelRawData.length - 24);
            String dataMd5 = Md5Utils.md5Hex(encryptModelData);
            if (!md5.equals(dataMd5)) {
                // TODO: 2023/7/11 很奇怪，这一块会出现不一致的问题
                ysmu.LOG.warn("Check values are not equal {} / {}", md5, dataMd5);
            }

            byte[] passwordBytes = ByteArrays.copy(rawPassword, 8, 16);
            byte[] ivBytes = ByteArrays.copy(rawPassword, 24, 16);
            SecretKeySpec key = new SecretKeySpec(passwordBytes, ENCRYPTION_METHOD);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            ByteArrayOutputStream decryptModelData = AESUtil.decrypt(key, iv, encryptModelData);
            byte[] modelData = DeflateUtil.decompressBytes(decryptModelData.toByteArray());

            if (modelData.length == 0) {
                return null;
            }

            ByteArrayInputStream tmp = new ByteArrayInputStream(modelData);

            String modelId = readString(tmp);
            boolean isAuth = readBoolean(tmp);
            Map<String, Integer> modelMapDataInfo = readMapDataInfo(tmp);
            Map<String, Integer> textureMapDataInfo = readMapDataInfo(tmp);
            Map<String, Integer> animationMapDataInfo = readMapDataInfo(tmp);

            Map<String, byte[]> modelMapData = readMapData(tmp, modelMapDataInfo);
            Map<String, byte[]> textureMapData = readMapData(tmp, textureMapDataInfo);
            Map<String, byte[]> animationMapData = readMapData(tmp, animationMapDataInfo);

            return new ModelData(modelId, isAuth, Type.UNKNOWN, modelMapData, textureMapData, animationMapData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("all")
    private static Map<String, byte[]> readMapData(ByteArrayInputStream tmp, Map<String, Integer> modelMapDataInfo) throws IOException {
        Map<String, byte[]> output = Maps.newHashMap();
        for (String name : modelMapDataInfo.keySet()) {
            int size = modelMapDataInfo.get(name);
            byte[] sizeBytes = new byte[size];
            tmp.read(sizeBytes);
            output.put(name, sizeBytes);
        }
        return output;
    }

    private static Map<String, Integer> readMapDataInfo(ByteArrayInputStream tmp) throws IOException {
        Map<String, Integer> mapDataInfo = Maps.newHashMap();
        int modelCount = readInt(tmp);
        for (int i = 0; i < modelCount; i++) {
            String name = readString(tmp);
            int size = readInt(tmp);
            mapDataInfo.put(name, size);
        }
        return mapDataInfo;
    }

    @SuppressWarnings("all")
    private static String readString(ByteArrayInputStream stream) throws IOException {
        int size = readInt(stream);
        byte[] stringBytes = new byte[size];
        stream.read(stringBytes);
        return new String(stringBytes);
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
}
