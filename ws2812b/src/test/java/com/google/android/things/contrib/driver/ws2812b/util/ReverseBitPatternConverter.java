package com.google.android.things.contrib.driver.ws2812b.util;


import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Size;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ReverseBitPatternConverter {

    @IntRange(from = 0, to = (1 << 24) -1)
    public int convertBitPatternTo24BitInt(@Size(value = 8) byte[] bitPatterns) {
        byte[] firstBitPatterns = new byte[4];
        byte[] secondBitPatterns = new byte[4];

        System.arraycopy(bitPatterns, 0, firstBitPatterns, 0, 4);
        System.arraycopy(bitPatterns, 4, secondBitPatterns, 0, 4);

        int first12BitInt = convertBitPatternTo12BitInt(firstBitPatterns);
        int second12BitInt = convertBitPatternTo12BitInt(secondBitPatterns);

        return (first12BitInt << 12) | second12BitInt;
    }

    @IntRange(from = 0, to = (1 << 12) -1)
    public int convertBitPatternTo12BitInt(@Size(value = 4) byte[] bitPatterns) {
        List<Boolean> booleanBitPatterns = convertToBooleanBitPatternWithMissingPauseBit(bitPatterns);
        List<Boolean> originalBooleanBits = new ArrayList<>();
        ListIterator<Boolean> booleanBitPatternIterator = booleanBitPatterns.listIterator();

        Assert.assertEquals(12 * 3, booleanBitPatterns.size());

        for (int i = 0; i < 12; i++) {
            boolean bit0 = booleanBitPatternIterator.next();
            boolean bit1 = booleanBitPatternIterator.next();
            boolean bit2 = booleanBitPatternIterator.next();

            originalBooleanBits.add(isOneBitPattern(bit0, bit1, bit2));
        }
        return convertTo12BitInt(originalBooleanBits);
    }

    @NonNull
    @Size(value = 9)
    private List<Boolean> convertToBooleanBitPatternWithMissingPauseBit(@Size(value = 4) byte[] bitPatterns) {
        List<Boolean> booleanBitPatterns = new ArrayList<>();
        for (byte bitPattern : bitPatterns) {
            booleanBitPatterns.addAll(convertBitPatternTo12BitInt(bitPattern));
            // Add missing pause bit
            booleanBitPatterns.add(false);
        }
        return booleanBitPatterns;
    }

    private int convertTo12BitInt(@Size(value = 12) List<Boolean> originalBooleanBits) {
        int converted = 0;
        int highestBit = 1 << 11;
        for (int i = 0; i < 12; i++) {
            if (originalBooleanBits.get(i)) {
                converted |= highestBit;
            }
            highestBit = highestBit >> 1;
        }
        return converted;
    }

    private Boolean isOneBitPattern(boolean bit0, boolean bit1, boolean bit2) {
        if (!bit0)
        {
            throw new AssertionError("First bit in pattern must always be true");
        }
        return bit1 & !bit2;
    }

    @Size(value = 8)
    private List<Boolean> convertBitPatternTo12BitInt(int bitPattern)
    {
        List<Boolean> booleanBitPattern = new ArrayList<>();
        int oneByteBitMask = 0b1000_0000;
        for (int i = 0; i < 8; i++) {
            booleanBitPattern.add((oneByteBitMask & bitPattern) == oneByteBitMask);
            bitPattern = bitPattern << 1;
        }
        return booleanBitPattern;
    }
}
