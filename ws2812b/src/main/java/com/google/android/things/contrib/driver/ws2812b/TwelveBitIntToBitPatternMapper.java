package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Creates a storage which maps any possible 12 bit sized integer to bit patterns. If a sequence of
 * these bit patterns is sent to a WS2812b LED strip using SPI, the outcoming high and low voltage
 * pulses are recognized as the original 12 bit integers.<br>
 * Converting algorithm:<br>
 * - 1 src bit is converted to a 3 bit long bit pattern<br>
 * - The 9th bit in a sequence of bit pattern is a pause bit and must be removed. The pause bit is
 * automatically transmitted between every byte<br>
 * - This results in the fact that 3 source bits are converted to 1 destination byte<br>
 *
 * => 3 src bits = 8 bit (1 dst byte)<br>
 * => 12 src bits = 32 bit (4 dst bytes)<br>
 */
class TwelveBitIntToBitPatternMapper {
    private static final int BIGGEST_12_BIT_NUMBER = 0B1111_1111_1111;
    private static final List<Boolean> BIT_PATTERN_FOR_ZERO_BIT = Arrays.asList(true, false, false);
    private static final List<Boolean> BIT_PATTERN_FOR_ONE_BIT = Arrays.asList(true, true, false);
    private static final int ONE_BYTE_BIT_MASKS[] = new int[]{  0B10000000,
                                                                0B01000000,
                                                                0B00100000,
                                                                0B00010000,
                                                                0B00001000,
                                                                0B00000100,
                                                                0B00000010,
                                                                0B00000001};
    @NonNull
    private final SparseArray<byte[]> mSparseArray;

    TwelveBitIntToBitPatternMapper() {
        this(new SparseArray<byte[]>(BIGGEST_12_BIT_NUMBER));
    }

    @VisibleForTesting
    TwelveBitIntToBitPatternMapper(@NonNull final SparseArray<byte[]> sparseArray) {
        mSparseArray = sparseArray;
        fillBitPatternStorage();
    }

    /**
     * Returns for each possible 12 bit integer a corresponding sequence of bit pattern as byte array.
     * Throws an {@link IllegalArgumentException} if the integer is using more than 12 bit.
     *
     * @param twelveBitValue Any 12 bit integer (from 0 to 4095)
     * @return The corresponding bit pattern as 4 byte sized array
     */
    @NonNull
    @Size(value = 4)
    byte[] getBitPattern(@IntRange(from = 0, to = BIGGEST_12_BIT_NUMBER) int twelveBitValue) {
        byte[] bitPatternByteArray = mSparseArray.get(twelveBitValue);
        if (bitPatternByteArray == null)
        {
            throw new IllegalArgumentException("Only values from 0 to " + BIGGEST_12_BIT_NUMBER + " are allowed. The passed input value was: " + twelveBitValue);
        }
        return bitPatternByteArray;
    }

    private void fillBitPatternStorage() {
        for (int i = 0; i <= BIGGEST_12_BIT_NUMBER; i++) {
            mSparseArray.append(i, calculateBitPatternByteArray(i));
        }
    }

    @Size(value = 4)
    private byte[] calculateBitPatternByteArray(@IntRange(from = 0, to = BIGGEST_12_BIT_NUMBER) int twelveBitNumber) {
        List<Boolean> bitPatterns = new ArrayList<>();
        int highest12BitBitMask = 1 << 11;
        for (int i = 0; i < 12; i++) {
            if ((twelveBitNumber & highest12BitBitMask) == highest12BitBitMask){
                bitPatterns.addAll(BIT_PATTERN_FOR_ONE_BIT);
            }
            else{
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

    @Size(value = 4)
    private byte[] convertBitPatternsToByteArray(List<Boolean> bitPatterns) {

        if (bitPatterns.size() != 32)
        {
            throw new IllegalArgumentException("Undefined bit pattern size: Expected size is 32. Passed size: " + bitPatterns.size());
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
}
