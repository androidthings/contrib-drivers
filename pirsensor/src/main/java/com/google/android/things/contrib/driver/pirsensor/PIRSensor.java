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

package com.google.android.things.contrib.driver.pirsensor;

import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.ViewConfiguration;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver for GPIO based PIR Sensor (Passive IR).
 */
public class PIRSensor implements AutoCloseable {
    private static final String TAG = PIRSensor.class.getSimpleName();

    private Gpio mSensorGpio;
    private OnPIREventListener mListener;

    /**
     * Interface definition for a callback to be invoked when a PIR Sensor event occurs.
     */
    public interface OnPIREventListener {
        /**
         * Called when a Movement was detected or the sensor reset (LOW)
         *
         * @param sensor the PIR Sensor for which the event occurred
         * @param detected true if a movement was is detected
         */
        void onSensorEvent(PIRSensor sensor, boolean detected);
    }

    /**
     * Create a new PIR Sensor driver for the given GPIO pin name.
     * @param pin GPIO pin where the PIR Input comes in.
     * @throws IOException
     */
    public PIRSensor(String pin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio sensorGpio = pioService.openGpio(pin);
        try {
            connect(sensorGpio);
        } catch (IOException|RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ PIRSensor(Gpio sensorGpio) throws IOException {
       connect(sensorGpio);
    }

    /**
     * Configure the GPIO PIN as an INPUT to read the sensor data
     * @param sensorGpio
     * @throws IOException
     */
    private void connect(Gpio sensorGpio) throws IOException {
        mSensorGpio = sensorGpio;
        mSensorGpio.setDirection(Gpio.DIRECTION_IN);
        mSensorGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);

        // Configure so value change means ACTIVE_HIGH
        mSensorGpio.setActiveType(Gpio.ACTIVE_HIGH);
        mSensorGpio.registerGpioCallback(mInterruptCallback);

    }

    /**
     * Local callback to monitor GPIO edge events.
     */
    private GpioCallback mInterruptCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                boolean currentState = gpio.getValue();

                // Trigger event immediately
                performPIREvent(currentState);

            } catch (IOException e) {
                Log.e(TAG, "Error reading PIR state", e);
            }

            return true;
        }
    };

    /**
     * Set the listener to be called when a Sensor event occurred.
     *
     * @param listener Sensor event listener to be invoked.
     */
    public void setOnPIREventListener(OnPIREventListener listener) {
        mListener = listener;
    }

    /**
     * Close the driver and the underlying device.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        mListener = null;

        if (mSensorGpio != null) {
            mSensorGpio.unregisterGpioCallback(mInterruptCallback);
            try {
                mSensorGpio.close();
            } finally {
                mSensorGpio = null;
            }
        }
    }

    /**
     * Invoke Sensor event callback
     */
    private void performPIREvent(boolean state) {
        if (mListener != null) {
            mListener.onSensorEvent(this, state);
        }
    }
}
