/*
 * Copyright 2017 Google Inc.
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

package com.google.android.things.contrib.driver.bh1750;

import android.annotation.SuppressLint;
import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the BH1750 digital ambient light sensor.
 */
@SuppressWarnings({"unused"})
public class Bh1750 implements AutoCloseable {

    private static final String TAG = Bh1750.class.getSimpleName();

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x23;

    /**
     * Alternative I2C address for the sensor.
     */
    public static final int ALTERNATIVE_I2C_ADDRESS = 0x5c;

    // Sensor constants from the datasheet.
    // http://cpre.kmutnb.ac.th/esl/learning/bh1750-light-sensor/bh1750fvi-e_datasheet.pdf

    /**
     * Minimum light in Lux the sensor can measure.
     */
    private static final float MIN_LIGHT_LX = 1f;
    /**
     * Maximum light in Lux the sensor can measure.
     */
    public static final float MAX_LIGHT_LX = 65535f;

    /**
     * Maximum power consumption in micro-amperes.
     */
    public static final float MAX_POWER_CONSUMPTION_UA = 190f;

    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 60f;

    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 8f;

    /**
     * Measurement mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONTINUOUS_HIGH_RES_MODE, CONTINUOUS_HIGH_RES_MODE_2, CONTINUOUS_LOW_RES_MODE,
            ONE_TIME_HIGH_RES_MODE, ONE_TIME_HIGH_RES_MODE_2, ONE_TIME_LOW_RES_MODE})
    public @interface Resolution {
    }

    // Start measurement at 1lx resolution. Measurement time is approx 120ms.
    public static final int CONTINUOUS_HIGH_RES_MODE = 0x10;

    // Start measurement at 0.5lx resolution. Measurement time is approx 120ms.
    public static final int CONTINUOUS_HIGH_RES_MODE_2 = 0x11;

    // Start measurement at 4lx resolution. Measurement time is approx 16ms.
    public static final int CONTINUOUS_LOW_RES_MODE = 0x13;

    //Start measurement at 1lx resolution. Measurement time is approx 120ms.
    //Device is automatically set to Power Down after measurement.
    public static final int ONE_TIME_HIGH_RES_MODE = 0x20;

    // Start measurement at 0.5lx resolution. Measurement time is approx 120ms.
    // Device is automatically set to Power Down after measurement.
    public static final int ONE_TIME_HIGH_RES_MODE_2 = 0x21;

    // Start measurement at 1lx resolution. Measurement time is approx 120ms.
    // Device is automatically set to Power Down after measurement.
    public static final int ONE_TIME_LOW_RES_MODE = 0x23;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({POWER_DOWN, POWER_ON})
    public @interface Mode {
    }

    // Commands
    // No active state
    public static final int POWER_DOWN = 0x00;

    // Waiting for measurement command
    public static final int POWER_ON = 0x01;

    // Reset data register value - not accepted in POWER_DOWN mode
    public static final int RESET = 0x07;

    // Registers
    private static final int BH1750_DATA_REGISTER = 0x00;
    private static final int BH1750_MEASUREMENT_TIME_REGISTER = 0x45;

    private I2cDevice mDevice;
    private int mMode = -1;
    private int mResolution = -1;
    private final byte[] mBuffer = new byte[2]; // for reading sensor values

    /**
     * Create a new BH1750 sensor driver connected on the given bus.
     *
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Bh1750(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new BH1750 sensor driver connected on the given bus and address.
     *
     * @param bus     I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public Bh1750(String bus, int address) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, address);
        try {
            connect(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Create a new BH1750 sensor driver connected to the given I2c device.
     *
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  Bh1750(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        setMode(POWER_ON);
        setResolution(CONTINUOUS_HIGH_RES_MODE);

        // Issue a soft reset
        mDevice.write(new byte[]{(byte) RESET}, 1);
    }

    /**
     * Set the sleep mode of the sensor.
     *
     * @param mode sleep mode.
     * @throws IOException
     */
    @SuppressLint("WrongConstant")
    public void setMode(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        mDevice.write(new byte[]{(byte) mode}, 1);
        if(mResolution != -1) {
            setResolution(mResolution);
        }
        mMode = mode;
    }

    /**
     * Set the resolution mode of the sensor.
     *
     * @param resolution resolution mode.
     * @throws IOException
     */
    public void setResolution(@Resolution int resolution) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        mDevice.write(new byte[]{(byte) resolution}, 1);
        mResolution = resolution;
    }

    public float readLightLevel() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int rawLevel;

        // Read two bytes, which are low and high parts of sensor value
        mDevice.read(mBuffer, 2);
        rawLevel = mBuffer[0] & 0xFF; // Unsigned int
        rawLevel <<= 8;
        rawLevel |= (mBuffer[1] & 0xFF); // Unsigned int

        return this.convertRawValueToLux(rawLevel);
    }

    public float convertRawValueToLux(int rawLevel){
        return rawLevel / 1.2f;
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
}

