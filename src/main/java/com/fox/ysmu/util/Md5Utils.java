package com.fox.ysmu.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Md5Utils {
    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5Hex(byte[] data) {
        return toHexString(DIGEST.digest(data));
    }

    public static byte[] md5(byte[] data) {
        return DIGEST.digest(data);
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
