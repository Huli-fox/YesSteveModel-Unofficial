package com.fox.ysmu.util;

import javax.annotation.Nullable;
import java.io.*;

public final class ObjectStreamUtil {
    public static byte[] toByteArray(Object object) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(stream)) {
            output.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stream.toByteArray();
    }

    @Nullable
    public static Object toObject(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        try (ObjectInputStream input = new ObjectInputStream(stream)) {
            return input.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
