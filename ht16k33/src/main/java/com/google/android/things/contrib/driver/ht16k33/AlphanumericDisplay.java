/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.ht16k33;

import android.text.TextUtils;

import com.google.android.things.pio.I2cDevice;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AlphanumericDisplay extends Ht16k33 {

    private static final short DOT = (short) (1 << 14);
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
    /*package*/ AlphanumericDisplay(I2cDevice device) throws IOException {
        super(device);
    }

    /**
     * Clear the display memory.
     */
    public void clear() throws IOException {
        for (int i = 0; i < 4; i++) {
            writeColumn(i, (short) 0);
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
            val |= DOT;
        }
        writeColumn(index, (short) val);
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
     * Display an integer number.
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
        if (TextUtils.isEmpty(s)) {
            clear();
            return;
        }

        mBuffer.clear();
        mBuffer.mark();
        short n = (short) 0;
        char prevChar = (char) 0;
        for (char c : s.toCharArray()) {
            // truncate string to the size of the display
            if (mBuffer.position() == mBuffer.limit()) {
                break;
            }
            if (c == '.') {
                if (prevChar == '.') {
                    mBuffer.putShort(DOT);
                } else {
                    // add dot LED flag to the previous character.
                    n |= DOT;
                    mBuffer.reset();
                    mBuffer.putShort(n);
                }
            } else {
                // extract character data from font.
                n = (short) Font.DATA[c];
                mBuffer.mark();
                mBuffer.putShort(n);
            }
            prevChar = c;
        }

        // clear the rest of the display
        while (mBuffer.position() < mBuffer.capacity()) {
            mBuffer.put((byte) 0);
        }

        mBuffer.flip();
        // write display memory.
        for (int i = 0; i < mBuffer.limit() / 2; i++) {
            writeColumn(i, mBuffer.getShort());
        }
    }
}
