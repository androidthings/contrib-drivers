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
import com.google.android.things.userdriver.lowpan.LowpanDriver;
import com.google.android.things.userdriver.lowpan.LowpanDriverCallback;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;

/**
 * User-space driver to support UART-based peripherals using Spinel with recommended UART
 * framing
 */
@SuppressWarnings("WeakerAccess")
public class UartLowpanDriver extends LowpanDriver implements AutoCloseable {
    /** String tag used for logging */
    private static final String TAG = UartLowpanDriver.class.getSimpleName();

    /** How many times to attempt to restart if the UART dissapears unexpectedly. */
    private static final int RESTART_ATTEMPT_COUNT = 3;

    /** How many milliseconds to wait before the first restart attempt. */
    private static final int RESTART_ATTEMPT_FIRST_MS = 1200;

    /** How many milliseconds to wait inbetween restart attempts. */
    private static final int RESTART_ATTEMPT_MS = 500;

    private final String mUartName;
    private final int mBaudRate;
    private final int mHardwareFlowControl;
    private final Handler mHandler;

    private UartLowpanModule mUartLowpanModule = null;
    private LowpanDriverCallback mLowpanDriverCallback = null;

    private int mRestartAttemptsRemaining = 0;

    /** Trampoline instance to allow UartLowpanModule to signal when it runs into problems. */
    private final UartLowpanModule.UnexpectedCloseListener mUnexpectedCloseListener = new UartLowpanModule.UnexpectedCloseListener() {
        @Override
        public void onUnexpectedClose(UartLowpanModule uartLowpanModule) {
            UartLowpanDriver.this.onUnexpectedClose(uartLowpanModule);
        }
    };

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

    /** Called when there is trouble with the UartDevice. */
    private synchronized void onUnexpectedClose(final UartLowpanModule uartLowpanModule) {
        if (uartLowpanModule == mUartLowpanModule && uartLowpanModule.isClosed()) {
            if (RESTART_ATTEMPT_COUNT > 1) {
                Log.i(TAG, "UART closed unexpectedly (normal for USB UART), will try to re-establish before giving up.");

                mRestartAttemptsRemaining = RESTART_ATTEMPT_COUNT;
                mHandler.postDelayed(new Runnable() {
                                         @Override
                                         public void run() {
                                             attemptRestart(uartLowpanModule);
                                         }
                                     },
                        RESTART_ATTEMPT_FIRST_MS
                );
            } else {
                Log.e(TAG, "UART closed unexpectedly.");
                mLowpanDriverCallback.onError(ERROR_IOFAIL);
                stop();
            }
        }
    }

    private synchronized void attemptRestart(final UartLowpanModule uartLowpanModule) {
        if (uartLowpanModule == mUartLowpanModule && isRunning()
                && isPaused() && (mRestartAttemptsRemaining-- >= 0)) {
            try {
                mUartLowpanModule = new UartLowpanModule(mLowpanDriverCallback, mUartName,
                        mBaudRate, mHardwareFlowControl, mUnexpectedCloseListener, mHandler);

                Log.i(TAG, "Driver successfully restarted");

            } catch (IOException ioe) {
                if (mRestartAttemptsRemaining <= 0) {
                    Log.e(TAG, "Last restart attempt failed: " + ioe);
                    ioe.printStackTrace();
                    mLowpanDriverCallback.onError(ERROR_IOFAIL);
                    stop();
                } else {
                    Log.w(TAG, "Restart attempt "
                            + (RESTART_ATTEMPT_COUNT - mRestartAttemptsRemaining) + " failed. ");
                    mHandler.postDelayed(new Runnable() {
                                             @Override
                                             public void run() {
                                                 attemptRestart(uartLowpanModule);
                                             }
                                         },
                            RESTART_ATTEMPT_MS
                    );
                }
            }
        }
    }

    /**
     * Register this driver with the Android lowpan framework.
     */
    @SuppressWarnings("unused")
    public void register() {
        final UserDriverManager manager = UserDriverManager.getInstance();
        manager.registerLowpanDriver(this);
    }

    /**
     * Unregister this driver with the Android lowpan framework.
     */
    public void unregister() {
        final UserDriverManager manager = UserDriverManager.getInstance();
        manager.unregisterLowpanDriver(this);
    }

    private boolean isRunning() {
        return mLowpanDriverCallback != null;
    }

    private boolean isPaused() {
        return mUartLowpanModule == null || mUartLowpanModule.isClosed();
    }

    @Override
    public synchronized void start(final LowpanDriverCallback lowpanDriverCallback) {
        if (isRunning()) {
            // Fail to start if we are already running.
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
                    mBaudRate, mHardwareFlowControl, mUnexpectedCloseListener, mHandler);
        } catch (IOException ioe) {
            Log.w(TAG, "Exception on start(): " + ioe);
            ioe.printStackTrace();
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

    private synchronized void closeUart() throws IOException {
        try {
            if (mUartLowpanModule != null) {
                mUartLowpanModule.close();
            }
        } finally {
            mUartLowpanModule = null;
            mLowpanDriverCallback = null;
        }
    }

    @Override
    public void stop() {
        try {
            closeUart();
        } catch (IOException ioe) {
            // Caller won't receive any exceptions we might throw,
            // so we just log them here.
            Log.e(TAG, "Error closing on stop", ioe);
        }
    }

    @Override
    public void close() throws IOException {
        unregister();
        closeUart();
    }

    @Override
    public synchronized void sendFrame(byte[] bytes) {
        if (!isRunning()) {
            Log.e(TAG, "Call to sendFrame() while stopped");
            return;
        }

        if (isPaused()) {
            Log.w(TAG, "Call to sendFrame() while paused, will drop frame.");
            return;
        }

        try {
            mUartLowpanModule.sendFrame(bytes);
        } catch (IOException ioe) {
            Log.w(TAG, "Exception on sendFrame(): " + ioe);
            ioe.printStackTrace();

            onUnexpectedClose(mUartLowpanModule);
        }
    }

    @Override
    public synchronized void reset() {
        if (!isRunning()) {
            Log.e(TAG, "Call to reset() while stopped");
            return;
        }

        if (isPaused()) {
            Log.w(TAG, "Call to reset() while paused");
            return;
        }

        // Here we are going to perform an Arduino(tm)-Autoreset-style
        // hardware reset, where we assume that pulsing DTR will perform
        // a hardware reset of the NCP. To do this, we simply close
        // mUartLowpanModule and then immediately re-open it. If your
        // product has a dedicated NCP reset pin on your application
        // processor, you would toggle that here instead.

        try {
            mUartLowpanModule.close();

            mUartLowpanModule = new UartLowpanModule(mLowpanDriverCallback, mUartName,
                    mBaudRate, mHardwareFlowControl, mUnexpectedCloseListener, mHandler);
        } catch (IOException ioe) {
            Log.w(TAG, "Exception on reset(): " + ioe);
            ioe.printStackTrace();

            onUnexpectedClose(mUartLowpanModule);
        }
    }
}
