package com.fox.ysmu.util;

import java.text.DecimalFormat;

public final class FileSizeUtils {

    private static final String[] UNITS = new String[] { "B", "kB", "MB", "GB", "TB" };

    /**
     * 来自 https://stackoverflow.com/a/5599842
     */
    public static String size(long size) {
        if (size <= 0) {
            return "0";
        }
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + UNITS[digitGroups];
    }
}
