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

package com.google.android.things.contrib.driver.gps;

import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Peripheral that generates NMEA location sentences transmitted
 * over a UART.
 */
@SuppressWarnings("WeakerAccess")
public class NmeaGpsModule implements AutoCloseable {
    private static final String TAG = "NmeaGpsModule";

    private UartDevice mDevice;
    private NmeaParser mParser;

    private float mGpsAccuracy;

    /**
     * Create a new NmeaGpsModule.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     */
    public NmeaGpsModule(String uartName, int baudRate) throws IOException {
        this(uartName, baudRate, null);
    }

    /**
     * Create a new NmeaGpsModule.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param handler optional {@link Handler} for software polling and callback events.
     */
    public NmeaGpsModule(String uartName, int baudRate, Handler handler) throws IOException {
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            UartDevice device = manager.openUartDevice(uartName);
            init(device, baudRate, handler);
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    /**
     * Constructor invoked from unit tests.
     */
    @VisibleForTesting
    /*package*/ NmeaGpsModule(UartDevice device, int baudRate,
                              Handler handler) throws IOException {
        init(device, baudRate, handler);
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(UartDevice device, int baudRate, Handler handler) throws IOException {
        mDevice = device;
        mDevice.setBaudrate(baudRate);
        mDevice.registerUartDeviceCallback(mCallback, handler);

        mParser = new NmeaParser();
    }

    /**
     * Provide the measured accuracy from the GPS module specification.
     *
     * @param accuracy specified accuracy, in meters CEP.
     */
    public void setGpsAccuracy(float accuracy) {
        mGpsAccuracy = accuracy;
    }

    /**
     * Return the specified accuracy, in meters CEP, of this module.
     */
    public float getGpsAccuracy() {
        return mGpsAccuracy;
    }

    /**
     * Register a callback to be invoked when the GPS module
     * generates a new location events.
     *
     * @param callback The callback to invoke, or null to remove the current callback.
     */
    public void setGpsModuleCallback(GpsModuleCallback callback) {
        mParser.setGpsModuleCallback(callback);
    }

    /**
     * Close this device and any underlying resources associated with the connection.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            mDevice.unregisterUartDeviceCallback(mCallback);
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    /**
     * Callback invoked when new data arrives in the UART buffer.
     */
    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            try {
                readUartBuffer();
            } catch (IOException e) {
                Log.w(TAG, "Unable to read UART data", e);
            }

            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, "Error receiving incoming data: " + error);
        }
    };

    /**
     * Drain the current contents of the UART buffer.
     */
    private static final int CHUNK_SIZE = 512;
    private void readUartBuffer() throws IOException {
        byte[] buffer = new byte[CHUNK_SIZE];
        int count;
        while ((count = mDevice.read(buffer, buffer.length)) > 0) {
            processBuffer(buffer, count);
        }
    }

    /**
     * Traverse each buffer received from the UART, looking for
     * a valid message frame.
     */
    private boolean mFrameFlag = false;
    private ByteBuffer mMessageBuffer = ByteBuffer.allocate(CHUNK_SIZE*2);
    private void processBuffer(byte[] buffer, int count) {
        for (int i = 0; i < count; i++) {
            if (mParser.getFrameStart() == buffer[i]) {
                handleFrameStart();
            } else if (mParser.getFrameEnd() == buffer[i]) {
                handleFrameEnd();
            } else if (buffer[i] != 0){
                //Insert all other characters except '0's into the buffer
                mMessageBuffer.put(buffer[i]);
            }
        }
    }

    /**
     * Restart the buffer when a new frame start character is detected.
     */
    private void handleFrameStart() {
        mMessageBuffer.clear();
        mFrameFlag = true;
    }

    /**
     * Parse a message once the frame end character is detected.
     */
    private void handleFrameEnd() {
        if (!mFrameFlag) {
            // We never saw the whole message, discard
            resetBuffer();
            return;
        }

        // Gather the bytes into a single array
        mMessageBuffer.flip();
        byte[] raw = new byte[mMessageBuffer.limit()];
        mMessageBuffer.get(raw);

        mParser.processMessageFrame(raw);

        // Reset the buffer state
        resetBuffer();
    }

    /**
     * Reset the buffer state.
     */
    private void resetBuffer() {
        mMessageBuffer.clear();
        mFrameFlag = false;
    }
}
