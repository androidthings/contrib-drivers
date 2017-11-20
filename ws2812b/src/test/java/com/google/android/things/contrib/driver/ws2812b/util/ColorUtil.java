package com.google.android.things.contrib.driver.ws2812b.util;


import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;

public class ColorUtil {

    @SuppressWarnings("SameParameterValue")
    public static int []generateRandomColors(int numberOfRandomColors) {
        int[] randomColors = new int[numberOfRandomColors];
        for (int i = 0; i < randomColors.length; i++) {
            randomColors[i] = generateRandomColorValue();
        }
        return randomColors;
    }

    @IntRange(from = 0, to = 255)
    public static int generateRandomColorValue() {
        return (int) Math.round(Math.random() * 255);
    }

    public static int get1stColor(@ColorInt int color)
    {
        return Color.red(color);
    }

    public static int get2ndColor(@ColorInt int color)
    {
        return Color.green(color);
    }

    public static int get3rdColor(@ColorInt int color)
    {
        return Color.blue(color);
    }
}
