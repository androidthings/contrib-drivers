package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class ColorOrderShifter {

    @Retention(SOURCE)
    @IntDef({RGB, RBG, GRB, GBR, BRG, BGR})
    public @interface Order {}
    public static final int RGB = 1;
    public static final int RBG = 2;
    public static final int GRB = 3;
    public static final int GBR = 4;
    public static final int BRG = 5;
    public static final int BGR = 6;

    public static int reorderColorToRbg(@ColorInt int color) {
        return (color & 0xff0000) | ((color & 0xff00) >> 8) | ((color & 0xff) << 8);
    }

    public static int reorderColorToGrb(@ColorInt int color) {
        return ((color & 0xff0000) >> 8) | ((color & 0xff00) << 8) | (color & 0xff);
    }

    public static int reorderColorToGbr(@ColorInt int color) {
        return ((color & 0xff0000) >> 16) | ((color & 0xff00) << 8) | ((color & 0xff) << 8);
    }

    public static int reorderColorToBrg(@ColorInt int color) {
        return ((color & 0xff0000) >> 8) | ((color & 0xff00) >> 8) | ((color & 0xff) << 16);
    }

    public static int reorderColorToBgr(@ColorInt int color) {
        return ((color & 0xff0000) >> 16) | (color & 0xff00) | ((color & 0xff) << 16);
    }
}
