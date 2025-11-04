package com.eliotlash.mclib.utils.resources;

import com.eliotlash.mclib.utils.Color;

public enum PixelAccessor {

    BYTE() {

        @Override
        public void get(Pixels pixels, int index, Color color) {
            index *= pixels.pixelLength;

            int offset = 0;

            if (pixels.hasAlpha()) {
                color.a = ((int) pixels.pixelBytes[index + offset++] & 0xff) / 255F;
            } else {
                color.a = 1;
            }

            color.b = ((int) pixels.pixelBytes[index + offset++] & 0xff) / 255F;
            color.g = ((int) pixels.pixelBytes[index + offset++] & 0xff) / 255F;
            color.r = ((int) pixels.pixelBytes[index + offset] & 0xff) / 255F;
        }

        @Override
        public void set(Pixels pixels, int index, Color color) {
            index *= pixels.pixelLength;

            int offset = 0;

            if (pixels.hasAlpha()) {
                pixels.pixelBytes[index + offset++] = (byte) (color.a * 0xff);
            }

            pixels.pixelBytes[index + offset++] = (byte) (color.b * 0xff);
            pixels.pixelBytes[index + offset++] = (byte) (color.g * 0xff);
            pixels.pixelBytes[index + offset] = (byte) (color.r * 0xff);
        }
    },
    INT() {

        @Override
        public void get(Pixels pixels, int index, Color color) {
            int c = pixels.pixelInts[index];

            int a = c >> 24 & 0xff;
            int b = c >> 16 & 0xff;
            int g = c >> 8 & 0xff;
            int r = c & 0xff;

            color.r = r / 255F;
            color.g = g / 255F;
            color.b = b / 255F;
            color.a = a / 255F;
        }

        @Override
        public void set(Pixels pixels, int index, Color color) {
            pixels.pixelInts[index] = ((int) (color.a * 0xff) << 24) + ((int) (color.b * 0xff) << 16)
                + ((int) (color.g * 0xff) << 8)
                + (int) (color.r * 0xff);
        }
    };

    public abstract void get(Pixels pixels, int index, Color color);

    public abstract void set(Pixels pixels, int index, Color color);
}
