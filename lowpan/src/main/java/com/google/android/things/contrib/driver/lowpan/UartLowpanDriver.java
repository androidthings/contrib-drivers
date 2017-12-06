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

    private final String mUartName;
    private final int mBaudRate;
    private final int mHardwareFlowControl;
    private final Handler mHandler;

    private UartLowpanModule mUartLowpanModule = null;
    private LowpanDriverCallback mLowpanDriverCallback = null;

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
        mUartName = uartName;
        mBaudRate = baudRate;
        mHardwareFlowControl = hardwareFlowControl;
        mHandler = (handler != null) ? handler : new Handler();
    }

    /**
     * Register this driver with the Android lowpan framework.
     */
    @SuppressWarnings("unused")
    public void register() {
        final UserDriverManager manager = UserDriverManager.getManager();
        manager.registerLowpanDriver(this);
    }

    /**
     * Unregister this driver with the Android lowpan framework.
     */
    public void unregister() {
        final UserDriverManager manager = UserDriverManager.getManager();
        manager.unregisterLowpanDriver(this);
    }

    private boolean isRunning() {
        return mUartLowpanModule != null && !mUartLowpanModule.isClosed();
    }

    @Override
    public synchronized void start(final LowpanDriverCallback lowpanDriverCallback) {
        if (isRunning()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    lowpanDriverCallback.onError(ERROR_IOFAIL);
                }
            });
            return;
        }

        UartLowpanModule uartLowpanModule;

        try {
            uartLowpanModule = new UartLowpanModule(lowpanDriverCallback, mUartName,
                    mBaudRate, mHardwareFlowControl, mHandler);
        } catch (IOException x) {
            x.printStackTrace();
            lowpanDriverCallback.onError(ERROR_IOFAIL);
            return;
        }

        mUartLowpanModule = uartLowpanModule;
        mLowpanDriverCallback = lowpanDriverCallback;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                lowpanDriverCallback.onStarted();
            }
        });
    }

    @Override
    public synchronized void stop() {
        if (isRunning()) {
            mUartLowpanModule.close();
        }
        mUartLowpanModule = null;
        mLowpanDriverCallback = null;
    }

    @Override
    public void close() {
        unregister();
        stop();
    }

    @Override
    public synchronized void sendFrame(byte[] bytes) {
        if (!isRunning()) {
            Log.e(TAG, "Call to sendFrame() while stopped");
            return;
        }

        try {
            mUartLowpanModule.sendFrame(bytes);
        } catch (IOException ioe) {
            Log.d(TAG, "Exception on write: " + ioe);
            ioe.printStackTrace();
            mLowpanDriverCallback.onError(ERROR_IOFAIL);
            stop();
        }
    }

    @Override
    public synchronized void reset() {
        if (!isRunning()) {
            Log.e(TAG, "Call to reset() while stopped");
            return;
        }

        // Here we are going to perform an Arduino(tm)-Autoreset-style
        // hardware reset, where we assume that pulsing DTR will perform
        // a hardware reset of the NCP. To do this, we simply close
        // mUartLowpanModule and then immediately re-open it.

        mUartLowpanModule.close();
        mUartLowpanModule = null;

        try {
            mUartLowpanModule = new UartLowpanModule(mLowpanDriverCallback, mUartName,
                    mBaudRate, mHardwareFlowControl, mHandler);
        } catch (IOException ioe) {
            Log.d(TAG, "Exception on reset: " + ioe);
            ioe.printStackTrace();
            mLowpanDriverCallback.onError(ERROR_IOFAIL);
            stop();
        }
    }
}
