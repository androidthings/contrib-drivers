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

package com.google.android.things.contrib.driver.bmx280;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the BMP/BME 280 temperature sensor.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Bmx280 implements AutoCloseable {

    private static final String TAG = Bmx280.class.getSimpleName();

    /**
     * Chip ID for the BMP280
     */
    public static final int CHIP_ID_BMP280 = 0x58;
    /**
     * Chip ID for the BME280
     */
    public static final int CHIP_ID_BME280 = 0x60;
    /**
     * Default I2C address for the sensor.
     */
    public static final int DEFAULT_I2C_ADDRESS = 0x77;
    @Deprecated
    public static final int I2C_ADDRESS = DEFAULT_I2C_ADDRESS;

    // Sensor constants from the datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    /**
     * Mininum temperature in Celsius the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius the sensor can measure.
     */
    public static final float MAX_TEMP_C = 85f;
    /**
     * Minimum pressure in hPa the sensor can measure.
     */
    public static final float MIN_PRESSURE_HPA = 300f;
    /**
     * Maximum pressure in hPa the sensor can measure.
     */
    public static final float MAX_PRESSURE_HPA = 1100f;
    /**
     * Mininum humidity in RH the sensor can measure.
     */
    public static final float MIN_HUM_RH = 0f;
    /**
     * Maximum humidity in RH the sensor can measure.
     */
    public static final float MAX_HUM_RH = 100f;
    /**
     * Maximum power consumption in micro-amperes when measuring temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_TEMP_UA = 325f;
    /**
     * Maximum power consumption in micro-amperes when measuring pressure.
     */
    public static final float MAX_POWER_CONSUMPTION_PRESSURE_UA = 720f;
    /**
     * Maximum power consumption in micro-amperes when measuring humidity.
     */
    public static final float MAX_POWER_CONSUMPTION_HUMIDITY_UA = 340f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 181f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 23.1f;

    /**
     * Power mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_SLEEP, MODE_FORCED, MODE_NORMAL})
    public @interface Mode {}
    public static final int MODE_SLEEP = 0;
    public static final int MODE_FORCED = 1;
    public static final int MODE_NORMAL = 2;

    /**
     * Oversampling multiplier.
     * TODO: add other oversampling modes
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OVERSAMPLING_SKIPPED, OVERSAMPLING_1X})
    public @interface Oversampling {}
    public static final int OVERSAMPLING_SKIPPED = 0;
    public static final int OVERSAMPLING_1X = 1;

    // Registers
    private static final int BMX280_REG_TEMP_CALIB_1 = 0x88;
    private static final int BMX280_REG_TEMP_CALIB_2 = 0x8A;
    private static final int BMX280_REG_TEMP_CALIB_3 = 0x8C;

    private static final int BMX280_REG_PRESS_CALIB_1 = 0x8E;
    private static final int BMX280_REG_PRESS_CALIB_2 = 0x90;
    private static final int BMX280_REG_PRESS_CALIB_3 = 0x92;
    private static final int BMX280_REG_PRESS_CALIB_4 = 0x94;
    private static final int BMX280_REG_PRESS_CALIB_5 = 0x96;
    private static final int BMX280_REG_PRESS_CALIB_6 = 0x98;
    private static final int BMX280_REG_PRESS_CALIB_7 = 0x9A;
    private static final int BMX280_REG_PRESS_CALIB_8 = 0x9C;
    private static final int BMX280_REG_PRESS_CALIB_9 = 0x9E;

    private static final int BME280_REG_HUM_CALIB_1 = 0xA1;
    private static final int BME280_REG_HUM_CALIB_2 = 0xE1;
    private static final int BME280_REG_HUM_CALIB_3 = 0xE3;
    private static final int BME280_REG_HUM_CALIB_4 = 0xE4;
    private static final int BME280_REG_HUM_CALIB_5 = 0xE5;
    private static final int BME280_REG_HUM_CALIB_6 = 0xE6;
    private static final int BME280_REG_HUM_CALIB_7 = 0xE7;

    private static final int BMX280_REG_ID = 0xD0;

    @VisibleForTesting
    static final int BMX280_REG_CTRL = 0xF4;
    @VisibleForTesting
    static final int BME280_REG_CTRL_HUM = 0xF2;

    private static final int BMX280_REG_PRESS = 0xF7;
    private static final int BMX280_REG_TEMP = 0xFA;
    private static final int BME280_REG_HUM = 0xFD;

    private static final int BMX280_POWER_MODE_MASK = 0b00000011;
    private static final int BMX280_POWER_MODE_SLEEP = 0b00000000;
    private static final int BMX280_POWER_MODE_NORMAL = 0b00000011;
    private static final int BMX280_OVERSAMPLING_PRESSURE_MASK = 0b00011100;
    private static final int BMX280_OVERSAMPLING_PRESSURE_BITSHIFT = 2;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_MASK = 0b11100000;
    private static final int BMX280_OVERSAMPLING_TEMPERATURE_BITSHIFT = 5;

    private I2cDevice mDevice;
    private final int[] mTempCalibrationData = new int[3];
    private final int[] mPressureCalibrationData = new int[9];
    private final int[] mHumCalibrationData = new int[6];
    private final byte[] mBuffer = new byte[3]; // for reading sensor values
    private boolean mEnabled;
    private boolean mHasHumiditySensor;
    private int mChipId;
    private int mMode;
    private int mPressureOversampling;
    private int mTemperatureOversampling;
    private int mHumidityOversampling;

    /**
     * Create a new BMP/BME280 sensor driver connected on the given bus.
     * @param bus I2C bus the sensor is connected to.
     * @throws IOException
     */
    public Bmx280(String bus) throws IOException {
        this(bus, DEFAULT_I2C_ADDRESS);
    }

    /**
     * Create a new BMP/BME280 sensor driver connected on the given bus and address.
     * @param bus I2C bus the sensor is connected to.
     * @param address I2C address of the sensor.
     * @throws IOException
     */
    public Bmx280(String bus, int address) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
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
     * Create a new BMP/BME280 sensor driver connected to the given I2c device.
     * @param device I2C device of the sensor.
     * @throws IOException
     */
    /*package*/  Bmx280(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        mChipId = mDevice.readRegByte(BMX280_REG_ID);
        mHasHumiditySensor = mChipId == CHIP_ID_BME280;

        // Read temperature calibration data (3 words). First value is unsigned.
        mTempCalibrationData[0] = mDevice.readRegWord(BMX280_REG_TEMP_CALIB_1) & 0xffff;
        mTempCalibrationData[1] = mDevice.readRegWord(BMX280_REG_TEMP_CALIB_2);
        mTempCalibrationData[2] = mDevice.readRegWord(BMX280_REG_TEMP_CALIB_3);
        // Read pressure calibration data (9 words). First value is unsigned.
        mPressureCalibrationData[0] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_1) & 0xffff;
        mPressureCalibrationData[1] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_2);
        mPressureCalibrationData[2] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_3);
        mPressureCalibrationData[3] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_4);
        mPressureCalibrationData[4] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_5);
        mPressureCalibrationData[5] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_6);
        mPressureCalibrationData[6] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_7);
        mPressureCalibrationData[7] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_8);
        mPressureCalibrationData[8] = mDevice.readRegWord(BMX280_REG_PRESS_CALIB_9);

        if (mHasHumiditySensor) {
            // Read humidity calibration data
            mHumCalibrationData[0] = mDevice.readRegByte(BME280_REG_HUM_CALIB_1) & 0xff; // dig_H1
            mHumCalibrationData[1] = mDevice.readRegWord(BME280_REG_HUM_CALIB_2); // dig_H2
            mHumCalibrationData[2] = mDevice.readRegByte(BME280_REG_HUM_CALIB_3) & 0xff; // dig_H3
            int E4 = mDevice.readRegByte(BME280_REG_HUM_CALIB_4) & 0xff;
            int E5 = mDevice.readRegByte(BME280_REG_HUM_CALIB_5) & 0xff;
            int E6 = mDevice.readRegByte(BME280_REG_HUM_CALIB_6) & 0xff;
            int E7 = mDevice.readRegByte(BME280_REG_HUM_CALIB_7);
            mHumCalibrationData[3] = (E4 << 4) | (E5 & 0x0F); // dig_H4
            mHumCalibrationData[4] = (E6 << 4) | (E5 >> 4); // dig_H5
            mHumCalibrationData[5] = E7; // dig_H6
        }
    }

    /**
     * Set the power mode of the sensor.
     * @param mode power mode.
     * @throws IOException
     */
    public void setMode(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (mode == MODE_SLEEP) {
            regCtrl &= ~BMX280_POWER_MODE_MASK;
        } else {
            regCtrl |= BMX280_POWER_MODE_NORMAL;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte)(regCtrl));
        mMode = mode;
    }

    /**
     * Set oversampling multiplier for the temperature measurement.
     * @param oversampling temperature oversampling multiplier.
     * @throws IOException
     */
    public void setTemperatureOversampling(@Oversampling int oversampling) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (oversampling == OVERSAMPLING_SKIPPED) {
            regCtrl &= ~BMX280_OVERSAMPLING_TEMPERATURE_MASK;
        } else {
            regCtrl |= 1 << BMX280_OVERSAMPLING_TEMPERATURE_BITSHIFT;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte)(regCtrl));
        mTemperatureOversampling = oversampling;
    }

    /**
     * Set oversampling multiplier for the pressure measurement.
     * @param oversampling pressure oversampling multiplier.
     * @throws IOException
     */
    public void setPressureOversampling(@Oversampling int oversampling) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }

        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        if (oversampling == OVERSAMPLING_SKIPPED) {
            regCtrl &= ~BMX280_OVERSAMPLING_PRESSURE_MASK;
        } else {
            regCtrl |= 1 << BMX280_OVERSAMPLING_PRESSURE_BITSHIFT;
        }
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte)(regCtrl));
        mPressureOversampling = oversampling;
    }

    /**
     * Set oversampling multiplier for the humidity measurement.
     * @param oversampling humidity oversampling multiplier.
     * @throws IOException
     */
    public void setHumidityOversampling(@Oversampling int oversampling) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        if (!hasHumiditySensor()) {
            throw new UnsupportedOperationException("This chip " + mChipId +
                    " doesn't have humidity sensor");
        }

        mDevice.writeRegByte(BME280_REG_CTRL_HUM, (byte)(oversampling));

        // from https://cdn-shop.adafruit.com/datasheets/BST-BME280_DS001-10.pdf:
        // "Changes to ctrl_hum only become effective after a write operation to ctrl_meas"
        int regCtrl = mDevice.readRegByte(BMX280_REG_CTRL) & 0xff;
        mDevice.writeRegByte(BMX280_REG_CTRL, (byte)(regCtrl));

        mHumidityOversampling = oversampling;
    }

    public @Oversampling int getTemperatureOversampling() {
        return mTemperatureOversampling;
    }

    public @Oversampling int getPressureOversampling() {
        return mPressureOversampling;
    }

    public @Oversampling int getHumidityOversampling() {
        return mHumidityOversampling;
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
     * Returns the sensor chip ID.
     */
    public int getChipId() {
        return mChipId;
    }

    /**
     * Returns if the associated BMX280 sensor is able to measure humidity.
     */
    public boolean hasHumiditySensor() {
        return mHasHumiditySensor;
    }

    @VisibleForTesting
    void setHasHumiditySensor(boolean hasHumiditySensor) {
        mHasHumiditySensor = hasHumiditySensor;
    }

    /**
     * Read the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException, IllegalStateException {
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        int rawTemp = readSample(BMX280_REG_TEMP);
        return compensateTemperature(rawTemp, mTempCalibrationData)[0];
    }

    /**
     * Read the current barometric pressure. If you also intend to use temperature readings, prefer
     * {@link #readTemperatureAndPressure()} instead since sampling the current pressure already
     * requires sampling the current temperature.
     *
     * @return the barometric pressure in hPa units
     * @throws IOException
     */
    public float readPressure() throws IOException, IllegalStateException {
        float[] values = readTemperatureAndPressure();
        return values[1];
    }

    /**
     * Read the current humidity.
     *
     * @return the current relative humidity in RH percentage (100f means totally saturated air)
     */
    public float readHumidity() throws IOException, IllegalStateException {
        if (!hasHumiditySensor()) {
            throw new UnsupportedOperationException("This chip " + mChipId +
                    " doesn't have humidity sensor");
        }
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        if (mHumidityOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("Humidity oversampling is skipped");
        }

        int rawTemp = readSample(BMX280_REG_TEMP);

        float[] temperatures = compensateTemperature(rawTemp, mTempCalibrationData);

        int rawHum = readSample16(BME280_REG_HUM);
        return compensateHumidity(rawHum, temperatures[1], mHumCalibrationData);
    }

    /**
     * Read the current temperature and barometric pressure.
     *
     * @return a 2-element array. The first element is temperature in degrees Celsius, and the
     * second is barometric pressure in hPa units.
     * @throws IOException
     */
    public float[] readTemperatureAndPressure() throws IOException, IllegalStateException {
        if (mTemperatureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("temperature oversampling is skipped");
        }
        if (mPressureOversampling == OVERSAMPLING_SKIPPED) {
            throw new IllegalStateException("pressure oversampling is skipped");
        }
        // The pressure compensation formula requires the fine temperature reading, so we always
        // read temperature first.
        int rawTemp = readSample(BMX280_REG_TEMP);
        float[] temperatures = compensateTemperature(rawTemp, mTempCalibrationData);
        int rawPressure = readSample(BMX280_REG_PRESS);
        float pressure = compensatePressure(rawPressure, temperatures[1], mPressureCalibrationData);
        return new float[]{temperatures[0], pressure};
    }

    /**
     * Reads 20 bits from the given address.
     * @throws IOException
     */
    private int readSample(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        synchronized (mBuffer) {
            mDevice.readRegBuffer(address, mBuffer, 3);
            // msb[7:0] lsb[7:0] xlsb[7:4]
            int msb = mBuffer[0] & 0xff;
            int lsb = mBuffer[1] & 0xff;
            int xlsb = mBuffer[2] & 0xf0;
            // Convert to 20bit integer
            return (msb << 16 | lsb << 8 | xlsb) >> 4;
        }
    }

    /**
     * Reads 16 bits from the given address.
     * @throws IOException
     */
    private int readSample16(int address) throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }
        synchronized (mBuffer) {
            // Reading a byte buffer instead of a short to avoid having to deal with
            // platform-specific endianness.
            mDevice.readRegBuffer(address, mBuffer, 2);
            // msb[7:0] lsb[7:0]
            int msb = mBuffer[0] & 0xff;
            int lsb = mBuffer[1] & 0xff;
            return msb << 8 | lsb;
        }
    }

    // Compensation formula from the BMP280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    @VisibleForTesting
    static float[] compensateTemperature(int rawTemp, int[] calibrationData) {
        int dig_T1 = calibrationData[0];
        int dig_T2 = calibrationData[1];
        int dig_T3 = calibrationData[2];

        float adc_T = (float) rawTemp;
        float var1 = (adc_T / 16384f - ((float) dig_T1) / 1024f) * ((float) dig_T2);
        float var2 = ((adc_T / 131072f - ((float) dig_T1) / 8192f) * (adc_T / 131072f
                - ((float) dig_T1) / 8192f)) * ((float) dig_T3);
        float fineTemp = var1 + var2;
        return new float[]{fineTemp / 5120.0f, fineTemp};
    }

    // Compensation formula from the BMP280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BMP280-DS001-11.pdf
    @VisibleForTesting
    static float compensatePressure(int rawPressure, float fineTemperature,
            int[] calibration) {
        int dig_P1 = calibration[0];
        int dig_P2 = calibration[1];
        int dig_P3 = calibration[2];
        int dig_P4 = calibration[3];
        int dig_P5 = calibration[4];
        int dig_P6 = calibration[5];
        int dig_P7 = calibration[6];
        int dig_P8 = calibration[7];
        int dig_P9 = calibration[8];

        float var1 = (fineTemperature / 2.0f) - 64000.0f;
        float var2 = var1 * var1 * ((float) dig_P6) / 32768.0f;
        var2 = var2 + var1 * ((float) dig_P5) * 2.0f;
        var2 = (var2 / 4.0f) + (((float) dig_P4) * 65536.0f);
        var1 = (((float) dig_P3) * var1 * var1 / 524288.0f + ((float) dig_P2) * var1) / 524288.0f;
        var1 = (1.0f + var1 / 32768.0f) * ((float) dig_P1);
        if (var1 == 0.0) {
            return 0; // avoid exception caused by division by zero
        }
        float p = 1048576.0f - (float) rawPressure;
        p = (p - (var2 / 4096.0f)) * 6250.0f / var1;
        var1 = ((float) dig_P9) * p * p / 2147483648.0f;
        var2 = p * ((float) dig_P8) / 32768.0f;
        p = p + (var1 + var2 + ((float) dig_P7)) / 16.0f;
        // p is in Pa, convert to hPa
        return p / 100.0f;
    }

    // Compensation formula from the BME280 datasheet.
    // https://cdn-shop.adafruit.com/datasheets/BST-BME280_DS001-10.pdf
    @VisibleForTesting
    static float compensateHumidity(int rawHumidity, float fineTemperature, int[] calibration)
    {
        int dig_H1 = calibration[0];
        int dig_H2 = calibration[1];
        int dig_H3 = calibration[2];
        int dig_H4 = calibration[3];
        int dig_H5 = calibration[4];
        int dig_H6 = calibration[5];
        float var_H;
        var_H = (fineTemperature - 76800f);
        var_H = (rawHumidity - (((float) dig_H4) * 64f + ((float) dig_H5) / 16384f * var_H)) *
                (((float) dig_H2) / 65536f * (1f + ((float) dig_H6) / 67108864f * var_H *
                        (1f + ((float) dig_H3) / 67108864f * var_H)));
        var_H = var_H * (1f - ((float) dig_H1) * var_H / 524288f);
        if (var_H > 100)
            var_H = 100f;
        else if (var_H < 0)
            var_H = 0f;
        return var_H;
    }
}
