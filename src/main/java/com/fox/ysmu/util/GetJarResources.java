package com.fox.ysmu.util;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.ysmu;

public final class GetJarResources {

    private GetJarResources() {}

    /**
     * 复制本模组的文件到指定文件夹
     *
     * @param filePath jar 里面的文件地址
     * @param destPath 想要复制到的目录
     * @param fileName 复制后的文件名
     */
    public static void copyYesSteveModelFile(String filePath, Path destPath, String fileName) {
        URL url = ysmu.class.getResource(filePath);
        if (url == null) {
            return;
        }
        try {
            FileUtils.copyURLToFile(
                url,
                destPath.resolve(fileName)
                    .toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
