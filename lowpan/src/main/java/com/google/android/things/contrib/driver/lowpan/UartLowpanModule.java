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

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.android.things.userdriver.LowpanDriverCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Peripheral that transmits spinel frames transmitted
 * over a UART.
 */
@SuppressWarnings("WeakerAccess")
public class UartLowpanModule implements AutoCloseable {

    public static final int HAL_ERROR_FAILED = 1;
    public static final int HAL_ERROR_ALREADY = 2;
    public static final int HAL_ERROR_TOOBIG  = 3;
    public static final int HAL_ERROR_IOFAIL  = 4;
    public static final int HAL_ERROR_GARBAGE = 5;

    private static final String TAG = UartLowpanModule.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int MAX_SPINEL_SIZE = 1300;
    private final String mUartName;
    private final Handler mHandler;
    private final int mHardwareFlowControl;
    private final int mBaudRate;

    private enum FrameFlag { NOT_STARTED,  STARTED, UNESCAPING }

    private UartDevice mDevice;
    private byte[] mInboundFrame = new byte[MAX_SPINEL_SIZE];
    private LowpanDriverCallback mLowpanDriverCallback;
    private short mInboundFrameHDLCCRC;
    private int mInboundFrameSize;
    private FrameFlag mFrameFlag;

    /**
     * Create a new UartLowpanModule.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     * @param hardwareFlowControl hardware flow control setting for uart device
     *        {@link UartDevice#HW_FLOW_CONTROL_NONE},
     *        {@link UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     *        @see UartDevice#HW_FLOW_CONTROL_NONE}
     *        @see UartDevice#HW_FLOW_CONTROL_AUTO_RTSCTS}
     * @param handler optional {@link Handler} for UartDevice.
     */
    public UartLowpanModule(String uartName, int baudRate, int hardwareFlowControl,
                            @Nullable Handler handler) throws IOException {
        try {
            mUartName = uartName;
            mHardwareFlowControl = hardwareFlowControl;
            mHandler = handler;
            mBaudRate = baudRate;
            PeripheralManagerService manager = new PeripheralManagerService();
            UartDevice device = manager.openUartDevice(mUartName);
            init(device, mBaudRate, mHardwareFlowControl, mHandler);
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (Exception ioe) {
            } finally {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * Create a new UartLowpanModule.
     *
     * @param uartName UART port name where the module is attached. Cannot be null.
     * @param baudRate Baud rate used for the module UART.
     */
    @SuppressWarnings("unused")
    public UartLowpanModule(String uartName, int baudRate)
            throws IOException {
        this(uartName, baudRate, UartDevice.HW_FLOW_CONTROL_NONE,  null);
    }

    /**
     * Set the driver callback
     * @param lowpanDriverCallback @see {@link LowpanDriverCallback}
     */
    public void setLowpanDriverCallback(LowpanDriverCallback lowpanDriverCallback) {
        mLowpanDriverCallback = lowpanDriverCallback;
    }

    /**
     * Sends a frame to the ncp
     *
     * @param frame byte[] frame to write to ncp
     */
    public void sendFrame(byte[] frame) {
        byte[] outboundFrame = escapeFrame(frame, frame.length);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (byte b : outboundFrame) {
                sb.append(String.format("%02X ", b));
            }
            Log.d(TAG, "writing frame " + sb.toString());
        }
        int frameSize = outboundFrame.length;
        try {
            int written = mDevice.write(outboundFrame, frameSize);
            if (frameSize != written) {
                throw new IOException("Failed to write frames wrote " + written +
                        " bytes, but expected " + frameSize);
            }
        } catch (IOException ioe) {
            Log.d(TAG, ioe.getMessage());
            onError(HAL_ERROR_IOFAIL);
        }
    }

    /**
     * Close this device and any underlying resources associated with the connection.
     */
    @Override
    public void close() throws IOException {
        if (mDevice != null) {
            mDevice.unregisterUartDeviceCallback(mCallback);
            mLowpanDriverCallback = null;
            try {
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    public void onError(int error) {
        if (mLowpanDriverCallback != null) {
            mLowpanDriverCallback.onError(error);
        }
    }

    protected void reset() {
        if (mLowpanDriverCallback == null) {
            Log.e(TAG, "Device is closed");
            return;
        }
        try {
            close();
        } catch (IOException ioe) {
            Log.e(TAG, "Failure trying to close uart device on reset, attempting to reopen anyway");
        }
        try {
            PeripheralManagerService manager = new PeripheralManagerService();
            UartDevice device = manager.openUartDevice(mUartName);
            init(device, mBaudRate, mHardwareFlowControl, mHandler);
        } catch (IOException ioe) {
            Log.e(TAG, "Failure opening uart device on reset");
            onError(HAL_ERROR_IOFAIL);
        }
    }

    /**
     * Initialize peripheral defaults from the constructor.
     */
    private void init(UartDevice device, int baudRate, int hardwareFlowControl, Handler handler)
            throws IOException {
        mDevice = device;
        mDevice.setBaudrate(baudRate);
        mDevice.setDataSize(8);
        mDevice.setHardwareFlowControl(hardwareFlowControl);
        handleFrameStart();
        mDevice.registerUartDeviceCallback(mCallback, handler);
    }

    private byte[] escapeFrame(byte[] inboundFrame, int inboundFrameLen) {
        ByteBuffer outputBufferEscaped = ByteBuffer.allocate(MAX_SPINEL_SIZE * 2);
        outputBufferEscaped.clear();
        outputBufferEscaped.put(Hdlc.HDLC_BYTE_FLAG);
        short crc = Hdlc.HDLC_SHORT_CRC_RESET;
        for (int i = 0; i < inboundFrameLen; i++) {
            byte frameByte = inboundFrame[i];
            crc = Hdlc.hdlcCrc16(crc, frameByte);
            if (Hdlc.hdlcByteNeedsEscape(frameByte)) {
                outputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
                outputBufferEscaped.put((byte) (frameByte ^ Hdlc.HDLC_ESCAPE_XFORM));
            } else {
                outputBufferEscaped.put(frameByte);
            }
        }
        crc ^= Hdlc.HDLC_SHORT_CRC_RESET;
        byte current = (byte) (crc & 0xff);

        if (Hdlc.hdlcByteNeedsEscape(current)) {
            outputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
            outputBufferEscaped.put((byte) (current ^ Hdlc.HDLC_ESCAPE_XFORM));
        } else {
            outputBufferEscaped.put(current);
        }
        current = (byte) (crc >>> 8);
        if (Hdlc.hdlcByteNeedsEscape(current)) {
            outputBufferEscaped.put(Hdlc.HDLC_BYTE_ESC);
            outputBufferEscaped.put((byte) (current ^ Hdlc.HDLC_ESCAPE_XFORM));
        } else {
            outputBufferEscaped.put(current);
        }

        outputBufferEscaped.put(Hdlc.HDLC_BYTE_FLAG);
        outputBufferEscaped.flip();
        byte[] outboundFrame = new byte[outputBufferEscaped.limit()];
        outputBufferEscaped.get(outboundFrame);
        return outboundFrame;
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
                onError(HAL_ERROR_IOFAIL);
            }
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, "Error receiving incoming data: " + error);
            onError(HAL_ERROR_IOFAIL);
        }
    };

    private void readUartBuffer() throws IOException {
        byte[] buffer = new byte[MAX_SPINEL_SIZE];
        int count;
        while ((count = mDevice.read(buffer, buffer.length)) > 0) {
            processBuffer(buffer, count);
        }
    }

    private void processBuffer(byte[] buffer, int count) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(String.format("%02X ", (byte) buffer[i]));
            }
            Log.d(TAG, "to wpantund <- raw " + sb.toString());
        }

        for (int i = 0; i < count; i++) {
            byte current = buffer[i];
            switch (mFrameFlag) {
                case NOT_STARTED:
                    if (current == Hdlc.HDLC_BYTE_FLAG) {
                        handleFrameStart();
                        mFrameFlag = FrameFlag.STARTED;
                    }
                    continue;
                case UNESCAPING:
                    if (current == Hdlc.HDLC_BYTE_FLAG) {
                        handleFrameEnd();
                        handleFrameStart();
                        continue;
                    }
                    current ^= Hdlc.HDLC_ESCAPE_XFORM;
                    mFrameFlag = FrameFlag.STARTED;
                    break;
                case STARTED:
                    if (current == Hdlc.HDLC_BYTE_ESC) {
                        mFrameFlag = FrameFlag.UNESCAPING;
                        continue;
                    }
                    if (current == Hdlc.HDLC_BYTE_FLAG) {
                        handleFrameEnd();
                        handleFrameStart();
                        continue;
                    }
                    break;
                default:
                    break;
            }

            if (mInboundFrameSize >= 2) {
                mInboundFrameHDLCCRC = Hdlc.hdlcCrc16(mInboundFrameHDLCCRC,
                        mInboundFrame[mInboundFrameSize-2]);
            }
            mInboundFrame[mInboundFrameSize++] = current;
        }
    }

    /**
     * Restart the buffer and clear frame properties
     */
    private void handleFrameStart() {
        mFrameFlag = FrameFlag.NOT_STARTED;
        mInboundFrameSize = 0;
        mInboundFrameHDLCCRC = Hdlc.HDLC_SHORT_CRC_RESET;
    }

    /**
     * Parse a message once the frame end character is detected.
     */
    private void handleFrameEnd() {

        if (mInboundFrameSize <= 2) {
            handleFrameStart();
            return;
        }

        if (mLowpanDriverCallback == null) {
            return;
        }

        mInboundFrameHDLCCRC ^= Hdlc.HDLC_SHORT_CRC_RESET;
        mInboundFrameSize -= 2;
        short frameCrc = (short) (mInboundFrame[mInboundFrameSize] & 0xff |
                (mInboundFrame[mInboundFrameSize+1] << 8));
        if (mInboundFrameHDLCCRC != frameCrc) {
            Log.e(TAG, String.format("Frame CRC Mismatch: Calc:0x%04X != Frame:0x%04X",
                    mInboundFrameHDLCCRC, frameCrc));
            onError(HAL_ERROR_GARBAGE);
            return;
        }

        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mInboundFrameSize; i++) {
                sb.append(String.format("%02X ", mInboundFrame[i]));
            }
            Log.d(TAG, "from driver to wpantund unescaped <- " + sb.toString());
        }
        mLowpanDriverCallback.onReceiveFrame(Arrays.copyOf(mInboundFrame, mInboundFrameSize));
    }
}
