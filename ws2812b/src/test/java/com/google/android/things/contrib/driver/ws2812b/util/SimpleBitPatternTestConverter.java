package com.google.android.things.contrib.driver.ws2812b.util;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SimpleBitPatternTestConverter {
    private static final int [] ONE_BYTE_BIT_MASKS = {
            0b1000_0000,
            0b0100_0000,
            0b0010_0000,
            0b0001_0000,
            0b0000_1000,
            0b0000_0100,
            0b0000_0010,
            0b0000_0001,
    };

    public byte[] constructBitPatterns(int color)
    {
        List<Boolean> booleanBitPatterns = constructBooleanBitPatterns(color);
        booleanBitPatterns = removePauseBits(booleanBitPatterns);
        return convertToByteArray(booleanBitPatterns);
    }

    @NonNull
    private List<Boolean> constructBooleanBitPatterns(int color) {
        ArrayList<Boolean> bitPatterns = new ArrayList<>();
        List<Boolean> oneBitPattern = Arrays.asList(true, true, false);
        List<Boolean> zeroBitPattern = Arrays.asList(true, false, false);

        int highestBit = 1<<23;

        for (int i = 0; i < 24; i++) {
            List<Boolean> bitPattern = (color & highestBit) == highestBit ?  oneBitPattern : zeroBitPattern;
            bitPatterns.addAll(bitPattern);
            color = color << 1;
        }
        return bitPatterns;
    }

    private List<Boolean> removePauseBits(List<Boolean> bitPatterns) {
        Iterator<Boolean> iterator = bitPatterns.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            if (i == 8)
            {
                iterator.remove();
                i = 0;
                continue;
            }
            i++;
        }
        return bitPatterns;
    }

    private byte[] convertToByteArray(List<Boolean> bitPatterns) {
        int i;

        byte [] bytes = new byte[bitPatterns.size() / 8];
        byte currentByte = 0;
        i = 0;
        int j = 0;
        for (Boolean bitValue : bitPatterns) {
            if (bitValue)
            {
                currentByte |= ONE_BYTE_BIT_MASKS[i];
            }
            if (i == 7)
            {
                bytes[j++] = currentByte;
                currentByte = 0;
                i = 0;
                continue;
            }
            i++;
        }
        return bytes;
    }

}
