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

package com.google.android.things.contrib.driver.vcnl4200;

import android.support.annotation.IntDef;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the VCNL4200, a module that serves as both a high-sensitivity
 * long distance proximity sensor and an ambient light sensor. The datasheet
 * is available at: https://www.vishay.com/docs/84430/vcnl4200.pdf.
 */
public class Vcnl4200 implements AutoCloseable {
    private static final String TAG = Vcnl4200.class.getSimpleName();

    /**
     * I2C Slave Address of the VCNL4200
     */
    private static final int I2C_ADDRESS = 0x51;

    /**
     * I2S Register Addresses
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REGISTER_ALS_CONF, REGISTER_ALS_HIGH_INT_THRESH, REGISTER_ALS_LOW_INT_THRESH,
            REGISTER_PS_CONF_1_2, REGISTER_PS_CONF_3_MS, REGISTER_PS_CANC_LEVEL,
            REGISTER_PROX_LOW_INT_THRESH, REGISTER_PROX_HIGH_INT_THRESH, REGISTER_PROX_DATA,
            REGISTER_ALS_DATA, REGISTER_WHITE_DATA, REGISTER_INTERRUPT_FLAGS, REGISTER_DEVICE_ID})
    /*package*/ @interface Register {}

    static final int REGISTER_ALS_CONF               = 0x00;
    static final int REGISTER_ALS_HIGH_INT_THRESH    = 0x01;
    static final int REGISTER_ALS_LOW_INT_THRESH     = 0x02;
    static final int REGISTER_PS_CONF_1_2            = 0x03;
    static final int REGISTER_PS_CONF_3_MS           = 0x04;
    static final int REGISTER_PS_CANC_LEVEL          = 0x05;
    static final int REGISTER_PROX_LOW_INT_THRESH    = 0x06;
    static final int REGISTER_PROX_HIGH_INT_THRESH   = 0x07;
    static final int REGISTER_PROX_DATA              = 0x08;
    static final int REGISTER_ALS_DATA               = 0x09;
    static final int REGISTER_WHITE_DATA             = 0x0A;
    static final int REGISTER_INTERRUPT_FLAGS        = 0x0D;
    static final int REGISTER_DEVICE_ID              = 0x0E;

    /**
     * ALS Integration Time Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_IT_TIME_50MS, ALS_IT_TIME_100MS, ALS_IT_TIME_200MS, ALS_IT_TIME_400MS})
    public @interface AlsIntegrationTime {}

    public static final int ALS_IT_TIME_50MS    = 0;
    public static final int ALS_IT_TIME_100MS   = 0b01 << 6;
    public static final int ALS_IT_TIME_200MS   = 0b10 << 6;
    public static final int ALS_IT_TIME_400MS   = 0b11 << 6;

    /**
     * ALS Interrupt Switch Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_SWITCH_ALS_CHANNEL, ALS_INT_SWITCH_WHITE_CHANNEL})
    public @interface AlsInterruptSwitch {}

    public static final int ALS_INT_SWITCH_ALS_CHANNEL    = 0;
    public static final int ALS_INT_SWITCH_WHITE_CHANNEL  = 0b1 << 5;

    /**
     * ALS Interrupt Persistence Setting Values
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_PERSISTENCE_1, ALS_INT_PERSISTENCE_2, ALS_INT_PERSISTENCE_4,
            ALS_INT_PERSISTENCE_8})
    public @interface AlsInterruptPersistence {}

    public static final int ALS_INT_PERSISTENCE_1 = 0;
    public static final int ALS_INT_PERSISTENCE_2 = 0b01 << 2;
    public static final int ALS_INT_PERSISTENCE_4 = 0b10 << 2;
    public static final int ALS_INT_PERSISTENCE_8 = 0b11 << 2;

    /**
     * ALS Interrupt Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_INT_DISABLE, ALS_INT_ENABLE})
    public @interface AlsInterrupt {}

    public static final int ALS_INT_DISABLE = 0;
    public static final int ALS_INT_ENABLE  = 0b1 << 1;

    /**
     * ALS Power Setting
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ALS_POWER_ON, ALS_POWER_OFF})
    public @interface AlsPower {}

    public static final int ALS_POWER_ON  = 0;
    public static final int ALS_POWER_OFF = 1;

    private I2cDevice mDevice;

    /**
     * Create a new VCNL4200 driver connected to the given I2C bus.
     * @param bus
     * @throws IOException
     */
    public Vcnl4200(String bus) throws IOException {
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
     * Create a new VCNL4200 driver connected to the given I2C device.
     * @param device
     * @throws IOException
     */
    /*package*/ Vcnl4200(I2cDevice device) throws IOException {
        connect(device);
    }

    private void connect(I2cDevice device) throws IOException {
        if (mDevice != null) {
            throw new IllegalStateException("device already connected");
        }
        mDevice = device;
    }

    /**
     * Returns Device ID - for MP version sample, the LSB is 0x58.
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public int getDeviceId() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalArgumentException("device is not connected");
        }
        return mDevice.readRegWord(REGISTER_DEVICE_ID) & 0xFFFF;
    }

    /**
     * Returns ambient light sensor value.
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public short getAlsData() throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalArgumentException("device is not connected");
        }
        return mDevice.readRegWord(REGISTER_ALS_DATA);
    }

    /**
     * Sets the ALS configuration, where @alsConfiguration is the OR of one of each of:
     *  AlsIntegrationTime, AlsInterruptSwitch, AlsInterruptPersistence, AlsInterrupt, AlsPower
     * Ex. (ALS_IT_TIME_200MS | ALS_INT_SWITCH_ALS_CHANNEL | ALS_INT_PERSISTENCE_4)
     * The values for 'AlsInterrupt' and 'AlsPower' default to enabling the interrupt and the ALS
     * sensor, so OR'ing with these fields is not necessary.
     * @param alsConfiguration
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsConfiguration(short alsConfiguration)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalArgumentException("device is not connected");
        }
        mDevice.writeRegWord(REGISTER_ALS_CONF, alsConfiguration);
    }

    /**
     * Sets the low and high thresholds at which the interrupt is fired for ALS.
     * @param lowThreshold
     * @param highThreshold
     * @throws IOException
     * @throws IllegalStateException
     */
    public void setAlsThresholds(short lowThreshold, short highThreshold)
            throws IOException, IllegalStateException {
        if (mDevice == null) {
            throw new IllegalArgumentException("device is not connected");
        }
        mDevice.writeRegWord(REGISTER_ALS_LOW_INT_THRESH, lowThreshold);
        mDevice.writeRegWord(REGISTER_ALS_HIGH_INT_THRESH, highThreshold);
    }

    /**
     * Shuts down the underlying I2C device.
     * @throws IOException
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
