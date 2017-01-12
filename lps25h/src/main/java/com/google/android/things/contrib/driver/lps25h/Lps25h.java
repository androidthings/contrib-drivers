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

package com.google.android.things.contrib.driver.lps25h;

import android.support.annotation.IntDef;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for the LPS25H pressure sensor.
 *
 * @see <a href="http://www.st.com/resource/en/datasheet/lps25h.pdf">LPS25H datasheet</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Lps25h implements AutoCloseable {

    // Sensor constants obtained from the datasheet
    /**
     * Device ID of the sensor.
     */
    public static final int DEVICE_ID = 0xBD; // Value of the WHO_AM_I register
    /**
     * I2C address of the sensor.
     */
    public static final int I2C_ADDRESS = 0x5C;
    /**
     * Minimum pressure in hectopascals that the sensor can measure.
     */
    public static final float MIN_PRESSURE_HPA = 260f;
    /**
     * Maximum pressure in hectopascals that the sensor can measure.
     */
    public static final float MAX_PRESSURE_HPA = 1260f;
    /**
     * Minimum temperature in Celsius that the sensor can measure.
     */
    public static final float MIN_TEMP_C = -30f;
    /**
     * Maximum temperature in Celsius that the sensor can measure.
     */
    public static final float MAX_TEMP_C = 105f;
    /**
     * Maximum power consumption in micro-amperes when measuring pressure and temperature.
     */
    public static final float MAX_POWER_CONSUMPTION_UA = 25f;
    /**
     * Maximum frequency of the measurements.
     */
    public static final float MAX_FREQ_HZ = 25f;
    /**
     * Minimum frequency of the measurements.
     */
    public static final float MIN_FREQ_HZ = 1f;

    /**
     * Pressure average configuration.
     */
    @IntDef({RES_CONF_AVGP_8, RES_CONF_AVGP_32, RES_CONF_AVGP_128, RES_CONF_AVGP_512})
    public @interface PressureAverageConfiguration {
    }

    public static final int RES_CONF_AVGP_8 = 0b00;
    public static final int RES_CONF_AVGP_32 = 0b01; // Default
    public static final int RES_CONF_AVGP_128 = 0b10;
    public static final int RES_CONF_AVGP_512 = 0b11;

    /**
     * Temperature average configuration.
     */
    @IntDef({RES_CONF_AVGT_8, RES_CONF_AVGT_16, RES_CONF_AVGT_32, RES_CONF_AVGT_64})
    public @interface TemperatureAverageConfiguration {
    }

    public static final int RES_CONF_AVGT_8 = 0b00;
    public static final int RES_CONF_AVGT_16 = 0b01; // Default
    public static final int RES_CONF_AVGT_32 = 0b10;
    public static final int RES_CONF_AVGT_64 = 0b11;

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
    @IntDef({LPS25H_ODR_ONE_SHOT, LPS25H_ODR_1_HZ, LPS25H_ODR_7_HZ, LPS25H_ODR_12_5_HZ,
            LPS25H_ODR_25_HZ})
    public @interface OutputDataRate {
    }

    public static final int LPS25H_ODR_ONE_SHOT = 0b000;
    public static final int LPS25H_ODR_1_HZ = 0b001;
    public static final int LPS25H_ODR_7_HZ = 0b010;
    public static final int LPS25H_ODR_12_5_HZ = 0b011;
    public static final int LPS25H_ODR_25_HZ = 0b100;

    // Registers
    private static final int LPS25H_REG_REF_P_XL = 0x08; // R/W
    private static final int LPS25H_REG_REF_P_L = 0x09; // R/W
    private static final int LPS25H_REG_REF_P_H = 0x0A; // R/W
    private static final int LPS25H_REG_WHO_AM_I = 0x0F; // R
    private static final int LPS25H_REG_RES_CONF = 0x10; // R/W
    private static final int LPS25H_REG_CTRL_REG1 = 0x20; // R/W
    private static final int LPS25H_REG_CTRL_REG2 = 0x21; // R/W
    private static final int LPS25H_REG_CTRL_REG3 = 0x22; // R/W
    private static final int LPS25H_REG_CTRL_REG4 = 0x23; // R/W
    private static final int LPS25H_REG_INT_CFG = 0x24; // R/W
    private static final int LPS25H_REG_INT_SOURCE = 0x25; // R
    private static final int LPS25H_REG_STATUS_REG = 0x27; // R
    private static final int LPS25H_REG_PRESS_OUT_XL = 0x28; // R
    private static final int LPS25H_REG_PRESS_OUT_L = 0x29; // R
    private static final int LPS25H_REG_PRESS_OUT_H = 0x2A; // R
    private static final int LPS25H_REG_TEMP_OUT_L = 0x2B; // R
    private static final int LPS25H_REG_TEMP_OUT_H = 0x2C; // R
    private static final int LPS25H_REG_FIFO_CTRL = 0x2E; // R/W
    private static final int LPS25H_REG_FIFO_STATUS = 0x2F; // R
    private static final int LPS25H_REG_THS_P_L = 0x30; // R/W
    private static final int LPS25H_REG_THS_P_H = 0x31; // R/W
    private static final int LPS25H_REG_RPDS_L = 0x39; // R/W
    private static final int LPS25H_REG_RPDS_H = 0x3A; // R/W

    // Bit masks for CTRL_REG1
    private static final int LPS25H_POWER_DOWN_MASK = 0b10000000;
    private static final int LPS25H_BDU_MASK = 0b00000100;
    private static final int LPS25H_ODR_MASK = 0b01110000;

    private I2cDevice mDevice;

    private final byte[] mBuffer = new byte[3]; // For reading registers

    private int mMode;
    private boolean mBlockDataUpdate;
    private int mOutputDataRate;

    /**
     * Creates a new LPS25H sensor driver connected on the given bus.
     *
     * @param bus the I2C bus the sensor is connected to
     * @throws IOException
     */
    public Lps25h(String bus) throws IOException {
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
     * Creates a new LPS25H sensor driver connected to the given I2C device.
     *
     * @param device the I2C device of the sensor
     * @throws IOException
     */
    /*package*/ Lps25h(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        mDevice = device;

        if ((mDevice.readRegByte(LPS25H_REG_WHO_AM_I) & 0xFF) != DEVICE_ID) {
            throw new IllegalStateException("I2C device is not LPS25H sensor");
        }

        setBlockDataUpdate(true);
        setOutputDataRate(LPS25H_ODR_1_HZ);
        setMode(MODE_ACTIVE);

        // Suggested configuration in the datasheet
        mDevice.writeRegByte(LPS25H_REG_RES_CONF, (byte) 0x05);
        mDevice.writeRegByte(LPS25H_REG_FIFO_CTRL, (byte) 0xC0);
        mDevice.writeRegByte(LPS25H_REG_CTRL_REG2, (byte) 0x40);
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

        int regCtrl = mDevice.readRegByte(LPS25H_REG_CTRL_REG1) & 0xFF;
        if (mode == MODE_POWER_DOWN) {
            regCtrl &= (~LPS25H_POWER_DOWN_MASK & 0xFF);
        } else {
            regCtrl |= LPS25H_POWER_DOWN_MASK;
        }
        mDevice.writeRegByte(LPS25H_REG_CTRL_REG1, (byte) regCtrl);
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

        int regCtrl = mDevice.readRegByte(LPS25H_REG_CTRL_REG1) & 0xFF;
        regCtrl &= (~LPS25H_BDU_MASK & 0xFF);
        if (enabled) {
            regCtrl |= LPS25H_BDU_MASK;
        }
        mDevice.writeRegByte(LPS25H_REG_CTRL_REG1, (byte) regCtrl);
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
     * Configures the output data rate (ODR) of the pressure and temperature measurements.
     *
     * @param outputDataRate the configuration must be one of {@link #LPS25H_ODR_ONE_SHOT},
     *                       {@link #LPS25H_ODR_1_HZ}, {@link #LPS25H_ODR_7_HZ},
     *                       {@link #LPS25H_ODR_12_5_HZ} or {@link #LPS25H_ODR_25_HZ}
     * @throws IOException
     */
    public void setOutputDataRate(@OutputDataRate int outputDataRate) throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(LPS25H_REG_CTRL_REG1) & 0xFF;
        regCtrl &= (~LPS25H_ODR_MASK & 0xFF);
        regCtrl |= (outputDataRate << 4);
        mDevice.writeRegByte(LPS25H_REG_CTRL_REG1, (byte) regCtrl);
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
     * Configures the number of averaged samples for the pressure and temperature measurements.
     *
     * @param pressureAverage    the pressure average configuration must be one of the
     *                           <code>RES_CONF_AVGP*</code> constants
     * @param temperatureAverage the temperature average configuration must be one of the
     *                           <code>RES_CONF_AVGT*</code> constants
     * @throws IOException
     */
    public void setAveragedSamples(@PressureAverageConfiguration int pressureAverage,
                                   @TemperatureAverageConfiguration int temperatureAverage)
            throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        int regCtrl = mDevice.readRegByte(LPS25H_REG_RES_CONF) & 0xF0;
        regCtrl |= pressureAverage | (temperatureAverage << 2);
        mDevice.writeRegByte(LPS25H_REG_RES_CONF, (byte) (regCtrl));
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

    /**
     * Reads the current pressure.
     *
     * @return the current pressure in hectopascals or millibars
     */
    public float readPressure() throws IOException {
        if (mDevice == null) {
            throw new IllegalStateException("I2C device is already closed");
        }

        if (isPressureDataAvailable()) {
            int rawPressure = readThreeRegisters(LPS25H_REG_PRESS_OUT_XL);
            return (float) rawPressure / 4096f;
        } else {
            throw new IOException("Pressure data is not yet available");
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
            int rawTemp = (short) readTwoRegisters(LPS25H_REG_TEMP_OUT_L);
            return (float) rawTemp / 480f + 42.5f;
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
    private int readTwoRegisters(int address) throws IOException {
        mDevice.readRegBuffer(address | 0x80, mBuffer, 2); // 0x80 for auto increment in the address
        return ((mBuffer[1] & 0xFF) << 8) | (mBuffer[0] & 0xFF);
    }

    /**
     * Reads 24 bits from three 8-bit registers at the given address.
     *
     * @param address the address of the least significant byte (LSB)
     * @throws IOException
     */
    private int readThreeRegisters(int address) throws IOException {
        mDevice.readRegBuffer(address | 0x80, mBuffer, 3); // 0x80 for auto increment in the address
        return ((mBuffer[2] & 0xFF) << 16) | ((mBuffer[1] & 0xFF) << 8) | (mBuffer[0] & 0xFF);
    }

    /**
     * Validates the availability of updated pressure data.
     *
     * @return <code>true</code> if the pressure data is updated
     * @throws IOException
     */
    private boolean isPressureDataAvailable() throws IOException {
        return (mDevice.readRegByte(LPS25H_REG_STATUS_REG) & 0x02) != 0;
    }

    /**
     * Validates the availability of updated temperature data.
     *
     * @return <code>true</code> if the temperature data is updated
     * @throws IOException
     */
    private boolean isTemperatureDataAvailable() throws IOException {
        return (mDevice.readRegByte(LPS25H_REG_STATUS_REG) & 0x01) != 0;
    }

}
