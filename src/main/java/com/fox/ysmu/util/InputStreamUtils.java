package com.fox.ysmu.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class InputStreamUtils {

    public static byte[] toBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = stream.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
