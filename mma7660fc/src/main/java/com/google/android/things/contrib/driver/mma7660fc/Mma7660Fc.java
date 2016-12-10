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

package com.google.android.things.contrib.driver.mma7660fc;

import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the MMA7660FC 1.5g accelerometer.
 */
public class Mma7660Fc implements AutoCloseable {
    private static final String TAG = Mma7660Fc.class.getSimpleName();
    /**
     * I2C slave address of the MMA7660FC.
     */
    public static final int I2C_ADDRESS = 0x4c;
    static final float MAX_RANGE_G = 1.5f;
    static final float MAX_POWER_UA = 294.f; // at 120hz
    static final float MAX_FREQ_HZ = 120.f;
    static final float MIN_FREQ_HZ = 1.f;

    // acceleration lookup table from:
    // http://garden.seeedstudio.com/images/e/ee/MMA7660FC.pdf
    private static final float[] DRIVER_ACC_LOOKUP_G = {
            0.000f, 0.047f, 0.094f, 0.141f, 0.188f, 0.234f, 0.281f, 0.328f,
            0.375f, 0.422f, 0.469f, 0.516f, 0.563f, 0.609f, 0.656f, 0.703f,
            0.750f, 0.797f, 0.844f, 0.891f, 0.938f, 0.984f, 1.031f, 1.078f,
            1.125f, 1.172f, 1.219f, 1.266f, 1.313f, 1.359f, 1.406f, 1.453f,
            -1.500f, -1.453f, -1.406f, -1.359f, -1.313f, -1.266f, -1.219f, -1.172f,
            -1.125f, -1.078f, -1.031f, -0.984f, -0.938f, -0.891f, -0.844f, -0.797f,
            -0.750f, -0.703f, -0.656f, -0.609f, -0.563f, -0.516f, -0.469f, -0.422f,
            -0.375f, -0.328f, -0.281f, -0.234f, -0.188f, -0.141f, -0.094f, -0.047f
    };

    /**
     * Sampling rate of the measurement.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RATE_120HZ, RATE_64HZ, RATE_32HZ, RATE_16HZ, RATE_8HZ, RATE_4HZ, RATE_2HZ, RATE_1HZ})
    public @interface SamplingRate {}

    public static final int RATE_120HZ = 0b0000;
    public static final int RATE_64HZ  = 0b0001;
    public static final int RATE_32HZ  = 0b0010;
    public static final int RATE_16HZ  = 0b0011;
    public static final int RATE_8HZ   = 0b0100;
    public static final int RATE_4HZ   = 0b0101;
    public static final int RATE_2HZ   = 0b0110;
    public static final int RATE_1HZ   = 0b0111;

    private static final int REG_MODE = 0x07;
    private static final int REG_SAMPLING_RATE = 0x08;

    private I2cDevice mDevice;

    /**
     * Power mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_STANDBY, MODE_ACTIVE})
    public @interface Mode {}

    public static final int MODE_STANDBY = 0; // i2c on, output off, low power
    public static final int MODE_ACTIVE = 1; // i2c on, output on

    /**
     * Create a new MMA7660FC driver connected to the given I2C bus.
     * @param bus
     * @throws IOException
     */
    public Mma7660Fc(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
        try {
            connect(device);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new MMA7660FC driver connected to the given I2C device.
     * @param device
     * @throws IOException
     */
    /*package*/ Mma7660Fc(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        if (mDevice != null) {
            throw new IllegalStateException("device already connected");
        }
        mDevice = device;
        setSamplingRate(RATE_120HZ);
    }

    /**
     * Close the driver and the underlying device.
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
     * Set current power mode.
     * @param mode
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setMode(@Mode int mode) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device not connected");
        }
        mDevice.writeRegByte(REG_MODE, (byte) mode);
    }

    /**
     * Get current power mode.
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    @SuppressWarnings("ResourceType")
    public @Mode int getMode() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device not connected");
        }
        return mDevice.readRegByte(REG_MODE);
    }

    /**
     * Set current sampling rate
     * @param rate
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setSamplingRate(@SamplingRate int rate) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device not connected");
        }
        mDevice.writeRegByte(REG_SAMPLING_RATE, (byte) rate);
    }

    /**
     * Get current sampling rate.
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    @SuppressWarnings("ResourceType")
    public @SamplingRate int getSamplingRate() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device not connected");
        }
        return mDevice.readRegByte(REG_SAMPLING_RATE);
    }

    /**
     * Read an accelerometer sample.
     * @return acceleration over xyz axis in G.
     * @throws IOException
     * @throws IllegalStateException
     */
    public float[] readSample() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("device not connected");
        }
        byte[] sample = new byte[3];
        mDevice.read(sample, 3);
        int x = sample[0] & 0x3f; // 6 bit resolution
        int y = sample[1] & 0x3f;
        int z = sample[2] & 0x3f;
        return new float[]{
                DRIVER_ACC_LOOKUP_G[x],
                DRIVER_ACC_LOOKUP_G[y],
                DRIVER_ACC_LOOKUP_G[z]
        };
    }
}
