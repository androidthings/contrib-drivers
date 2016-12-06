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

package com.google.android.things.contrib.driver.cap12xx;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Driver for the Microchip CAP12xx Capacitive Touch Controller
 * e.g. http://www.microchip.com/wwwproducts/en/CAP1208
 */
@SuppressWarnings("WeakerAccess")
public class Cap12xx implements AutoCloseable {
    private static final String TAG = "Cap12xx";

    /**
     * Interface definition for a callback to be invoked when
     * capacitive touch events occur within the controller.
     */
    public interface OnCapTouchListener {
        /**
         * Called when a touch event occurs on any of the
         * controller's input channels.
         *
         * @param controller the touch controller triggering the event
         * @param inputStatus array of input states. An input will report
         *                    true when touched, false otherwise
         */
        void onCapTouchEvent(Cap12xx controller, boolean[] inputStatus);
    }

    /**
     * Definition of specific touch controller chip properties
     * that are not common across the entire device family.
     */
    public enum Configuration {
        // Channel Count, Max Touch Inputs
        CAP1203(3,3),
        CAP1293(3,3),
        CAP1206(6,4),
        CAP1296(6,4),
        CAP1208(8,4),
        CAP1298(8,4);

        final int channelCount;
        final int maxTouch;
        Configuration(int channelCount, int maxTouch) {
            this.channelCount = channelCount;
            this.maxTouch = maxTouch;
        }
    }

    /**
     * Default I2C slave address for the CAP1xxx family.
     */
    public static final int I2C_ADDRESS = 0x28;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REPEAT_DISABLE, REPEAT_FAST, REPEAT_NORMAL, REPEAT_SLOW})
    public @interface RepeatRate {}
    /**
     * Constant to disable repeated touch events while a channel is active.
     * The channel will only generate the initial active event.
     */
    public static final int REPEAT_DISABLE = -1;
    /**
     * Constant to enable repeated touch events every 35ms while a channel is active.
     */
    public static final int REPEAT_FAST = 0b00000000;   // 35ms
    /**
     * Constant to enable repeated touch events every 175ms while a channel is active.
     * This is the default behavior.
     */
    public static final int REPEAT_NORMAL = 0b00000100; // 175ms
    /**
     * Constant to enable repeated touch events every 315ms while a channel is active.
     */
    public static final int REPEAT_SLOW = 0b00001000;   // 315ms

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SENSITIVITY_HIGH, SENSITIVITY_NORMAL, SENSITIVITY_LOW})
    public @interface Sensitivity {}
    /**
     * Constant to enable touch activation with very light input pressure.
     */
    public static final int SENSITIVITY_HIGH = 0b00000000;   // 128x
    /**
     * Constant to enable touch activation with standard input pressure.
     * This is the default behavior.
     */
    public static final int SENSITIVITY_NORMAL = 0b00110000; // 16x
    /**
     * Constant to enable touch activation with firm input pressure.
     */
    public static final int SENSITIVITY_LOW = 0b01100000;    // 2x

    // Software polling delay
    private static final int SOFTWAREPOLL_DELAY_MS = 16;

    // CAP12xx register map
    private static final int REG_MAIN_CONTROL  = 0x00;
    private static final int REG_INPUT_STATUS  = 0x03;
    private static final int REG_INPUT_DELTA   = 0x10;
    private static final int REG_SENSE_CFG     = 0x1F;
    private static final int REG_INPUT_EN      = 0x21;
    private static final int REG_INPUT_CFG     = 0x22;
    private static final int REG_INPUT_CFG2    = 0x23;
    private static final int REG_INTERRUPT_EN  = 0x27;
    private static final int REG_REPEAT_EN     = 0x28;
    private static final int REG_MTOUCH_CFG    = 0x2A;
    private static final int REG_INPUT_THRESH  = 0x30;

    private I2cDevice mDevice;
    private Gpio mAlertPin;
    private Configuration mChipConfiguration;

    private Handler mInputHandler;
    private OnCapTouchListener mCapTouchListener;
    private boolean[] mInputStatus;

    /**
     * Create a new Cap12xx controller.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @throws IOException
     */
    public Cap12xx(Context context,
                   String i2cName,
                   String alertName,
                   Configuration chip) throws IOException {
        this(context, i2cName, alertName, chip, null);
    }

    /**
     * Create a new Cap12xx controller.
     *
     * @param context Current context, used for loading resources.
     * @param i2cName I2C port name where the controller is attached. Cannot be null.
     * @param alertName optional GPIO pin name connected to the controller's
     *                  alert interrupt signal. Can be null.
     * @param chip identifier for the connected controller device chip.
     * @param handler optional {@link Handler} for software polling and callback events.
     * @throws IOException
     */
    public Cap12xx(Context context,
                   String i2cName,
                   String alertName,
                   Configuration chip,
                   Handler handler) throws IOException {
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            I2cDevice device = manager.openI2cDevice(i2cName, I2C_ADDRESS);
            Gpio alertPin = null;
            if (alertName != null) {
                alertPin = manager.openGpio(alertName);
            }
            init(device, alertPin, chip, handler);
        } catch (IOException|RuntimeException e) {
            // Close the peripherals if an init error occurs
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ Cap12xx(I2cDevice i2cDevice,
                        Gpio alertPin,
                        Configuration chip) throws IOException {
        init(i2cDevice, alertPin, chip, null);
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(I2cDevice i2cDevice, Gpio alertPin, Configuration chip, Handler handler)
            throws IOException {
        if (i2cDevice == null) {
            throw new IllegalArgumentException("Must provide I2C device");
        }
        if (chip == null) {
            throw new IllegalArgumentException("Must provide a valid chip configuration");
        }

        mDevice = i2cDevice;
        mAlertPin = alertPin;
        mChipConfiguration = chip;

        // Create handler for polling and callbacks
        mInputHandler = new Handler(handler == null
                ? Looper.myLooper() : handler.getLooper());
        mInputStatus = new boolean[mChipConfiguration.channelCount];

        if (mAlertPin != null) {
            // Configure hardware interrupt trigger
            mAlertPin.setDirection(Gpio.DIRECTION_IN);
            mAlertPin.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mAlertPin.registerGpioCallback(mAlertPinCallback, mInputHandler);
        } else {
            // Begin software interrupt polling
            mInputHandler.post(mPollingCallback);
        }

        // Initialize device defaults
        setInputsEnabled(true); // Enable all inputs
        setInterruptsEnabled(true); // Enable all interrupts
        setMultitouchInputMax(mChipConfiguration.maxTouch); // Enable multitouch
        setRepeatRate(REPEAT_NORMAL);
        setSensitivity(SENSITIVITY_NORMAL);
    }

    /**
     * Register a callback to be invoked when the controller detects
     * a touch event on any of its active input channels.
     *
     * @param listener The callback to invoke, or null to remove the current callback.
     */
    public void setOnCapTouchListener(OnCapTouchListener listener) {
        mCapTouchListener = listener;
    }

    /**
     * Set the repeat rate of generated events on active input channels.
     * Can be one of {@link #REPEAT_NORMAL}, {@link #REPEAT_FAST}, or
     * {@link #REPEAT_SLOW} to generate continuous events while any of
     * the input channels are actively detecting touch input.
     *
     * <p>Use {@link #REPEAT_DISABLE} to generate a single event for each
     * input touch and release. The default is {@link #REPEAT_NORMAL}.
     *
     * @param rate one of {@link #REPEAT_NORMAL}, {@link #REPEAT_SLOW},
     *             {@link #REPEAT_FAST}, or {@link #REPEAT_DISABLE}.
     *
     * @throws IOException
     */
    public void setRepeatRate(@RepeatRate int rate) throws IOException {
        if (rate == REPEAT_DISABLE) {
            setRepeatEnabled(false); // Disable all repeats
        } else {
            setRepeatEnabled(true); // Enable all repeats
            // Set repeat value
            byte value = mDevice.readRegByte(REG_INPUT_CFG);
            value = BitwiseUtil.applyBitRange(value, rate, 0x0F); // RPT_RATE bits
            mDevice.writeRegByte(REG_INPUT_CFG, value);
            // Set press and hold value
            value = mDevice.readRegByte(REG_INPUT_CFG2);
            value = BitwiseUtil.applyBitRange(value, rate, 0x0F); // M_PRESS bits
            mDevice.writeRegByte(REG_INPUT_CFG2, value);
        }
    }

    /**
     * Set the detection sensitivity of the capacitive input channels.
     *
     * <p>The default is {@link #SENSITIVITY_NORMAL}.
     *
     * @param sensitivity one of {@link #SENSITIVITY_NORMAL},
     *                    {@link #SENSITIVITY_LOW}, or {@link #SENSITIVITY_HIGH}.
     *
     * @throws IOException
     */
    public void setSensitivity(@Sensitivity int sensitivity) throws IOException {
        byte value = mDevice.readRegByte(REG_SENSE_CFG);
        value = BitwiseUtil.applyBitRange(value, sensitivity, 0x70); // DELTA_SENSE bits
        mDevice.writeRegByte(REG_SENSE_CFG, value);
    }

    /**
     * Set the maximum number of input channels allowed to simultaneously
     * generate active events. Additional touches beyond this number will
     * be ignored.
     *
     * @param count Maximum number of inputs allowed to generate events.
     *
     * @throws IOException
     */
    public void setMultitouchInputMax(int count) throws IOException {
        if (count < 1 || count > mChipConfiguration.maxTouch) {
            throw new IllegalArgumentException("Multitouch count must be between 1 and "
                    + mChipConfiguration.maxTouch);
        }

        // Enable multitouch blocking to cap touches at the maximum value
        byte value = mDevice.readRegByte(REG_MTOUCH_CFG);
        value = BitwiseUtil.setBit(value, 7);   // Enable MULT_BLK
        mDevice.writeRegByte(REG_MTOUCH_CFG, value);

        // Configure the maximum number of touch points
        value = mDevice.readRegByte(REG_MTOUCH_CFG);
        count = (count - 1) << 2;
        value = BitwiseUtil.applyBitRange(value, count, 0x0C); // B_MULT_T bits
        mDevice.writeRegByte(REG_MTOUCH_CFG, value);
    }

    /**
     * Return whether the interrupt bit on the controller is currently active.
     *
     * @throws IOException
     */
    public boolean readInterruptFlag() throws IOException {
        byte value = mDevice.readRegByte(REG_MAIN_CONTROL);
        return BitwiseUtil.isBitSet(value, 0); // INT bit
    }

    /**
     * Clear the active interrupt bit on the controller.
     *
     * @throws IOException
     */
    public void clearInterruptFlag() throws IOException {
        byte value = mDevice.readRegByte(REG_MAIN_CONTROL);
        value = BitwiseUtil.clearBit(value, 0); // Clear the INT bit
        mDevice.writeRegByte(REG_MAIN_CONTROL, value);
    }

    /**
     * Get the current touch status of the given input channel.
     *
     * @param channel Input channel to read.
     * @return true if channel is sensing active touch, false otherwise.
     *
     * @throws IOException
     */
    public boolean readInputChannel(int channel) throws IOException {
        if (channel < 0 || channel >= mChipConfiguration.channelCount) {
            throw new IllegalArgumentException("Input channel must be between 0 and "
                    + (mChipConfiguration.channelCount-1));
        }

        byte status = readInputStatus();
        return BitwiseUtil.isBitSet(status, channel);
    }

    /**
     * Return the number of input channels supported by this controller.
     */
    public int getInputChannelCount() {
        return mChipConfiguration.channelCount;
    }

    /**
     * Return the maximum number of simultaneous multitouch events supported
     * by this controller.
     */
    public int getMaximumTouchPoints() {
        return mChipConfiguration.maxTouch;
    }

    /**
     * Close this device and any underlying resources associated with the connection.
     */
    @Override
    public void close() throws IOException {
        // Cancel software polling
        mInputHandler.removeCallbacks(mPollingCallback);

        if (mDevice != null) {
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }

        if (mAlertPin != null) {
            mAlertPin.unregisterGpioCallback(mAlertPinCallback);
            try {
                mAlertPin.close();
            } finally {
                mAlertPin = null;
            }
        }
    }

    /**
     * Write bitmask of input channels that should be enabled.
     * @param enable true to enable all inputs, false to disable them.
     * @throws IOException
     */
    public void setInputsEnabled(boolean enable) throws IOException {
        if (enable) {
            mDevice.writeRegByte(REG_INPUT_EN, (byte) 0xFF); // All channels ON
        } else {
            mDevice.writeRegByte(REG_INPUT_EN, (byte) 0x00); // All channels OFF
        }
    }

    /**
     * Write bitmask of input channels that should generate interrupts.
     * @param enable true to enable all interrupts, false to disable them.
     * @throws IOException
     */
    public void setInterruptsEnabled(boolean enable) throws IOException {
        if (enable) {
            mDevice.writeRegByte(REG_INTERRUPT_EN, (byte) 0xFF); // All channels ON
        } else {
            mDevice.writeRegByte(REG_INTERRUPT_EN, (byte) 0x00); // All channels OFF
        }
    }

    /**
     * Write bitmask of input channels to generate repeat interrupts on hold.
     * @param enable true to enable repeat on all channels, false to disable it.
     * @throws IOException
     */
    public void setRepeatEnabled(boolean enable) throws IOException {
        if (enable) {
            mDevice.writeRegByte(REG_REPEAT_EN, (byte) 0xFF); // All channels ON
        } else {
            mDevice.writeRegByte(REG_REPEAT_EN, (byte) 0x00); // All channels OFF
        }
    }

    /**
     * Read the active touch status of all input channels as a bitmask.
     * Each bit is set to "1" if the input is active, and "0" if inactive.
     *
     * @return Bitmask containing the status of all channels.
     *
     * @throws IOException
     */
    private byte readInputStatus() throws IOException {
        byte statusFlags = mDevice.readRegByte(REG_INPUT_STATUS);
        byte[] deltas = new byte[mChipConfiguration.channelCount];
        byte[] thresholds = new byte[mChipConfiguration.channelCount];
        mDevice.readRegBuffer(REG_INPUT_DELTA, deltas, deltas.length);
        mDevice.readRegBuffer(REG_INPUT_THRESH, thresholds, thresholds.length);

        for (int i = 0; i < mChipConfiguration.channelCount; i++) {
            // Check if input was active during interrupt
            if (BitwiseUtil.isBitSet(statusFlags, i)) {
                // Check if sense is high enough to register a touch
                if (deltas[i] >= thresholds[i]) {
                    // Input pressed this cycle
                    statusFlags = BitwiseUtil.setBit(statusFlags, i);
                } else {
                    // Input released this cycle
                    statusFlags = BitwiseUtil.clearBit(statusFlags, i);
                }
            }
        }

        return statusFlags;
    }

    /**
     * Callback invoked when the optional alert pin of the
     * touch controller signals an interrupt.
     */
    private GpioCallback mAlertPinCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            handleInterrupt();
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, "Error handling GPIO interrupt: " + error);
        }
    };

    /**
     * Callback invoked to poll the state of the controller
     * interrupt in software.
     */
    private Runnable mPollingCallback = new Runnable() {
        @Override
        public void run() {
            handleInterrupt();
            mInputHandler.postDelayed(this, SOFTWAREPOLL_DELAY_MS);
        }
    };

    /**
     * Callback invoked from polling or an interrupt on the
     * optional alert pin to read the current input channel
     * status from the controller.
     */
    private void handleInterrupt() {
        try {
            if (readInterruptFlag()) {
                // Update status result
                final byte inputStatus = readInputStatus();
                for (int i = 0; i < mChipConfiguration.channelCount; i++) {
                    mInputStatus[i] = BitwiseUtil.isBitSet(inputStatus, i);
                }

                // Call attached listener
                if (mCapTouchListener != null) {
                    mCapTouchListener.onCapTouchEvent(this, mInputStatus);
                }

                // Clear interrupt
                clearInterruptFlag();
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to process interrupt event", e);
        }
    }
}
