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

package com.google.android.things.contrib.driver.tm1637;

import android.support.annotation.VisibleForTesting;

import java.io.Closeable;
import java.io.IOException;

/**
 * Driver for a Tm1637 seven-segment display with 4 digits. Usually it's preferable to use a
 * {@link NumericDisplay} instead.
 */
public class Tm1637 implements Closeable {

    private static final int TM1637_ADDR = 0x40;
    private static final int TM1637_REG = 0xc0;
    private static final int TM1637_CMD = 0x88;

    /**
     * The maximum brightness of the display
     */
    public static final int MAX_BRIGHTNESS = 0x07;

    /**
     * The maximum number of bytes that can be written at a time
     */
    public static final int MAX_DATA_LENGTH = 4;

    I2cBitBangDevice mDevice;

    /**
     * Create a new driver for a TM1637 peripheral connected on the given GPIO pins.
     */
    public Tm1637(String dataPin, String clockPin) throws IOException {
        mDevice = new I2cBitBangDevice(TM1637_ADDR, dataPin, clockPin);
    }

    @VisibleForTesting
    /* package */ Tm1637(I2cBitBangDevice device) throws IOException {
        mDevice = device;
    }

    /**
     * Close the device and the underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and {@link #MAX_BRIGHTNESS}
     */
    public void setBrightness(int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("Device not opened");
        }
        if (value < 0 || value > MAX_BRIGHTNESS) {
            throw new IllegalArgumentException("brightness must be between 0 and " +
                    MAX_BRIGHTNESS);
        }
        mDevice.write(new byte[]{(byte) (TM1637_CMD | (byte) value)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and 1.0f
     */
    public void setBrightness(float value) throws IOException {
        int val = Math.round(value * MAX_BRIGHTNESS);
        setBrightness(val);
    }

    /**
     * Write up to {@link #MAX_DATA_LENGTH} bytes of LED data.
     */
    public void writeData(byte[] data) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("Device not opened");
        }
        if (data.length > MAX_DATA_LENGTH) {
            throw new IllegalArgumentException("data size should be less than " + MAX_DATA_LENGTH);
        }
        mDevice.writeRegBuffer(TM1637_REG, data, data.length);
    }
}
