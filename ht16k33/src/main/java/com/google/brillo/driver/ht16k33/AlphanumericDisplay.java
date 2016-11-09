package com.google.brillo.driver.ht16k33;

import android.hardware.pio.I2cDevice;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AlphanumericDisplay extends Ht16k33 {
    private ByteBuffer mBuffer = ByteBuffer.allocate(8);

    /**
     * Create a new driver for a HT16K33 based alphanumeric display connected on the given I2C bus.
     * @param bus
     * @throws IOException
     */
    public AlphanumericDisplay(String bus) throws IOException {
        super(bus);
    }

    /**
     * Create a new driver for a HT16K33 based alphanumeric display from a given I2C device.
     * @param device
     * @throws IOException
     */
    public AlphanumericDisplay(I2cDevice device) throws IOException {
        super(device);
    }

    /**
     * Clear the display memory.
     */
    public void clear() throws IOException {
        for (int i = 0; i < 4; i++) {
            writeColumn(i, (short)0x0000);
        }
    }

    /**
     * Display a character at the given index.
     * @param index index of the segment display
     * @param c character value
     * @param dot state of the dot LED
     */
    public void display(char c, int index, boolean dot) throws IOException {
        int val = Font.DATA[c];
        if (dot) {
            val |= 1 << 14;
        }
        writeColumn(index, (short)val);
    }

    /**
     * Display a decimal number.
     * @param n number value
     */
    public void display(double n) throws IOException {
        // pad with leading space until we get 5 chars
        // since double always get formatted with a dot
        // and the dot doesn't consume any space on the display.
        display(String.format("%5s", n));
    }


    /**
     * Display a integer number.
     * @param n number value
     */
    public void display(int n) throws IOException {
        // pad with leading space until we get 4 chars
        display(String.format("%4s", n));
    }

    /**
     * Display a string.
     * @param s string value
     */
    public void display(String s) throws IOException {
        mBuffer.clear();
        mBuffer.mark();
        short n = 0;
        for (char c : s.toCharArray()) {
            // truncate string to the size of the display
            if (mBuffer.position() == mBuffer.limit()) {
                break;
            }
            if (c == '.') {
                // add dot LED flag to the previous character.
                n |= 1 << 14;
                mBuffer.reset();
                mBuffer.putShort(n);
                continue;
            }
            // extract character data from font.
            n = (short)Font.DATA[c];
            mBuffer.mark();
            mBuffer.putShort(n);
        }
        mBuffer.flip();
        // write display memory.
        for (int i = 0; i < mBuffer.limit()/2; i++) {
            writeColumn(i, mBuffer.getShort());
        }
    }
}
