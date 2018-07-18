/*
 * Copyright 2018 Google Inc.
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

package com.google.android.things.contrib.driver.adc.ads1xxx;

import com.google.android.things.pio.I2cDevice;

/**
 * Mock instance of ADS1015 ADC
 *
 * Responses modeled after datasheet:
 * https://cdn-shop.adafruit.com/datasheets/ads1015.pdf
 */
public class Ads1015Device implements I2cDevice {

    private static final byte REG_CONVERSION = 0x00;
    private static final byte REG_CONFIG =     0x01;
    private static final byte REG_LO_THRESH =  0x02;
    private static final byte REG_HI_THRESH =  0x03;

    private enum Range {
        V6_144(0x00, 6.144f),
        V4_096(0x01, 4.096f),
        V2_048(0x02, 2.048f),
        V1_024(0x03, 1.024f),
        V0_512(0x04, 0.512f),
        V0_256_1(0x05, 0.256f),
        V0_256_2(0x06, 0.256f),
        V0_256_3(0x07, 0.256f);

        final int value;
        final float voltage;
        Range(int value, float voltage) {
            this.value = value;
            this.voltage = voltage;
        }

        static Range forValue(int value) {
            for (Range item : Range.values()) {
                if (item.value == value) {
                    return item;
                }
            }

            return null;
        }
    }

    // Default values provided in datasheet
    private int mLoThreshold = 0x8000;
    private int mHiThreshold = 0x7FFF;
    private int mConfigValue = 0x8583;
    private Range mCurrentScale = Range.V2_048;
    private float mCurrentVoltage;

    /**
     * Update the internal mock ADC value
     * @param voltage simulated voltage at the input channel
     */
    public void setChannelValue(float voltage) {
        mCurrentVoltage = voltage;
    }

    @Override
    public void close() {
        // Do nothing...not a real device
    }

    @Override
    public void read(byte[] buffer, int length) {
        throw new UnsupportedOperationException("This device does not support raw reads.");
    }

    @Override
    public byte readRegByte(int reg) {
        throw new UnsupportedOperationException("This device does not support single byte reads.");
    }

    @Override
    public short readRegWord(int reg) {
        byte[] buffer = new byte[2];
        readRegBuffer(reg, buffer, buffer.length);
        return (short) bytesToInt(buffer);
    }

    @Override
    public void readRegBuffer(int reg, byte[] buffer, int length) {
        if (length > 2) {
            throw new IllegalArgumentException("Register value cannot exceed 2 bytes");
        }
        switch (reg) {
            case REG_CONVERSION:
                // Convert voltage to value at current scale
                float scale = Math.max(-1f, Math.min(1f, mCurrentVoltage / mCurrentScale.voltage));
                int rawValue = (int) (scale * 0x7FF) << 4; // shift to high 12-bits
                intToBytes(rawValue, buffer);
                break;
            case REG_CONFIG:
                intToBytes(mConfigValue, buffer);
                break;
            case REG_LO_THRESH:
                intToBytes(mLoThreshold, buffer);
                break;
            case REG_HI_THRESH:
                intToBytes(mHiThreshold, buffer);
                break;
            default:
                throw new UnsupportedOperationException("Invalid register address received: " + reg);
        }
    }

    @Override
    public void write(byte[] buffer, int length) {
        throw new UnsupportedOperationException("This device does not support raw writes.");
    }

    @Override
    public void writeRegByte(int reg, byte data) {
        throw new UnsupportedOperationException("This device does not support single byte writes.");
    }

    @Override
    public void writeRegWord(int reg, short data) {
        writeRegBuffer(reg, new byte[] {
                (byte) ((data >> 8) & 0xFF),
                (byte) (data & 0xFF)
        }, 2);
    }

    @Override
    public void writeRegBuffer(int reg, byte[] buffer, int length) {
        if (length > 2) {
            throw new IllegalArgumentException("Register value cannot exceed 2 bytes");
        }
        switch (reg) {
            case REG_CONVERSION:
                throw new UnsupportedOperationException("Conversion register is read-only");
            case REG_CONFIG:
                mConfigValue = bytesToInt(buffer);
                // Parse new scale value
                int newRange = (mConfigValue & 0xE00) >> 9;
                mCurrentScale = Range.forValue(newRange);
                break;
            case REG_LO_THRESH:
                mLoThreshold = bytesToInt(buffer);
                break;
            case REG_HI_THRESH:
                mHiThreshold = bytesToInt(buffer);
                break;
            default:
                throw new UnsupportedOperationException("Invalid register address received: " + reg);
        }
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] << 8) | (bytes[1] & 0xFF));
    }

    private void intToBytes(int value, byte[] buffer) {
        buffer[0] = (byte) ((value >> 8) & 0xFF);
        buffer[1] = (byte) (value & 0xFF);
    }
}
