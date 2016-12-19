/*
 * Copyright 2016 Macro Yau
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

package com.google.android.things.contrib.driver.hts221;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for the HTS221 relative humidity and temperature sensor.
 *
 * @see <a href="http://www.st.com/resource/en/datasheet/hts221.pdf">HTS221 datasheet</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Hts221 implements AutoCloseable {

    // Sensor constants obtained from the datasheet
    /**
     * Device ID of the sensor.
     */
    public static final int DEVICE_ID = 0xBC; // Value of the WHO_AM_I register
    /**
     * I2C address of the sensor.
     */
    public static final int I2C_ADDRESS = 0x5F;
    /**
     * Minimum relative humidity that the sensor can measure.
     */
    public static final float MIN_HUMIDITY_PERCENT = 0f;
    /**
     * Maximum relative humidity that the sensor can measure.
     */
    public static final float MAX_HUMIDITY_PERCENT = 100f;
    /**
     * Minimum temperature in Celsius that the sensor can measure.
     */
    public static final float MIN_TEMP_C = -40f;
    /**
     * Maximum temperature in Celsius that the sensor can measure.
     */
    public static final float MAX_TEMP_C = 120f;
    /**
     * Maximum power consumption in micro-amperes when measuring relative humidity and temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_UA = 22.5f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 12.5f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 1f;

    /**
     * Humidity average configuration.
     */
    @IntDef({AV_CONF_AVGH_4, AV_CONF_AVGH_8, AV_CONF_AVGH_16, AV_CONF_AVGH_32, AV_CONF_AVGH_64,
            AV_CONF_AVGH_128, AV_CONF_AVGH_256, AV_CONF_AVGH_512})
    public @interface HumidityAverageConfiguration {
    }

    public static final int AV_CONF_AVGH_4 = 0b000;
    public static final int AV_CONF_AVGH_8 = 0b001;
    public static final int AV_CONF_AVGH_16 = 0b010;
    public static final int AV_CONF_AVGH_32 = 0b011; // Default
    public static final int AV_CONF_AVGH_64 = 0b100;
    public static final int AV_CONF_AVGH_128 = 0b101;
    public static final int AV_CONF_AVGH_256 = 0b110;
    public static final int AV_CONF_AVGH_512 = 0b111;

    /**
     * Temperature average configuration.
     */
    @IntDef({AV_CONF_AVGT_2, AV_CONF_AVGT_4, AV_CONF_AVGT_8, AV_CONF_AVGT_16, AV_CONF_AVGT_32,
            AV_CONF_AVGT_64, AV_CONF_AVGT_128, AV_CONF_AVGT_256})
    public @interface TemperatureAverageConfiguration {
    }

    public static final int AV_CONF_AVGT_2 = 0b000;
    public static final int AV_CONF_AVGT_4 = 0b001;
    public static final int AV_CONF_AVGT_8 = 0b010;
    public static final int AV_CONF_AVGT_16 = 0b011; // Default
    public static final int AV_CONF_AVGT_32 = 0b100;
    public static final int AV_CONF_AVGT_64 = 0b101;
    public static final int AV_CONF_AVGT_128 = 0b110;
    public static final int AV_CONF_AVGT_256 = 0b111;

    /**
     * Power mode.
     */
    @IntDef({MODE_POWER_DOWN, MODE_ACTIVE})
    public @interface Mode {
    }

    public static final int MODE_POWER_DOWN = 0;
    public static final int MODE_ACTIVE = 1;

    /**
     * Output data rate configuration.
     */
    @IntDef({HTS221_ODR_ONE_SHOT, HTS221_ODR_1_HZ, HTS221_ODR_7_HZ, HTS221_ODR_12_5_HZ})
    public @interface OutputDataRate {
    }

    public static final int HTS221_ODR_ONE_SHOT = 0b00;
    public static final int HTS221_ODR_1_HZ = 0b01;
    public static final int HTS221_ODR_7_HZ = 0b10;
    public static final int HTS221_ODR_12_5_HZ = 0b11;

    // Registers
    private static final int HTS221_REG_WHO_AM_I = 0x0F; // R
    private static final int HTS221_REG_AV_CONF = 0x10; // R/W
    private static final int HTS221_REG_CTRL_REG1 = 0x20; // R/W
    private static final int HTS221_REG_CTRL_REG2 = 0x21; // R/W
    private static final int HTS221_REG_CTRL_REG3 = 0x22; // R/W
    private static final int HTS221_REG_STATUS_REG = 0x27; // R
    private static final int HTS221_REG_HUMIDITY_OUT_L = 0x28; // R
    private static final int HTS221_REG_HUMIDITY_OUT_H = 0x29; // R
    private static final int HTS221_REG_TEMP_OUT_L = 0x2A; // R
    private static final int HTS221_REG_TEMP_OUT_H = 0x2B; // R

    // Calibration registers
    private static final int HTS221_REG_H0_RH_X2 = 0x30;
    private static final int HTS221_REG_H1_RH_X2 = 0x31;
    private static final int HTS221_REG_T0_DEGC_X8 = 0x32;
    private static final int HTS221_REG_T1_DEGC_X8 = 0x33;
    private static final int HTS221_REG_T1_T0_MSB = 0x35;
    private static final int HTS221_REG_H0_T0_OUT_L = 0x36;
    private static final int HTS221_REG_H0_T0_OUT_H = 0x37;
    private static final int HTS221_REG_H1_T0_OUT_L = 0x3A;
    private static final int HTS221_REG_H1_T0_OUT_H = 0x3B;
    private static final int HTS221_REG_T0_OUT_L = 0x3C;
    private static final int HTS221_REG_T0_OUT_H = 0x3D;
    private static final int HTS221_REG_T1_OUT_L = 0x3E;
    private static final int HTS221_REG_T1_OUT_H = 0x3F;

    // Bit masks for CTRL_REG1
    private static final int HTS221_POWER_DOWN_MASK = 0b10000000;
    private static final int HTS221_BDU_MASK = 0b00000100;
    private static final int HTS221_ODR_MASK = 0b00000011;

    private I2cDevice mDevice;

    private final byte[] mBuffer = new byte[2]; // For reading registers

    private int mMode;
    private boolean mBlockDataUpdate;
    private int mOutputDataRate;
    private int mHumidityAverage;
    private int mTemperatureAverage;
    private float[] mTemperatureCalibration, mHumidityCalibration; // Calibration parameters

    /**
     * Creates a new HTS221 sensor driver connected on the given bus.
     *
     * @param bus the I2C bus the sensor is connected to
     * @throws IOException
     */
    public Hts221(String bus) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        I2cDevice device = pioService.openI2cDevice(bus, I2C_ADDRESS);
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
     * Creates a new HTS221 sensor driver connected to the given I2C device.
     *
     * @param device the I2C device of the sensor
     * @throws IOException
     */
    /*package*/ Hts221(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        if ((mDevice.readRegByte(HTS221_REG_WHO_AM_I) & 0xFF) != DEVICE_ID) {
            throw new IllegalStateException("I2C device is not HTS221 sensor");
        }

        setAveragedSamples(AV_CONF_AVGH_32, AV_CONF_AVGT_16);
        setBlockDataUpdate(true);
        setOutputDataRate(HTS221_ODR_1_HZ);
        setMode(MODE_ACTIVE);

        readCalibration();
    }

    /**
     * Sets the power mode of the sensor as power-down or active.
     *
     * @param mode must be either {@link #MODE_POWER_DOWN} or {@link #MODE_ACTIVE}
     * @throws IOException
     */
    public void setMode(@Mode int mode) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(HTS221_REG_CTRL_REG1) & 0xFF;
        if (mode == MODE_POWER_DOWN) {
            regCtrl &= (~HTS221_POWER_DOWN_MASK & 0xFF);
        } else {
            regCtrl |= HTS221_POWER_DOWN_MASK;
        }
        mDevice.writeRegByte(HTS221_REG_CTRL_REG1, (byte) regCtrl);
        mMode = mode;
    }

    /**
     * Returns the power mode of the sensor.
     *
     * @return <code>true</code> if the sensor is active
     * @see #setMode(int)
     */
    public boolean isEnabled() {
        return mMode == MODE_ACTIVE;
    }

    /**
     * Sets the block data update (BDU) bit in the control register 1 (CTRL_REG1) of the sensor.
     * Reading MSB and LSB of different samples can be avoided by enabling BDU.
     *
     * @param enabled enable BDU if <code>true</code>
     * @throws IOException
     */
    public void setBlockDataUpdate(boolean enabled) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(HTS221_REG_CTRL_REG1) & 0xFF;
        regCtrl &= (~HTS221_BDU_MASK & 0xFF);
        if (enabled) {
            regCtrl |= HTS221_BDU_MASK;
        }
        mDevice.writeRegByte(HTS221_REG_CTRL_REG1, (byte) regCtrl);
        mBlockDataUpdate = enabled;
    }

    /**
     * Returns the status of the block data update mode.
     *
     * @return <code>true</code> if the block data update mode is enabled
     * @see #setBlockDataUpdate(boolean)
     */
    public boolean isBlockDataUpdateEnabled() {
        return mBlockDataUpdate;
    }

    /**
     * Configures the output data rate (ODR) of the humidity and temperature measurements.
     *
     * @param outputDataRate the configuration must be one of {@link #HTS221_ODR_ONE_SHOT},
     *                       {@link #HTS221_ODR_1_HZ}, {@link #HTS221_ODR_7_HZ} or
     *                       {@link #HTS221_ODR_12_5_HZ}
     * @throws IOException
     */
    public void setOutputDataRate(@OutputDataRate int outputDataRate) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(HTS221_REG_CTRL_REG1) & 0xFF;
        regCtrl &= (~HTS221_ODR_MASK & 0xFF);
        regCtrl |= outputDataRate;
        mDevice.writeRegByte(HTS221_REG_CTRL_REG1, (byte) regCtrl);
        mOutputDataRate = outputDataRate;
    }

    /**
     * Returns the configured output data rate.
     *
     * @return the output data rate
     * @see #setOutputDataRate(int)
     */
    public int getOutputDataRate() {
        return mOutputDataRate;
    }

    /**
     * Configures the number of averaged samples for the humidity and temperature measurements.
     *
     * @param humidityAverage    the humidity average configuration must be one of the
     *                           <code>AV_CONF_AVGH*</code> constants
     * @param temperatureAverage the temperature average configuration must be one of the
     *                           <code>AV_CONF_AVGT*</code> constants
     * @throws IOException
     */
    public void setAveragedSamples(@HumidityAverageConfiguration int humidityAverage,
                                   @TemperatureAverageConfiguration int temperatureAverage)
            throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(HTS221_REG_AV_CONF) & 0xC0;
        regCtrl |= humidityAverage | (temperatureAverage << 3);
        mDevice.writeRegByte(HTS221_REG_AV_CONF, (byte) (regCtrl));
    }

    /**
     * Closes the driver and its underlying device.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            try {
                setMode(MODE_POWER_DOWN);
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    private void readCalibration() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        mBuffer[0] = mDevice.readRegByte(HTS221_REG_H0_RH_X2);
        mBuffer[1] = mDevice.readRegByte(HTS221_REG_H1_RH_X2);
        int h0 = (mBuffer[0] & 0xFF);
        int h1 = (mBuffer[1] & 0xFF);

        int h0T0Out = (short) readRegister(HTS221_REG_H0_T0_OUT_L);
        int h1T0Out = (short) readRegister(HTS221_REG_H1_T0_OUT_L);

        int t0 = mDevice.readRegByte(HTS221_REG_T0_DEGC_X8) & 0xFF;
        int t1 = mDevice.readRegByte(HTS221_REG_T1_DEGC_X8) & 0xFF;
        int msb = mDevice.readRegByte(HTS221_REG_T1_T0_MSB) & 0x0F;
        t0 |= (msb & 0x03) << 8;
        t1 |= (msb & 0x0C) << 6;

        int t0Out = (short) readRegister(HTS221_REG_T0_OUT_L);
        int t1Out = (short) readRegister(HTS221_REG_T1_OUT_L);

        mHumidityCalibration = calibrateHumidityParameters(h0, h1, h0T0Out, h1T0Out);
        mTemperatureCalibration = calibrateTemperatureParameters(t0, t1, t0Out, t1Out);
    }

    @VisibleForTesting
    static float[] calibrateHumidityParameters(int h0, int h1, int h0T0Out, int h1T0Out) {
        float[] humidityParameters = new float[2];
        humidityParameters[0] = ((h1 - h0) / 2.0f) / (h1T0Out - h0T0Out);
        humidityParameters[1] = (h0 / 2.0f) - (humidityParameters[0] * h0T0Out);
        return humidityParameters;
    }

    @VisibleForTesting
    static float[] calibrateTemperatureParameters(int t0, int t1, int t0Out, int t1Out) {
        float[] temperatureParameters = new float[2];
        temperatureParameters[0] = ((t1 - t0) / 8.0f) / (t1Out - t0Out);
        temperatureParameters[1] = (t0 / 8.0f) - (temperatureParameters[0] * t0Out);
        return temperatureParameters;
    }

    /**
     * Reads the current humidity.
     *
     * @return the current relative humidity
     */
    public float readHumidity() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        if (isHumidityDataAvailable()) {
            int rawHumidity = (short) readRegister(HTS221_REG_HUMIDITY_OUT_L);
            return compensateSample(rawHumidity, mHumidityCalibration);
        } else {
            throw new IOException("Humidity data is not yet available");
        }
    }

    /**
     * Reads the current temperature.
     *
     * @return the current temperature in degrees Celsius
     */
    public float readTemperature() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        if (isTemperatureDataAvailable()) {
            int rawTemp = (short) readRegister(HTS221_REG_TEMP_OUT_L);
            return compensateSample(rawTemp, mTemperatureCalibration);
        } else {
            throw new IOException("Temperature data is not yet available");
        }
    }

    /**
     * Reads 16 bits from two 8-bit registers at the given address.
     *
     * @param address the address of the least significant byte (LSB)
     * @throws IOException
     */
    private int readRegister(int address) throws IOException {
        synchronized (mBuffer) {
            mBuffer[0] = mDevice.readRegByte(address); // LSB
            mBuffer[1] = mDevice.readRegByte(address + 1); // MSB
            return ((mBuffer[1] & 0xFF) << 8) | (mBuffer[0] & 0xFF);
        }
    }

    /**
     * Validates the availability of updated humidity data.
     *
     * @return <code>true</code> if the humidity data is updated
     * @throws IOException
     */
    private boolean isHumidityDataAvailable() throws IOException {
        return (mDevice.readRegByte(HTS221_REG_STATUS_REG) & 0x02) != 0;
    }

    /**
     * Validates the availability of updated temperature data.
     *
     * @return <code>true</code> if the temperature data is updated
     * @throws IOException
     */
    private boolean isTemperatureDataAvailable() throws IOException {
        return (mDevice.readRegByte(HTS221_REG_STATUS_REG) & 0x01) != 0;
    }

    /**
     * Returns the sensor reading compensated with the given calibration parameters.
     *
     * @param rawValue    the raw sensor reading value
     * @param calibration the calibration parameters, where <code>calibration[0]</code> is the slope
     *                    and <code>calibration[1]</code> is the intercept
     * @return the sensor reading compensated with calibration parameters
     */
    @VisibleForTesting
    static float compensateSample(int rawValue, float[] calibration) {
        return rawValue * calibration[0] + calibration[1];
    }

}
