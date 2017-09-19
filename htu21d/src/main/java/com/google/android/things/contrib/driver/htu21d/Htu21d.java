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

package com.google.android.things.contrib.driver.htu21d;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the HTU21D environmental sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Htu21d implements AutoCloseable {

    private static final String TAG = Htu21d.class.getSimpleName();

    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x40;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/1899_HTU21D.pdf
    /**
     * Minimum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 125f;
    /**
     * Minimum relative humidity in % the sensor can measure.
     */
    public static final float MIN_RH = 0f;
    /**
     * Maximum relative humidity in % the sensor can measure.
     */
    public static final float MAX_RH = 100f;
    /**
     * Maximum power consumption in micro-amperes.
     */
    public static final float MAX_POWER_CONSUMPTION_UA = 500f;

    /**
     * Sampling mode in bits for temperature and humidity.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_12_14, MODE_8_12, MODE_10_13, MODE_11_11})
    public @interface Mode {}
    public static final int MODE_12_14 = 0;
    public static final int MODE_8_12  = 1;
    public static final int MODE_10_13 = 2;
    public static final int MODE_11_11 = 3;

    /**
     * Registers
     */
    private static final int HTU21D_REG_TEMP_HOLD    = 0xE3;
    private static final int HTU21D_REG_HUM_HOLD     = 0xE5;
    private static final int HTU21D_REG_TEMP_NO_HOLD = 0xF3;
    private static final int HTU21D_REG_HUM_NO_HOLD  = 0xF5;
    private static final int HTU21D_REG_USER_WRITE   = 0xE6;
    private static final int HTU21D_REG_USER_READ    = 0xE7;
    private static final int HTU21D_REG_RESET        = 0xFE;

    private static final int HTU21D_RESOLUTION_MASK = 0b10000001;

    private I2cDevice mDevice;
    private final byte[] mBuffer = new byte[3]; // for reading sensor values
    private int mSensorResolution;

    /**
     * Create a new HTU21D sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException if device cannot be opened
     */
    public Htu21d(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new HTU21D sensor driver connected on the given bus and address.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException if device cannot be opened
     */
    public Htu21d(String bus, int address) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, address);
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
     * Create a new HTU21D sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws IOException if device cannot be opened
     */
    /*package*/ Htu21d(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        // Read current resolution of the sensors.
        mSensorResolution = mDevice.readRegByte(HTU21D_REG_USER_READ) & HTU21D_RESOLUTION_MASK;

        // Issue a soft reset
        mDevice.writeRegByte(HTU21D_REG_RESET, (byte) 1);
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException if device cannot be closed
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
     * Read the current resolution of the sensors
     * @return resolution enum
     */
    public @Mode int getSensorResolution() {
        return mSensorResolution;
    }

    /**
     * Read the current temperature while holding the master.
     * @return the measured temperature in degrees Celsius
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float readTemperature() throws IOException, IllegalStateException {
        return readTemperature(true);
    }

    /**
     * Read the current temperature.
     * @param hold will hold the I2C master while processing the result
     * @return the measured temperature in degrees Celsius
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float readTemperature(boolean hold) throws IOException, IllegalStateException {
        int rawTemp;

        if (hold) {
            rawTemp = readSampleWithHold(HTU21D_REG_TEMP_HOLD);
        } else {
            rawTemp = readSampleWithoutHold(HTU21D_REG_TEMP_NO_HOLD);
        }

        return compensateTemperature(rawTemp);
    }

    /**
     * Read the current relative humidity while holding the master.
     * @return the measured relative humidity in % units
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float readHumidity() throws IOException, IllegalStateException {
        return readHumidity(true);
    }

    /**
     * Read the current relative humidity.
     * @param hold will hold the I2C master while processing the result
     * @return the measured relative humidity in % units
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float readHumidity(boolean hold) throws IOException, IllegalStateException {
        int rawHum;

        if (hold) {
            rawHum = readSampleWithHold(HTU21D_REG_HUM_HOLD);
        } else {
            rawHum = readSampleWithoutHold(HTU21D_REG_HUM_NO_HOLD);
        }

        return compensateHumidity(rawHum);
    }

    /**
     * Read the current temperature and humidity while holding the master.
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is relative humidity in %.
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float[] readTemperatureAndHumidity() throws IOException, IllegalStateException {
        return readTemperatureAndHumidity(true);
    }

    /**
     * Read the current temperature and humidity.
     * @param hold will hold the I2C master while processing the result
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is relative humidity in %.
     * @throws IOException if read fails
     * @throws IllegalStateException if bus is not open
     */
    public float[] readTemperatureAndHumidity(boolean hold) throws IOException, IllegalStateException {
        int rawTemp, rawHumidity;
        float temperature, humidity;

        if (hold) {
            rawTemp = readSampleWithHold(HTU21D_REG_TEMP_HOLD);
            temperature = compensateTemperature(rawTemp);
            rawHumidity = readSampleWithHold(HTU21D_REG_HUM_HOLD);
            humidity = compensateHumidity(rawHumidity);
        } else {
            rawTemp = readSampleWithHold(HTU21D_REG_TEMP_NO_HOLD);
            temperature = compensateTemperature(rawTemp);
            rawHumidity = readSampleWithHold(HTU21D_REG_HUM_NO_HOLD);
            humidity = compensateHumidity(rawHumidity);
        }

        return new float[]{temperature, humidity};
    }

    /**
     * Reads a 14 bit sample while holding the master. See datasheet page 11
     * @param address location of address to read
     * @return register value
     * @throws IOException if address fails to read
     * @throws IllegalStateException if bus is not open
     */
    private int readSampleWithHold(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        synchronized (mBuffer) {
            try {
                mDevice.readRegBuffer(address, mBuffer, 3);
            } catch (IOException e) {
                // NACK can occur after 2nd byte to omit crc. See datasheet page 12
                return ((mBuffer[0] & 0xff) << 8) | (mBuffer[1] & 0xfc);
            }
            long crc = calculateCRC8(new byte[]{mBuffer[0], mBuffer[1]});
            if ((((byte) crc) & 0xff) == (mBuffer[2] & 0xff)) {
                // msb[7:0] lsb[7:2]
            	int msb = mBuffer[0] & 0xff;
                int lsb = mBuffer[1] & 0xfc;
                return (msb << 8 | lsb);
            } else {
                throw new IOException("CRC check failed " + (((byte) crc) & 0xff) + " != " + (mBuffer[2] & 0xff));
            }
        }
    }

    /**
     * Reads a 14 bit sample without holding the master. See datasheet page 11
     * @param address location of address to read
     * @return register value
     * @throws IOException if address fails to read
     * @throws IllegalStateException if bus is not open
     */
    private int readSampleWithoutHold(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        synchronized (mBuffer) {
            mDevice.write(new byte[]{(byte)address}, 1);
            for (int i=0; i<50; i++) {
                try {
                    mDevice.read(mBuffer, 2);
                    // msb[7:0] lsb[7:2]
                    int msb = mBuffer[0] & 0xff;
                    int lsb = mBuffer[1] & 0xfc; // last 2 bits are status
                    // Convert to 14 bit integer
                    return (msb << 8 | lsb);
                } catch (IOException e) {
                    // "NACK" while device is converting the result
                }
            }
            throw new IOException("Failed to read value from " + address);
        }
    }

    /**
     * Calculates the 8 bit Cyclic redundancy check of the input buffer with polynominal 0x31
     * @param input buffer of bytes to be checked
     * @return computed CRC8
     * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
     */
    @VisibleForTesting
    static long calculateCRC8(final byte[] input) {
        final int poly = 0x31;
        byte crc = 0;
        for (final byte i : input) {
            crc ^= i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x80) != 0) {
                    crc = (byte) ((crc << 1) ^ poly);
                } else {
                    crc <<= 1;
                }
            }
            crc &= 0xFF;
        }
        return crc & 0xFF;
    }

    /**
     * Formula T = -46.85 + 175.72 * ST / 2^16 from datasheet p14
     * @param rawTemp raw temperature value read from device
     * @return temperature in °C range from -40°C to +125°C
     */
    @VisibleForTesting
    static float compensateTemperature(final int rawTemp) {
        final int temp = ((21965 * rawTemp) >> 13) - 46850;
        return (float) temp / 1000;
    }

    /**
     * Formula RH = -6 + 125 * SRH / 2^16 from datasheet p14
     * @param rawHumidity raw humidity value read from device
     * @return relative humidity RH% range from 0-100
     */
    @VisibleForTesting
    static float compensateHumidity(final int rawHumidity) {
        final int hum = ((15625 * rawHumidity) >> 13) - 6000;
        return (float) hum / 1000;
    }
}
