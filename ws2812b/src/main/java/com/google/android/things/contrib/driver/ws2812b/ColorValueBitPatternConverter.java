package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.IntRange;
import android.util.SparseArray;

class ColorValueBitPatternConverter
{
    private static final byte BIT_PATTERN_01 = (byte) 142;
    private static final byte BIT_PATTERN_10 = (byte) 132;
    private static final byte BIT_PATTERN_11 = (byte) 238;
    private static final byte BIT_PATTERN_00 = (byte) 136;

    private SparseArray<byte []> colorValueToBitPatternMap = new SparseArray<>();

    ColorValueBitPatternConverter()
    {
        for (int colorValue = 0; colorValue < 256; colorValue++)
        {
            byte[] bitPattern = new byte [4];
            String binaryString = toEightBitBinaryString(colorValue);
            bitPattern[0] = convertToBitPattern(binaryString.subSequence(0, 2).toString());
            bitPattern[1] = convertToBitPattern(binaryString.subSequence(2, 4).toString());
            bitPattern[2] = convertToBitPattern(binaryString.subSequence(4, 6).toString());
            bitPattern[3] = convertToBitPattern(binaryString.subSequence(6, 8).toString());
            colorValueToBitPatternMap.put(colorValue, bitPattern);
        }
    }

    private static String toEightBitBinaryString(@IntRange(from = 0, to = 255) int value)
    {
        StringBuilder stringBuilder = new StringBuilder(Integer.toBinaryString(value));
        while (stringBuilder.length() < 8)
        {
            stringBuilder.insert(0, '0');
        }
        return stringBuilder.toString();
    }

    private static byte convertToBitPattern(String binaryString)
    {
        return
                "01".equals(binaryString) ? BIT_PATTERN_01 :
                "10".equals(binaryString) ? BIT_PATTERN_10 :
                "11".equals(binaryString) ? BIT_PATTERN_11 :
                BIT_PATTERN_00;
    }

    byte[] convertColorValue(@IntRange(from = 0, to = 255) int colorValue)
    {
        return colorValueToBitPatternMap.get(colorValue);
    }
}
