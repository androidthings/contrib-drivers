/*
 * Copyright 2016, The Android Open Source Project
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

package com.google.androidthings.driver.grove.lcdrgb;

import android.graphics.Color;
import android.hardware.pio.I2cDevice;
import android.hardware.pio.PeripheralManagerService;

import java.io.IOException;

@SuppressWarnings({"unused", "WeakerAccess"})
public class LcdRgbBacklight implements AutoCloseable {
    private static final int RGB_ADDRESS = 0xc4 >> 1;
    private static final int LCD_ADDRESS = 0x7c >> 1;
    private static final int REG_MODE_1 = 0x00;
    private static final int REG_OUTPUT = 0x08;
    private static final int REG_RED = 0x04;
    private static final int REG_GREEN = 0x03;
    private static final int REG_BLUE = 0x02;
    private static final int ENABLE_PWM = 0xff;
    private static final int ENABLE_BACKLIGHT = 0x00;
    private static final int SEND_COMMAND = 0x80;
    private static final int DISPLAY_ON = 0x04;
    private static final int DISPLAY_CONTROL = 0x08;
    private static final int ENTRY_LEFT = 0x02;
    private static final int ENTRY_MODE_SET = 0x04;
    private static final int CLEAR_DISPLAY = 0x01;
    private static final int RETURN_HOME = 0x02;
    private static final int WRITE_CHAR = 0x40;

    private static final byte[] COMMAND_DISPLAY_ON =
            {(byte) SEND_COMMAND, (byte) (DISPLAY_CONTROL | DISPLAY_ON)};
    private static final byte[] COMMAND_ENTRY_MODE =
            {(byte) SEND_COMMAND, (byte) (ENTRY_MODE_SET | ENTRY_LEFT)};
    private static final byte[] COMMAND_CLEAR = {(byte) SEND_COMMAND, (byte) CLEAR_DISPLAY};
    private static final byte[] COMMAND_HOME = {(byte) SEND_COMMAND, (byte) RETURN_HOME};

    private I2cDevice mRgbDevice;
    private I2cDevice mLcdDevice;


    public LcdRgbBacklight(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mRgbDevice = pioService.openI2cDevice(bus, RGB_ADDRESS);
            mLcdDevice = pioService.openI2cDevice(bus,LCD_ADDRESS);
            mRgbDevice.writeRegByte(REG_MODE_1, (byte) ENABLE_BACKLIGHT);
            mRgbDevice.writeRegByte(REG_OUTPUT, (byte) ENABLE_PWM);
            mLcdDevice.write(COMMAND_DISPLAY_ON,COMMAND_DISPLAY_ON.length);
            mLcdDevice.write(COMMAND_ENTRY_MODE,COMMAND_ENTRY_MODE.length);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    public void close() throws IOException {
        if (mRgbDevice != null) {
            try {
                mRgbDevice.close();
            } finally {
                mRgbDevice = null;
            }
        }
        if (mLcdDevice != null) {
            try {
                mRgbDevice.close();
            } finally {
                mRgbDevice = null;
            }
        }
    }

    public void clear() throws IOException, IllegalStateException {
        if (mLcdDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mLcdDevice.write(COMMAND_CLEAR, COMMAND_CLEAR.length);
        mLcdDevice.write(COMMAND_HOME, COMMAND_HOME.length);
    }

    public void setBackground(int color) throws IOException, IllegalStateException {
        if (mRgbDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mRgbDevice.writeRegByte(REG_RED, (byte)Color.red(color));
        mRgbDevice.writeRegByte(REG_GREEN, (byte)Color.green(color));
        mRgbDevice.writeRegByte(REG_BLUE, (byte)Color.blue(color));
    }

    public void write(String message) throws IOException, IllegalStateException {
        if (mLcdDevice == null) {
            throw new IllegalStateException("i2c device not opened");
        }
        mLcdDevice.write(COMMAND_HOME, COMMAND_HOME.length);
        for (byte c : message.getBytes()) {
            byte[] text = {(byte) WRITE_CHAR, c};
            mLcdDevice.write(text, text.length);
        }
    }

}
