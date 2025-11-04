package com.eliotlash.mclib.utils;

import org.apache.commons.lang3.StringUtils;

public class Color implements ICopy<Color> {

    public float r;
    public float g;
    public float b;
    public float a = 1;

    public Color() {}

    public Color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color(float r, float g, float b, float a) {
        this(r, g, b);

        this.a = a;
    }

    public Color(int color) {
        this(color, true);
    }

    public Color(int color, boolean alpha) {
        this.set(color, alpha);
    }

    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        return this;
    }

    public Color set(float value, int component) {
        switch (component) {
            case 1:
                this.r = value;
                break;

            case 2:
                this.g = value;
                break;

            case 3:
                this.b = value;
                break;

            default:
                this.a = value;
                break;
        }

        return this;
    }

    public Color set(int color) {
        return this.set(color, true);
    }

    public Color set(int color, boolean alpha) {
        this.set(
            (color >> 16 & 0xff) / 255F,
            (color >> 8 & 0xff) / 255F,
            (color & 0xff) / 255F,
            alpha ? (color >> 24 & 0xff) / 255F : 1F);

        return this;
    }

    @Override
    public Color copy() {
        Color copy = new Color();

        copy.copy(this);

        return copy;
    }

    @Override
    public void copy(Color color) {
        this.set(color.r, color.g, color.b, color.a);
    }

    public int getRGBAColor() {
        float r = MathUtils.clamp(this.r, 0, 1);
        float g = MathUtils.clamp(this.g, 0, 1);
        float b = MathUtils.clamp(this.b, 0, 1);
        float a = MathUtils.clamp(this.a, 0, 1);

        return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public int getRGBColor() {
        return this.getRGBAColor() & 0xffffff;
    }

    public String stringify() {
        return this.stringify(false);
    }

    public String stringify(boolean alpha) {
        if (alpha) {
            return "#" + StringUtils.leftPad(Integer.toHexString(this.getRGBAColor()), 8, '0');
        }

        return "#" + StringUtils.leftPad(Integer.toHexString(this.getRGBColor()), 6, '0');
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Color) {
            Color color = (Color) obj;

            return color.getRGBAColor() == this.getRGBAColor();
        }

        return super.equals(obj);
    }
}
