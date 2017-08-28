package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;

public class ColorToBitPatternConverter {
    private static final int MAX_NUMBER_OF_SUPPORTED_LEDS = 512;
    private static final int FIRST_TWELVE_BIT_BIT_MASK = 0x00FFF000;
    private static final int SECOND_TWELVE_BIT_BIT_MASK = 0x00000FFF;

    private final TwelveBitIntToBitPatternMapper mTwelveBitIntToBitPatternMapper;
    private ColorChannelSequence.Sequencer colorChannelSequencer;

    public ColorToBitPatternConverter(@ColorChannelSequence.Sequence int colorChannelSequence) {
        this(colorChannelSequence, new TwelveBitIntToBitPatternMapper());
    }

    @VisibleForTesting
    ColorToBitPatternConverter(@ColorChannelSequence.Sequence int colorChannelSequence, @NonNull TwelveBitIntToBitPatternMapper twelveBitIntToBitPatternMapper) {
        colorChannelSequencer = ColorChannelSequence.createSequencer(colorChannelSequence);
        mTwelveBitIntToBitPatternMapper = twelveBitIntToBitPatternMapper;
    }

    /**
     * Converts the passed color array to a correlating byte array of bit patterns. These resulting
     * bit patterns are readable by a WS2812B LED strip if they are sent by a SPI device with the
     * right frequency.
     *
     * @param colors An array of color integers {@link ColorInt}
     * @return Returns a byte array of correlating bit patterns
     */
    public byte[] convertToBitPattern(@ColorInt @NonNull @Size(max = MAX_NUMBER_OF_SUPPORTED_LEDS) int[] colors) {
        if (colors.length > MAX_NUMBER_OF_SUPPORTED_LEDS) {
            throw new IllegalArgumentException("Only " + MAX_NUMBER_OF_SUPPORTED_LEDS + " LEDs are supported. A Greater Number (" + colors.length + ") will result in SPI errors!");
        }

        byte[] bitPatterns = new byte[colors.length * 8];

        int i = 0;
        for (int color : colors) {
            color = colorChannelSequencer.rearrangeColorChannels(color);
            int firstValue = (color & FIRST_TWELVE_BIT_BIT_MASK) >> 12;
            int secondValue = color & SECOND_TWELVE_BIT_BIT_MASK;

            System.arraycopy(mTwelveBitIntToBitPatternMapper.getBitPattern(firstValue), 0, bitPatterns, i, 4);
            i += 4;
            System.arraycopy(mTwelveBitIntToBitPatternMapper.getBitPattern(secondValue), 0, bitPatterns, i, 4);
            i += 4;
        }
        return bitPatterns;
    }
}
