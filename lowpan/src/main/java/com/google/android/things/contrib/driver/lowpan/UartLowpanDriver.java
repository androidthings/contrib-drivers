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

package com.google.android.things.contrib.driver.lowpan;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.pio.UartDevice;
import com.google.android.things.userdriver.LowpanDriver;
import com.google.android.things.userdriver.LowpanDriverCallback;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to support UART-based peripherals using Spinel with recommended UART
 * framing
 */
@SuppressWarnings("WeakerAccess")
public class UartLowpanDriver extends LowpanDriver implements AutoCloseable {

    private static final String TAG = UartLowpanDriver.class.getSimpleName();
    private final UartLowpanModule mUartLowpanModule;

    /**
     * Create a new UartLowpanDriver to send/receive commands and events to the
     * Android lowpan framework.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     */
    public UartLowpanDriver(String uartName, int baudRate)
            throws IOException {
        this(uartName, baudRate, UartDevice.HW_FLOW_CONTROL_NONE, null);
    }

    /**
     * Create a new UartLowpanDriver to send/receive commands and events to the
     * Android lowpan framework.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param hardwareFlowControl hardware flow control setting for uart device
     *        {@link UartDevice#HW_FLOW_CONTROL_NONE},
     *        {@link UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     *        @see UartDevice#HW_FLOW_CONTROL_NONE}
     *        @see UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     */
    public UartLowpanDriver(String uartName, int baudRate, int hardwareFlowControl)
            throws IOException {
        this(uartName, baudRate, hardwareFlowControl, null);
    }

    /**
     * Create a new UartLowpanDriver to communicate  with the Android lowpan framework.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param hardwareFlowControl hardware flow control setting for uart device
     *        {@link UartDevice#HW_FLOW_CONTROL_NONE},
     *        {@link UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     *        @see UartDevice#HW_FLOW_CONTROL_NONE}
     *        @see UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     * @param handler  optional {@link Handler} for software polling and callback events.
     */
    public UartLowpanDriver(String uartName, int baudRate,
                            int hardwareFlowControl, @Nullable Handler handler)
            throws IOException {
        mUartLowpanModule = new UartLowpanModule(uartName, baudRate, hardwareFlowControl, handler);
    }

    /**
     * Register this driver with the Android lowpan framework.
     */
    @SuppressWarnings("unused")
    public void register() {
        UserDriverManager manager = UserDriverManager.getManager();
        manager.registerLowpanDriver(this);
    }

    /**
     * Unregister this driver with the Android lowpan framework.
     */
    public void unregister() {
        UserDriverManager manager = UserDriverManager.getManager();
        manager.unregisterLowpanDriver(this);
    }

    @Override
    public void open(LowpanDriverCallback lowpanDriverCallback) {
        mUartLowpanModule.setLowpanDriverCallback(lowpanDriverCallback);
        lowpanDriverCallback.onOpened();
    }

    @Override
    public void close() {
        try {
            mUartLowpanModule.close();
        } catch (IOException ioe) {
            Log.e(TAG, "Error closing uart lowpan module", ioe);
            mUartLowpanModule.onError(UartLowpanModule.HAL_ERROR_IOFAIL);
        }
        unregister();
    }

    @Override
    public void sendFrame(byte[] bytes) {
        mUartLowpanModule.sendFrame(bytes);
    }

    @Override
    public void reset() {
        mUartLowpanModule.reset();
    }
}
