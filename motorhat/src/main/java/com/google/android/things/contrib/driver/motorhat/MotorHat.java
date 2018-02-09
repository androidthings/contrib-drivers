/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.things.contrib.driver.motorhat;

import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MotorHat implements AutoCloseable {

    public static final int DEFAULT_I2C_ADDRESS = 0x60;

    private static final int MAX_DC_MOTORS = 4;

    /**
     * Stop spinning.
     */
    public static final int MOTOR_STATE_RELEASE = 0;
    /**
     * Spin the motor clockwise.
     */
    public static final int MOTOR_STATE_CW = 1;
    /**
     * Spin the motor counter-clockwise.
     */
    public static final int MOTOR_STATE_CCW = 2;

    @IntDef({MOTOR_STATE_RELEASE, MOTOR_STATE_CW, MOTOR_STATE_CCW})
    public @interface MotorState{}

    private static final int REG_MODE_1 = 0x00;
    private static final int REG_MODE_2 = 0x01;
    private static final int REG_PRESCALE = 0xFE;
    private static final int REG_LED_0_ON_L = 0x06;
    private static final int REG_LED_0_ON_H = 0x07;
    private static final int REG_LED_0_OFF_L = 0x08;
    private static final int REG_LED_0_OFF_H = 0x09;
    private static final int REG_ALL_LED_ON_L = 0xFA;
    private static final int REG_ALL_LED_ON_H = 0xFB;
    private static final int REG_ALL_LED_OFF_L = 0xFC;
    private static final int REG_ALL_LED_OFF_H = 0xFD;

    private static final byte ALLCALL = 0x01;
    private static final byte OUTDRV = 0x04;
    private static final byte SLEEP = 0x10;
    private static final byte RESTART = (byte) 0x80;

    private static final int DC_PIN_LOW = 0;
    private static final int DC_PIN_HIGH = 4096;

    private static final int MIN_SPEED = 0;
    private static final int MAX_SPEED = 255;

    private I2cDevice mI2cDevice;
    private DcMotor[] mMotors;

    public MotorHat(String i2cBusName) throws IOException {
        this(i2cBusName, DEFAULT_I2C_ADDRESS);
    }

    public MotorHat(String i2cBusName, int i2cAddress) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        I2cDevice device = pioService.openI2cDevice(i2cBusName, i2cAddress);
        try {
            initialize(device);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException | RuntimeException ignored) {
            }
            throw e;
        }
    }

    @VisibleForTesting
    /*package*/ MotorHat(I2cDevice device) throws IOException {
        initialize(device);
    }

    private void initialize(I2cDevice device) throws IOException {
        mI2cDevice = device;

        // reset
        mI2cDevice.writeRegByte(REG_ALL_LED_ON_L, (byte) 0);
        mI2cDevice.writeRegByte(REG_ALL_LED_ON_H, (byte) 0);
        mI2cDevice.writeRegByte(REG_ALL_LED_OFF_L, (byte) 0);
        mI2cDevice.writeRegByte(REG_ALL_LED_OFF_H, (byte) 0);

        mI2cDevice.writeRegByte(REG_MODE_2, OUTDRV);
        mI2cDevice.writeRegByte(REG_MODE_1, ALLCALL);

        byte mode1 = mI2cDevice.readRegByte(REG_MODE_1);
        // Remove the restart and sleep bits.
        mode1 = (byte) (mode1 & ~(RESTART | SLEEP));
        // Sleep while we set the prescale value.
        mI2cDevice.writeRegByte(REG_MODE_1, (byte) (mode1 | SLEEP));
        float prescaleval = 25000000f // 25MHz
                / 4096 // 12-bit
                / 1600 // motor frequency
                - 1; // pineapple
        byte prescale = (byte) (prescaleval + 0.5f);
        mI2cDevice.writeRegByte(REG_PRESCALE, prescale);

        // Restart: clear the sleep bit, wait for the oscillator to stabilize, set the restart bit.
        // https://cdn-shop.adafruit.com/datasheets/PCA9685.pdf (page 15)
        mI2cDevice.writeRegByte(REG_MODE_1, (byte) (mode1 & ~SLEEP));
        try {
            TimeUnit.MICROSECONDS.sleep(500);
        } catch (InterruptedException ignored) {
        }
        mI2cDevice.writeRegByte(REG_MODE_1, (byte) (mode1 | RESTART));

        mMotors = new DcMotor[MAX_DC_MOTORS];
        mMotors[0] = new DcMotor(8, 9, 10);
        mMotors[1] = new DcMotor(13, 12, 11);
        mMotors[2] = new DcMotor(2, 3, 4);
        mMotors[3] = new DcMotor(7, 6, 5);
    }

    @Override
    public void close() throws IOException {
        if (mI2cDevice != null) {
            try {
                mI2cDevice.close();
            } finally {
                mI2cDevice = null;
            }
        }
    }

    private void setPin(int pin, boolean value) throws IOException {
        setPwm(pin, value ? DC_PIN_HIGH : DC_PIN_LOW, value ? DC_PIN_LOW : DC_PIN_HIGH);
    }

    private void setPwm(int channel, int on, int off) throws IOException {
        int offset = 4 * channel;
        mI2cDevice.writeRegByte(REG_LED_0_ON_L + offset, (byte) (on & 0xFF));
        mI2cDevice.writeRegByte(REG_LED_0_ON_H + offset, (byte) (on >> 8));
        mI2cDevice.writeRegByte(REG_LED_0_OFF_L + offset, (byte) (off & 0xFF));
        mI2cDevice.writeRegByte(REG_LED_0_OFF_H + offset, (byte) (off >> 8));
    }

    public void setMotorSpeed(int motor, int speed) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        mMotors[motor].setSpeed(speed);
    }

    public void setMotorState(int motor, @MotorState int state) throws IOException {
        if (mI2cDevice == null) {
            throw new IllegalStateException("I2C device not open");
        }
        mMotors[motor].setState(state);
    }

    // TODO make this extensible?
    class DcMotor {

        private final int mPwmPin, mIn1Pin, mIn2Pin;
        private int mSpeed;

        public DcMotor(int pwmPin, int in1Pin, int in2Pin) {
            mPwmPin = pwmPin;
            mIn1Pin = in1Pin;
            mIn2Pin = in2Pin;
        }

        private void setState(@MotorState int state) throws IOException {
            switch (state) {
                case MOTOR_STATE_RELEASE:
                    setPin(mIn1Pin, false);
                    setPin(mIn2Pin, false);
                    break;
                case MOTOR_STATE_CW:
                    setPin(mIn2Pin, false);
                    setPin(mIn1Pin, true);
                    break;
                case MOTOR_STATE_CCW:
                    setPin(mIn1Pin, false);
                    setPin(mIn2Pin, true);
                    break;
            }
        }

        private void setSpeed(int speed) throws IOException {
            if (speed < MIN_SPEED) {
                speed = MIN_SPEED;
            } else if (speed > MAX_SPEED) {
                speed = MAX_SPEED;
            }
            if (mSpeed != speed) {
                mSpeed = speed;
                setPwm(mPwmPin, 0, speed << 4);
            }
        }
    }
}
