package com.google.android.things.contrib.driver.ws2812b;


import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import java.util.Arrays;

import static com.google.android.things.contrib.driver.ws2812b.Ws2812b.*;


class ColorToBitPatternConverter
{
    @Ws2812b.LedMode
    private final int mLedMode;
    private static final int [] SUPPORTED_MODES = {RGB, RBG, GRB, GBR, BRG, BGR};


    ColorToBitPatternConverter(int ledMode)
    {
        this.mLedMode = ledMode;
    }

    @NonNull
    byte[] convertColorsToBitPattern(@NonNull @ColorInt int colors[])
    {
        byte [] convertedColors;
        switch (mLedMode)
        {
            case BGR:
                convertedColors = convertToBgr(colors);
                break;
            case BRG:
                convertedColors = convertToBrg(colors);
                break;
            case GBR:
                convertedColors = convertToGbr(colors);
                break;
            case GRB:
                convertedColors = convertToGrb(colors);
                break;
            case RBG:
                convertedColors = convertToRbg(colors);
                break;
            case Ws2812b.RGB:
                convertedColors = convertToRgb(colors);
                break;
            default:
                throw new IllegalStateException("This LED mode is not supported. Chosen mode: " + mLedMode + ". Supported modes: " + Arrays.toString(SUPPORTED_MODES));
        }
        return convertedColors;

    }

    private byte[] convertToBgr(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }

    private byte[] convertToBrg(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }

    private byte[] convertToGbr(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }

    private byte[] convertToGrb(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }

    private byte[] convertToRbg(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }

    private byte[] convertToRgb(@ColorInt int colors[])
    {
        BitPatternWriter bitPatternWriter = new BitPatternWriter(colors.length);

        for (int color : colors)
        {
            bitPatternWriter.writeBitPatternToBuffer(Color.red(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.green(color));
            bitPatternWriter.writeBitPatternToBuffer(Color.blue(color));
        }
        bitPatternWriter.finishBitPatternBuffer();

        return bitPatternWriter.getBitPatternBuffer();
    }
}
