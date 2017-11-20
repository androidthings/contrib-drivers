package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.util.Arrays;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@SuppressWarnings("WeakerAccess")
public class ColorChannelSequence {
    public static final int RGB = 1;
    public static final int RBG = 2;
    public static final int GRB = 3;
    public static final int GBR = 4;
    public static final int BRG = 5;
    public static final int BGR = 6;
    @Sequence
    private static final int[] ALL_SEQUENCES = {RGB, RBG, GRB, GBR, BRG, BGR};

    @Retention(SOURCE)
    @IntDef({RGB, RBG, GRB, GBR, BRG, BGR})
    @SuppressWarnings("WeakerAccess")
    public @interface Sequence {}

    interface Sequencer
    {
        int rearrangeColorChannels(@ColorInt int color);
    }

    @NonNull
    static Sequencer createSequencer(@Sequence int colorChannelSequence)
    {
        switch (colorChannelSequence) {
            case BGR:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return ((color & 0xff0000) >> 16) | (color & 0xff00) | ((color & 0xff) << 16);
                    }
                };
            case BRG:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return ((color & 0xff0000) >> 8) | ((color & 0xff00) >> 8) | ((color & 0xff) << 16);
                    }
                };
            case GBR:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return ((color & 0xff0000) >> 16) | ((color & 0xff00) << 8) | ((color & 0xff) << 8);
                    }
                };
            case GRB:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return ((color & 0xff0000) >> 8) | ((color & 0xff00) << 8) | (color & 0xff);
                    }
                };
            case RBG:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return (color & 0xff0000) | ((color & 0xff00) >> 8) | ((color & 0xff) << 8);
                    }
                };
            case RGB:
                return new Sequencer() {
                    @Override
                    public int rearrangeColorChannels(int color) {
                        return color;
                    }
                };
            default:
                throw new IllegalArgumentException("Invalid color channel sequence: " + colorChannelSequence + ". Supported color channel sequences are: " + Arrays.toString(ColorChannelSequence.ALL_SEQUENCES));
        }
    }

}
