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

package com.google.android.things.contrib.driver.ssd1306;

import android.graphics.Bitmap;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

/**
 * Driver for controlling the SSD1306 OLED display.
 */
public class Ssd1306 implements Closeable {
    private static final String TAG = "Ssd1306";

    private I2cDevice mI2cDevice;

    // Screen configuration constants.
    private static final int LCDWIDTH = 128;
    private static final int LCDHEIGHT = 64;

    /**
     * I2C address for this peripheral
     */
    public static final int I2C_ADDRESS = 0x3C;
    public static final int I2C_ADDRESS_ALT = 0x3D;

    // Protocol constants
    private static final int COMMAND_ACTIVATE_SCROLL = 0x2F;
    private static final int COMMAND_DEACTIVATE_SCROLL = 0x2E;
    private static final int COMMAND_RIGHT_HORIZONTAL_SCROLL = 0x26;
    private static final int COMMAND_LEFT_HORIZONTAL_SCROLL = 0x27;
    private static final int COMMAND_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = 0x29;
    private static final int COMMAND_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = 0x2A;
    private static final int COMMAND_DISPLAY_ON = 0xAF;
    private static final int COMMAND_DISPLAY_OFF = 0xAE;
    private static final int COMMAND_START_LINE = 0x40;
    private static final int DATA_OFFSET = 1;
    private static final int INIT_CHARGE_PUMP = 0x8D;
    private static final int INIT_CLK_DIV = 0xD5;
    private static final int INIT_COMSCAN_DEC = 0xC8;
    private static final int INIT_DISPLAY_NO_OFFSET = 0x0;
    private static final int INIT_DISPLAY_OFFSET = 0xD3;
    private static final int INIT_DUTY_CYCLE_1_64 = 0x3F;
    private static final int INIT_MEMORY_ADDRESSING_HORIZ = 0x0;
    private static final int INIT_RESISTER_RATIO = 0x80;
    private static final int INIT_SEGREMAP = 0xA1;
    private static final int INIT_SET_MEMORY_ADDRESSING_MODE = 0x20;

    private static final byte SSD1306_DISPLAY_WRITE = (byte) 0xA4;

    private static final byte[] INIT_PAYLOAD = new byte[]{
            // Step 1: Start with the display off
            0, (byte) COMMAND_DISPLAY_OFF,

            // Step 2: Set up the required communication / power settings
            0, (byte) INIT_SEGREMAP,
            0, (byte) INIT_COMSCAN_DEC,
            0, (byte) INIT_DUTY_CYCLE_1_64,
            0, (byte) INIT_CLK_DIV,
            0, (byte) INIT_RESISTER_RATIO,

            // Step 3: Set display input configuration and start. This will start the display all
            // on, you must transmit START_LINE to present the memory-based mBuffer to the screen
            0, (byte) INIT_SET_MEMORY_ADDRESSING_MODE,
            0, (byte) INIT_MEMORY_ADDRESSING_HORIZ,
            0, (byte) INIT_DISPLAY_OFFSET,
            0, (byte) INIT_DISPLAY_NO_OFFSET,
            0, (byte) COMMAND_DISPLAY_ON,
            0, (byte) INIT_CHARGE_PUMP
    };

    // Holds the i2c payload.
    private final byte[] mBuffer = new byte[((LCDWIDTH * LCDHEIGHT) / 8) + 1];

    public enum ScrollMode {
        RightHorizontal,
        LeftHorizontal,
        VerticalRightHorizontal,
        VerticalLeftHorizontal
    }

    /**
     * Create a new Ssd1306 driver connected to the named I2C bus
     * @param i2cName I2C bus name the display is connected to
     * @throws IOException
     */
    public Ssd1306(String i2cName) throws IOException {
        this(i2cName, I2C_ADDRESS);
    }

    /**
     * Create a new Ssd1306 driver connected to the named I2C bus and address
     * @param i2cName I2C bus name the display is connected to
     * @param i2cAddress I2C address of the display
     * @throws IOException
     */
    public Ssd1306(String i2cName, int i2cAddress) throws IOException {
        I2cDevice device = new PeripheralManagerService().openI2cDevice(i2cName, i2cAddress);
        try {
            init(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new Ssd1306 driver connected to the given device
     * @param device I2C device of the display
     * @throws IOException
     */
    /*package*/ Ssd1306(I2cDevice device) throws IOException {
        init(device);
    }

    /**
     * Recommended start sequence for initializing the communications with the OLED display.
     * WARNING: If you change this code, power cycle your display before testing.
     * @throws IOException
     */
    private void init(I2cDevice device) throws IOException {
        mI2cDevice = device;
        BitmapHelper.bmpToBytes(mBuffer, DATA_OFFSET,
                Bitmap.createBitmap(LCDWIDTH, LCDHEIGHT, Bitmap.Config.ARGB_8888),
                false);
        mBuffer[0] = (byte) COMMAND_START_LINE;

        // Recommended initialization sequence based on http://goo.gl/VSu0C8
        mI2cDevice.write(INIT_PAYLOAD, INIT_PAYLOAD.length);
        stopScroll();
    }

    @Override
    public void close() throws IOException {
        if (mI2cDevice != null) {
            try {
                mI2cDevice.close();
            } finally {
                mI2cDevice = null;
            }
        }
    }

    /**
     * @return the width of the display
     */
    public int getLcdWidth() {
        return LCDWIDTH;
    }

    /**
     * @return the height of the display
     */
    public int getLcdHeight() {
        return LCDHEIGHT;
    }

    /**
     * Clears all pixel data in the display buffer. This will be rendered the next time
     * {@link #show()} is called.
     */
    public void clearPixels() {
        Arrays.fill(mBuffer, DATA_OFFSET, mBuffer.length, (byte) 0);
    }

    /**
     * Sets a specific pixel in the display buffer to on or off. This will be rendered the next time
     * {@link #show()} is called.
     *
     * @param x The horizontal coordinate.
     * @param y The vertical coordinate.
     * @param on Set to true to enable the pixel; false to disable the pixel.
     */
    public void setPixel(int x, int y, boolean on) throws IllegalArgumentException {
        if (x < 0 || y < 0 || x >= LCDWIDTH || y >= LCDHEIGHT) {
            throw new IllegalArgumentException("pixel out of bound:" + x + "," + y);
        }
        if (on) {
            mBuffer[DATA_OFFSET + x + ((y / 8) * LCDWIDTH)] |= (1 << y % 8);
        } else {
            mBuffer[DATA_OFFSET + x + ((y / 8) * LCDWIDTH)] &= ~(1 << y % 8);
        }
    }

    /**
     * Turns the display on and off.
     *
     * @param on Set to true to enable the display; set to false to disable the display.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setDisplayOn(boolean on) throws IOException, IllegalStateException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C Device not open");
        }
        if (on) {
            mI2cDevice.writeRegByte(0, (byte) COMMAND_DISPLAY_ON);
        } else {
            mI2cDevice.writeRegByte(0, (byte) COMMAND_DISPLAY_OFF);
        }
    }

    /**
     * Renders the current pixel data to the screen.
     *
     * @throws IOException
     * @throws IllegalStateException
     */
    public void show() throws IOException, IllegalStateException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C Device not open");
        }
        mI2cDevice.writeRegByte(0, SSD1306_DISPLAY_WRITE);
        mI2cDevice.write(mBuffer, mBuffer.length);
    }

    /**
     * Start scrolling the display horizontally.
     *
     * @param startY The starting row to scroll.
     * @param finishY The ending row to scroll.
     * @param scrollMode Configures the direction that the display contents scroll.
     * @throws IOException
     * @throws IllegalStateException
     */
    public void startScroll(int startY, int finishY, ScrollMode scrollMode)
            throws IOException, IllegalStateException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C Device not open");
        }

        int scrollModeVal = 0;
        switch(scrollMode) {
            case RightHorizontal:
                scrollModeVal = COMMAND_RIGHT_HORIZONTAL_SCROLL;
                break;
            case LeftHorizontal:
                scrollModeVal = COMMAND_LEFT_HORIZONTAL_SCROLL;
                break;
            case VerticalLeftHorizontal:
                scrollModeVal = COMMAND_VERTICAL_AND_LEFT_HORIZONTAL_SCROLL;
                break;
            case VerticalRightHorizontal:
                scrollModeVal = COMMAND_VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL;
                break;
            default:
                // Should never happen
                break;
        }
        byte[] payload = new byte[]{
                0, (byte) scrollModeVal,
                0, 0,
                0, (byte) startY,
                0, 0,
                0, (byte) finishY,
                0, 0,
                0, (byte) 0xFF,
                0, COMMAND_ACTIVATE_SCROLL
        };
        mI2cDevice.write(payload, payload.length);
    }

    /**
     * Stop scrolling the display
     *
     * @throws IOException
     * @throws IllegalStateException
     */
    public void stopScroll() throws IOException, IllegalStateException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C Device not open");
        }
        mI2cDevice.writeRegByte(0, (byte) COMMAND_DEACTIVATE_SCROLL);
    }
}
