package com.google.brillo.driver.captouch;

/**
 * Collection of helpers for bitwise operations
 */
class BitwiseUtil {

    /**
     * Set a bit in the provided value at the given index.
     * @return the updated value.
     */
    static byte setBit(byte value, int bitIndex) {
        return (byte)((value | (1 << bitIndex)) & 0xFF);
    }

    /**
     * Clear a bit in the provided value at the given index.
     * @return the updated value.
     */
    static byte clearBit(byte value, int bitIndex) {
        return (byte)((value & ~(1 << bitIndex)) & 0xFF);
    }

    /**
     * Return true if the provided bit index is set in the given value.
     */
    static boolean isBitSet(byte value, int bitIndex) {
        return (value & (1 << bitIndex)) != 0;
    }

    /**
     * Mask the bits of the provided value into base, according to
     * the provided bitmask and return the updated value.
     */
    static byte applyBitRange(byte base, int value, int bitmask) {
        base &= ~bitmask;
        base |= value & bitmask;

        return base;
    }
}
