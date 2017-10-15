package com.google.android.things.contrib.driver.ws2812b.util;


import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SimpleColorToBitPatternConverter {
    private static final List<Boolean> ONE_BIT_PATTERN = Arrays.asList(true, true, false);
    private static final List<Boolean> ZERO_BIT_PATTERN = Arrays.asList(true, false, false);

    public byte[] convertColorsToBitPattern(int [] colors)
    {
        List<Boolean> booleanBitPatterns = new ArrayList<>();
        for (int color : colors) {
            booleanBitPatterns.addAll(constructBooleanBitPatterns(color));
        }
        booleanBitPatterns = removePauseBits(booleanBitPatterns);
        return convertBitPatternToByteArray(booleanBitPatterns);
    }

    @NonNull
    private List<Boolean> constructBooleanBitPatterns(int color) {
        ArrayList<Boolean> bitPatterns = new ArrayList<>();

        int highestBit = 1<<23;

        for (int i = 0; i < 24; i++) {
            List<Boolean> bitPattern = (color & highestBit) == highestBit ? ONE_BIT_PATTERN : ZERO_BIT_PATTERN;
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
            if (i == 8){
                iterator.remove();
                i = 0;
                continue;
            }
            i++;
        }
        return bitPatterns;
    }

    private byte[] convertBitPatternToByteArray(List<Boolean> bitPatterns) {
        List<List<Boolean>> eightBitPatterns = splitInEightBitParts(bitPatterns);
        byte [] bytes = new byte[eightBitPatterns.size()];
        int i = 0;

        for (List<Boolean> eightBitPattern : eightBitPatterns) {
            int currentBitMask = 0b10000000;
            byte currentByte = 0;
            for (Boolean booleanBit : eightBitPattern) {
                if (booleanBit) {
                    currentByte |= currentBitMask;
                }
                currentBitMask >>= 1;
            }
            bytes[i++] = currentByte;
        }
        return bytes;
    }

    @NonNull
    private List<List<Boolean>> splitInEightBitParts(List<Boolean> bitPatterns) {
        List<List<Boolean>> eightBitPatterns = new ArrayList<>();
        int index = 0;
        int numberOfBits = bitPatterns.size();
        while (index < numberOfBits && index + 8 <= numberOfBits) {
            eightBitPatterns.add(bitPatterns.subList(index, index + 8));
            index += 8;
        }
        if (index != numberOfBits) {
            throw new IllegalStateException("Wrong number of bit pattern size: " + numberOfBits);
        }
        return eightBitPatterns;
    }

}
