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

import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class Ht16k33 implements AutoCloseable {

    private static final String TAG = Ht16k33.class.getSimpleName();

    /**
     * I2C slave address.
     */
    public static final int I2C_ADDRESS = 0x70;

    private static final int HT16K33_CMD_SYSTEM_SETUP = 0x20;
    private static final int HT16K33_OSCILLATOR_ON = 0b0001;
    private static final int HT16K33_OSCILLATOR_OFF = 0b0000;
    private static final int HT16K33_CMD_DISPLAYSETUP = 0x80;
    private static final int HT16K33_DISPLAY_ON = 0b0001;
    private static final int HT16K33_DISPLAY_OFF = 0b0000;
    private static final int HT16K33_CMD_BRIGHTNESS = 0xE0;

    /**
     * The maximum brightness level for this display
     */
    public static final int HT16K33_BRIGHTNESS_MAX = 0b00001111;

    private I2cDevice mDevice;

    /**
     * Create a new driver for a HT16K33 peripheral connected on the given I2C bus.
     * @param bus
     */
    public Ht16k33(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        connect(device);
    }

    /**
     * Create a new driver for a HT16K33 peripheral from a given I2C device.
     * @param device
     */
    @VisibleForTesting
    /*package*/ Ht16k33(I2cDevice device) {
        connect(device);
    }

    private void connect(I2cDevice device) {
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
     * Enable oscillator and LED display.
     * @throws IOException
     */
    public void setEnabled(boolean enabled) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not opened");
        }
        int oscillator_flag = enabled ? HT16K33_OSCILLATOR_ON : HT16K33_OSCILLATOR_OFF;
        mDevice.write(new byte[]{(byte) (HT16K33_CMD_SYSTEM_SETUP | oscillator_flag)}, 1);
        int display_flag = enabled ? HT16K33_DISPLAY_ON : HT16K33_DISPLAY_OFF;
        mDevice.write(new byte[]{(byte) (HT16K33_CMD_DISPLAYSETUP | display_flag)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and {@link #HT16K33_BRIGHTNESS_MAX}
     */
    public void setBrightness(int value) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not opened");
        }
        if (value < 0 || value > HT16K33_BRIGHTNESS_MAX) {
            throw new IllegalArgumentException("brightness must be between 0 and " +
                    HT16K33_BRIGHTNESS_MAX);
        }
        mDevice.write(new byte[]{(byte) (HT16K33_CMD_BRIGHTNESS | (byte) value)}, 1);
    }

    /**
     * Set LED display brightness.
     * @param value brigthness value between 0 and 1.0f
     */
    public void setBrightness(float value) throws IOException {
        int val = Math.round(value * HT16K33_BRIGHTNESS_MAX);
        setBrightness(val);
    }

    /***
     * Write 16bit of LED row data to the given column.
     * @param column
     * @param data LED state for ROW0-15
     */
    public void writeColumn(int column, short data) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not opened");
        }
        mDevice.writeRegWord(column * 2, data);
    }
}
