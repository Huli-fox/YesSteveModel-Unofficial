package com.fox.ysmu.client.upload;

public class UploadManager {
    public static volatile String FILE_PATH = null;
    public static volatile Statue STATUE = Statue.FULFILL;

    public static void finishUpload() {
        FILE_PATH = null;
        STATUE = Statue.FULFILL;
    }

    public enum Statue {
        PROCESSING,
        FULFILL
    }
}
