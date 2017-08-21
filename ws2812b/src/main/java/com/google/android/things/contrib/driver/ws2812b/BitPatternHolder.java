package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Map which stores bit patterns for each possible 12 bit number -> (numbers from 0 to 4096)
 */
public class BitPatternHolder {
    private static final int BIGGEST_12_BIT_NUMBER = (1 << 12) - 1; // 2¹² - 1 = 4095
    private static final List<Boolean> BIT_PATTERN_FOR_ZERO_BIT = Arrays.asList(true, false, false);
    private static final List<Boolean> BIT_PATTERN_FOR_ONE_BIT = Arrays.asList(true, true, false);
    private static final int ONE_BYTE_BIT_MASKS[] = new int[]{  0b10000000,
                                                                0b01000000,
                                                                0b00100000,
                                                                0b00010000,
                                                                0b00001000,
                                                                0b00000100,
                                                                0b00000010,
                                                                0b00000001};

    private final Storage bitPatternStorage;

    public BitPatternHolder() {
        this(new DefaultStorage());
    }

    @VisibleForTesting
    /*package*/ BitPatternHolder(@NonNull Storage storage) {
        this.bitPatternStorage = storage;
        fillBitPatternCache();
    }

    /**
     * Returns for each possible 12 bit integer an corresponding bit pattern as byte array.
     * Throws an {@link IllegalArgumentException} if the integer is using more than 12 bit.
     *
     * @param twelveBitValue A 12 bit integer (from 0 to 4095)
     * @return The corresponding bit pattern as byte array
     */
    @NonNull
    public byte[] getBitPattern(@IntRange(from = 0, to = BIGGEST_12_BIT_NUMBER) int twelveBitValue) {
        byte[] bitPatternByteArray = bitPatternStorage.get(twelveBitValue);
        if (bitPatternByteArray == null)
        {
            throw new IllegalArgumentException("Only values from 0 to " + BIGGEST_12_BIT_NUMBER + " are allowed. The passed input value was: " + twelveBitValue);
        }
        return bitPatternByteArray;
    }

    private void fillBitPatternCache() {
        for (int i = 0; i <= BIGGEST_12_BIT_NUMBER; i++) {
            bitPatternStorage.put(i, calculateBitPatternByteArray(i));
        }
    }

    private byte[] calculateBitPatternByteArray(@IntRange(from = 0, to = BIGGEST_12_BIT_NUMBER) int twelveBitNumber) {
        List<Boolean> bitPatterns = new ArrayList<>();
        int highest12BitBitMask = 1 << 11;
        for (int i = 0; i < 12; i++) {
            if ((twelveBitNumber & highest12BitBitMask) == highest12BitBitMask)
            {
                bitPatterns.addAll(BIT_PATTERN_FOR_ONE_BIT);
            }
            else
            {
                bitPatterns.addAll(BIT_PATTERN_FOR_ZERO_BIT);
            }
            twelveBitNumber = twelveBitNumber << 1;
        }
        bitPatterns = removePauseBits(bitPatterns);
        return convertBitPatternsToByteArray(bitPatterns);
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

    private byte[] convertBitPatternsToByteArray(List<Boolean> bitPatterns) {

        if (bitPatterns.size() != 32)
        {
            throw new IllegalStateException("Undefined bit pattern size");
        }
        byte[] bitPatternsAsByteArray = new byte[4];
        bitPatternsAsByteArray [0] = convertBitPatternsToByte(bitPatterns.subList(0, 8));
        bitPatternsAsByteArray [1] = convertBitPatternsToByte(bitPatterns.subList(8, 16));
        bitPatternsAsByteArray [2] = convertBitPatternsToByte(bitPatterns.subList(16, 24));
        bitPatternsAsByteArray [3] = convertBitPatternsToByte(bitPatterns.subList(24, 32));
        return bitPatternsAsByteArray;
    }

    private byte convertBitPatternsToByte(List<Boolean> bitPatterns) {
        int bitPatternByte = 0;
        for (int i = 0; i < 8; i++) {
            if (bitPatterns.get(i))
            {
                bitPatternByte |= ONE_BYTE_BIT_MASKS[i];
            }
        }
        return (byte) bitPatternByte;
    }

    @VisibleForTesting
    /*package*/ interface Storage
    {
        void put(int key, byte[] value);
        byte[] get(int key);
    }

    private static class DefaultStorage implements Storage
    {
        private final SparseArray<byte[]> sparseArray = new SparseArray<>();

        @Override
        public void put(int key, byte[] value) {
            sparseArray.append(key, value);
        }

        @Override
        public byte[] get(int key) {
            return sparseArray.get(key);
        }
    }
}
