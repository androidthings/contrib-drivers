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

package com.google.android.things.contrib.driver.zxgesturesensor;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;

import java.io.IOException;

public class ZXGestureSensorI2c extends ZXGestureSensor {
    private I2cDevice mDevice;
    private Gpio mGpio;
    private Handler mHandler;
    public static final int DEFAULT_I2C_ADDRESS = 0x10;

    /**
     * Interval in ms for the position read function to be called.
     * As sensor operates at 50Hz, this value should not go below 20.
     */
    private static final int SAMPLE_INTERVAL = 40; //Sensor operates at 50Hz



    /**
     * Creates new sensor driver for given I2C port.
     * @param port I2C port identifier
     * @throws IOException
     */
    public ZXGestureSensorI2c(String port, Handler handler) throws IOException {
        this(port, DEFAULT_I2C_ADDRESS, null, handler);
    }

    /**
     * Creates new sensor driver for given I2C port.
     * @param port I2C port identifier
     * @param i2cAddress I2C address that sensor listens on. either 0x10 or 0x11
     * @throws IOException
     */
    public ZXGestureSensorI2c(String port, int i2cAddress, Handler handler) throws IOException {
        this(port, i2cAddress, null, handler);
    }

    /**
     * Creates new sensor driver for given I2C port.
     * @param port I2C port identifier
     * @param i2cAddress I2C address that sensor listens on. either 0x10 or 0x11
     * @param gpioPin gpio pin with interrupt enabled that will listen to DR line
     * @throws IOException
     *
     * currently interrupt with <code>gpioPin</code> does not work;
     * register value for REG_DRE is not set
     */
    private ZXGestureSensorI2c(String port, int i2cAddress, String gpioPin, Handler handler) throws IOException {
        super();
        mHandler = handler == null ? new Handler() : handler;
        try {
            connectI2c(port, i2cAddress, gpioPin);
        } catch (IOException|RuntimeException e) {
            try {
                close();
            } catch (IOException ignored) {
            }

            throw e;
        }
    }

    private static final byte GESTURE_DR_MASK = 0b00111100;

    private GpioCallback onGpioDataReady = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "DR triggered");
            readData();
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    /**
     * Creates new sensor driver for given I2C port.
     * @param port I2C port identifier
     * @param i2cAddress I2C address that sensor listens on. either 0x10 or 0x11
     * @param gpioPin gpio pin with interrupt enabled that will listen to DR line
     * @throws IOException
     *
     * currently interrupt with <code>gpioPin</code> does not work;
     * register value for REG_DRE is not set
     */
    private void connectI2c(String port, int i2cAddress, String gpioPin) throws IOException {
        mDevice = pioService.openI2cDevice(port, i2cAddress);
        if (gpioPin != null) {
            mDevice.writeRegByte(REG_DRE, GESTURE_DR_MASK);
            mGpio = pioService.openGpio(gpioPin);
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setActiveType(Gpio.ACTIVE_HIGH);
            mGpio.setEdgeTriggerType(Gpio.EDGE_RISING);
            mGpio.registerGpioCallback(onGpioDataReady);
        } else {
            mHandler.post(mI2cTimer);
        }
    }

    /**
     * mask to convert signed int to unsigned byte value
     */
    private static final int UNSIGNED_MASK = 0xff;

    /**
     * I2C register addresses.
     * @see <a href="https://cdn.sparkfun.com/datasheets/Sensors/Proximity/XYZ%20I2C%20Registers%20v1.zip>XYZ I2C Register Map</a>
     */
    private static final int REG_STATUS = 0x00;
    private static final int REG_DRE = 0x01;
    private static final int REG_GESTURE = 0x04;
    private static final int REG_GESTURE_PARAM = 0x05;
    private static final int REG_XPOS = 0x08;
    private static final int REG_ZPOS = 0x0a;

    // status register masks
    private static final byte STATUS_POS_DATA_AVAIL = 0x1;
    private static final byte STATUS_GESTURE_DETECTED = 0b00111100;

    /**
     * X position value will come as a unsigned byte in range of [0, 240] with offset of 120,
     * and should be converted to [-120,120] range for real x position value.
     */
    private static final int X_POSITION_OFFSET = 120;

    private void readData() {
        try {
            byte status = mDevice.readRegByte(REG_STATUS);
            if ((status & STATUS_POS_DATA_AVAIL) != 0) { //check DAV (Position data available) bit
                mGestureDetector.setXpos(
                        (mDevice.readRegByte(REG_XPOS) & UNSIGNED_MASK) - X_POSITION_OFFSET);
                mGestureDetector.setZpos(mDevice.readRegByte(REG_ZPOS) & UNSIGNED_MASK);
            }
            if ((status & STATUS_GESTURE_DETECTED) != 0) { //if any of the gesture is detected
                int gestureId = mDevice.readRegByte(REG_GESTURE) & UNSIGNED_MASK;
                Gesture gesture = Gesture.getGesture(gestureId);
                if (gesture != null) {
                    mGestureDetector.setGesture(
                            gesture, mDevice.readRegByte(REG_GESTURE_PARAM) & UNSIGNED_MASK);
                } else {
                    Log.e(TAG,
                            "Undefined gesture (id: " + gestureId +
                                    ") is read from the sensor");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occured while reading from sensor connected to I2C", e);
        }
    }

    /**
     * Timer to perform polling on I2C port
     */
    private final Runnable mI2cTimer = new Runnable() {
        @Override
        public void run() {
            readData();
            mHandler.postDelayed(mI2cTimer, SAMPLE_INTERVAL);
        }
    };

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        IOException exception = null;
        if (mHandler != null) {
            mHandler.removeCallbacks(mI2cTimer);
        }
        if (mDevice != null) {
            try {
                mDevice.close();
            } catch (IOException e) {
                exception = e;
            } finally {
                mDevice = null;
            }
        }
        if (mGpio != null) {
            try {
                mGpio.unregisterGpioCallback(onGpioDataReady);
                mGpio.close();
            } catch (IOException e) {
                exception = e;
            }  finally {
                mGpio = null;
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
}
