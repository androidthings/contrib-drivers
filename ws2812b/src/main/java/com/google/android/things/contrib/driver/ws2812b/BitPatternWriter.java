package com.google.android.things.contrib.driver.ws2812b;


import android.support.annotation.IntRange;

class BitPatternWriter {
    private static final int MAX_NUMBER_OF_SUPPORTED_LEDS = 512;
    private static final int NUMBER_OF_BYTES_PER_RGB_COLOR = 8;
    private static final int ONE_BYTE_BIT_MASKS[] = new int[]{128, 64, 32, 16, 8, 4, 2, 1};

    private final byte[] mDestinationByteBuffer;
    private int mCurrentByteBufferIndex;
    private int mBitMaskIndex;
    private int mCurrentWrittenByte;

    /**
     * Converts bits of a color value with two bit patterns to bits comprehensible for a WS2812B
     * controller. To convert and write a color value call {@link #writeBitPatternToBuffer(int)}.
     * These converted bits are written to a buffer. The buffer is a simple byte array
     * which can be retrieved with {@link #getBitPatternBuffer()} after
     * {@link #finishBitPatternBuffer()} was called
     * 1 src bit
     * 3 source bits will be represented by 1 destination byte:
     * - The first 2 source bits are represented by 3 destination bits
     * - The last source bit is represented by 2 destination bits and 1 pause bit
     * - The pause bit is automatically sent between each destination byte (it is always 0)
     *
     * => 3 src bits = 1 dst byte
     * => 24 src bits = 8 dst bytes
     * => 1 RGB LED color = 8 dst bytes
     * @param numberOfLeds number of LEDs of the
     */
    BitPatternWriter(@IntRange(from = 1, to = MAX_NUMBER_OF_SUPPORTED_LEDS) int numberOfLeds) {
        if (numberOfLeds > MAX_NUMBER_OF_SUPPORTED_LEDS) {
            throw new IllegalArgumentException("Only " + MAX_NUMBER_OF_SUPPORTED_LEDS + " LEDs are supported. A Greater Number (" + numberOfLeds + ") will result in SPI errors!");
        }

        int numberOfDestinationBytes = numberOfLeds * NUMBER_OF_BYTES_PER_RGB_COLOR;

        mDestinationByteBuffer = new byte[numberOfDestinationBytes];
        mCurrentByteBufferIndex = 0;
        mBitMaskIndex = 0;
        mCurrentWrittenByte = 0;
    }

    /**
     * Converts one value of a channel of a RGB color into a bit pattern and writes the bit pattern in a byte buffer
     *
     * @param colorChannelValue The value of one channel of a RGB color
     */
    void writeBitPatternToBuffer(@IntRange(from = 0, to = 255) int colorChannelValue) {
        // Highest byte bit
        int mask = 128;

        for (int i = 0; i < 8; i++) {
            if ((colorChannelValue & mask) == mask) {
                writeOneBitPattern();
            } else {
                writeZeroBitPattern();
            }

            colorChannelValue = colorChannelValue << 1;
        }
    }

    /**
     * Writes the bit pattern for a 0 bit which is represented by a 1, 0, 0
     */
    private void writeZeroBitPattern() {
        mCurrentWrittenByte = mCurrentWrittenByte | ONE_BYTE_BIT_MASKS[mBitMaskIndex++]; // Write 1
        mBitMaskIndex++; // Do nothing means write 0
        mBitMaskIndex++; // Do nothing means write 0

        storeValueAndShiftToNextBitsInBuffer();
    }

    /**
     * Writes the bit pattern for a 1 bit which is represented by a 1, 1, 0
     */
    private void writeOneBitPattern() {
        mCurrentWrittenByte = mCurrentWrittenByte | ONE_BYTE_BIT_MASKS[mBitMaskIndex++]; // Write 1
        mCurrentWrittenByte = mCurrentWrittenByte | ONE_BYTE_BIT_MASKS[mBitMaskIndex++]; // Write 1
        mBitMaskIndex++; // Do nothing means write 0

        storeValueAndShiftToNextBitsInBuffer();
    }

    private void storeValueAndShiftToNextBitsInBuffer() {
        // There is no need to to store the pause bit. It can be ignored
        if (mBitMaskIndex % 8 == 1) {
            mDestinationByteBuffer[mCurrentByteBufferIndex] = (byte) mCurrentWrittenByte;
            mCurrentByteBufferIndex++;
            mBitMaskIndex = 0;
            mCurrentWrittenByte = 0;
        }
    }

    void finishBitPatternBuffer() {
        if (mCurrentByteBufferIndex < mDestinationByteBuffer.length) {
            mDestinationByteBuffer[mCurrentByteBufferIndex] = (byte) mCurrentWrittenByte;
        }
    }

    byte[] getBitPatternBuffer() {
        return mDestinationByteBuffer;
    }

}
