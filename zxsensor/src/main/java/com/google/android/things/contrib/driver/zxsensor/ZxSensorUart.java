package com.google.android.things.contrib.driver.zxsensor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import java.io.IOException;

public class ZxSensorUart implements ZxSensor {

    private final String bus;
    private final UartDevice mDevice;

    @Nullable
    private ZxSensor.IdMessageListener idMessageListener;
    @Nullable
    private RangeListener rangeListener;
    @Nullable
    private ZCoordinateListener zCoordinateListener;
    @Nullable
    private XCoordinateListener xCoordinateListener;
    @Nullable
    private GestureEventListener gestureEventListener;
    @Nullable
    private PenUpEventListener penUpEventListener;
    @Nullable
    private ZxSensor.SwipeLeftListener swipeLeftListener;
    @Nullable
    private ZxSensor.SwipeRightListener swipeRightListener;
    @Nullable
    private ZxSensor.SwipeDownListener swipeDownListener;
    @Nullable
    private ZxSensor.HoverListener hoverListener;
    @Nullable
    private ZxSensor.MessageListener messageListener;
    @Nullable
    private ZxSensor.GestureListener gestureListener;
    @Nullable
    private DeviceErrorListener deviceErrorListener;

    ZxSensorUart(String bus) throws IOException {
        this(bus, new PeripheralManagerService().openUartDevice(bus));
    }

    ZxSensorUart(String bus, UartDevice device) throws IOException {
        this.bus = bus;
        this.mDevice = device;
        configure();
    }

    private void configure() {
        try {
            mDevice.setBaudrate(115200);
            mDevice.setDataSize(8);
            mDevice.setParity(UartDevice.PARITY_NONE);
            mDevice.setStopBits(1);
        } catch (IOException e) {
            throw new IllegalStateException(bus + " cannot be configured.", e);
        }

    }

    public void setIdMessageListener(@Nullable ZxSensor.IdMessageListener idMessageListener) {
        this.idMessageListener = idMessageListener;
    }

    public void setRangeListener(@Nullable RangeListener rangeListener) {
        this.rangeListener = rangeListener;
    }

    public void setzCoordinateListener(@Nullable ZCoordinateListener zCoordinateListener) {
        this.zCoordinateListener = zCoordinateListener;
    }

    public void setxCoordinateListener(@Nullable XCoordinateListener xCoordinateListener) {
        this.xCoordinateListener = xCoordinateListener;
    }

    public void setGestureEventListener(@Nullable GestureEventListener gestureEventListener) {
        this.gestureEventListener = gestureEventListener;
    }

    public void setPenUpEventListener(@Nullable PenUpEventListener penUpEventListener) {
        this.penUpEventListener = penUpEventListener;
    }

    public void setSwipeLeftListener(@Nullable ZxSensor.SwipeLeftListener swipeLeftListener) {
        this.swipeLeftListener = swipeLeftListener;
    }

    public void setSwipeRightListener(@Nullable ZxSensor.SwipeRightListener swipeRightListener) {
        this.swipeRightListener = swipeRightListener;
    }

    public void setSwipeDownListener(@Nullable ZxSensor.SwipeDownListener swipeDownListener) {
        this.swipeDownListener = swipeDownListener;
    }

    public void setHoverListener(@Nullable ZxSensor.HoverListener hoverListener) {
        this.hoverListener = hoverListener;
    }

    public void setMessageListener(@Nullable ZxSensor.MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setGestureListener(@Nullable ZxSensor.GestureListener gestureListener) {
        this.gestureListener = gestureListener;
    }

    public void setDeviceErrorListener(@Nullable DeviceErrorListener errorListener) {
        this.deviceErrorListener = errorListener;
    }

    private UartDeviceCallback onUartBusHasData;

    @Override
    public void startMonitoringGestures() {
        onUartBusHasData = createCallback();
        try {
            mDevice.registerUartDeviceCallback(onUartBusHasData);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot listen for input from " + bus, e);
        }
    }

    /**
     * We create it like this because otherwise the class is untestable
     * <p>
     * https://issuetracker.google.com/issues/37133681
     */
    @NonNull
    private UartDeviceCallback createCallback() {
        return new UartDeviceCallback() {
            @Override
            public boolean onUartDeviceDataAvailable(UartDevice uart) {
                try {
                    byte[] buffer = new byte[4];
                    while (uart.read(buffer, buffer.length) > 0) {
                        byte messageCode = buffer[0];
                        switch (messageCode) {
                            case (byte) 0xFF:
                                handlePenUpMessage();
                                break;
                            case (byte) 0xFE:
                                handleRangesMessage(buffer[1], buffer[2]);
                                break;
                            case (byte) 0xFA:
                                handleXCoordinateMessage(buffer[1]);
                                break;
                            case (byte) 0xFB:
                                handleZCoordinateMessage(buffer[1]);
                                break;
                            case (byte) 0xFC:
                                handleGestureEventMessage(buffer[1], buffer[2], buffer[2]);
                                break;
                            case (byte) 0xF1:
                                handleIdMessage(buffer[1], buffer[2], buffer[3]);
                                break;
                            default:
                                // do nothing, fail silently
                                break;
                        }
                    }

                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read device data.", e);
                }
                return true;
            }

            @Override
            public void onUartDeviceError(UartDevice uart, int error) {
                if (deviceErrorListener != null) {
                    deviceErrorListener.onDeviceError(error);
                }
            }
        };
    }

    /**
     * Indicates that the reflector moved out
     * of range. Mark the end of a data
     * stream.
     */
    private void handlePenUpMessage() {
        if (messageListener != null) {
            messageListener.onPenUp();
        }
        if (penUpEventListener != null) {
            penUpEventListener.onPenUp();
        }
    }

    /**
     * Each range is represented by 1
     * unsigned byte with a range between
     * 0 to 240
     * <p>
     * These numbers get larger as the reflector
     * gets closer to the sensor so they are
     * more a measure of reflected energy.
     * They are used to calculate the Z and X coordinates
     *
     * @param leftRangeByte  higher as object closer
     * @param rightRangeByte higher as object closer
     */
    private void handleRangesMessage(byte leftRangeByte, byte rightRangeByte) {
        int left = (leftRangeByte & 0xff) + 120;
        int right = (rightRangeByte & 0xff) + 120;
        if (messageListener != null) {
            messageListener.onRangeUpdate(left, right);
        }
        if (rangeListener != null) {
            rangeListener.onRangeUpdate(left, right);
        }
    }

    /**
     * X coordinate is represented as a
     * signed byte with a 120 bias.
     *
     * @param byteCoordinate coordinate
     */
    private void handleXCoordinateMessage(byte byteCoordinate) {
        int coordinate = (byteCoordinate & 0xff) + 120;
        if (messageListener != null) {
            messageListener.onXCoordinateUpdate(coordinate);
        }
        if (xCoordinateListener != null) {
            xCoordinateListener.onXCoordinateUpdate(coordinate);
        }
    }

    /**
     * Z coordinate is represented as an
     * unsigned byte between 0 to 240
     *
     * @param byteCoordinate coordinate
     */
    private void handleZCoordinateMessage(byte byteCoordinate) {
        int coordinate = (byteCoordinate & 0xff) + 120;
        if (messageListener != null) {
            messageListener.onZCoordinateUpdate(coordinate);
        }
        if (zCoordinateListener != null) {
            zCoordinateListener.onZCoordinateUpdate(coordinate);
        }
    }

    /**
     * The first byte represents the gesture
     * code. The additional byte encode
     * parameters associated with the
     * gesture (i.e. speed)
     *
     * @param gesture  code of the type of gesture
     * @param position position of the hover event
     * @param speed    a number corresponding to the speed of the gesture
     */
    private void handleGestureEventMessage(byte gesture, byte position, byte speed) {
        if (messageListener != null) {
            messageListener.onGestureEvent();
        }
        if (gestureEventListener != null) {
            gestureEventListener.onGestureEvent();
        }

        if (gesture == 0x01) {
            if (gestureListener != null) {
                gestureListener.onSwipeRight(speed);
            }
            if (swipeRightListener != null) {
                swipeRightListener.onSwipeRight(speed);
            }
        } else if (gesture == 0x02) {
            if (gestureListener != null) {
                gestureListener.onSwipeLeft(speed);
            }
            if (swipeLeftListener != null) {
                swipeLeftListener.onSwipeLeft(speed);
            }
        } else if (gesture == 0x03) {
            if (gestureListener != null) {
                gestureListener.onSwipeDown(speed);
            }
            if (swipeDownListener != null) {
                swipeDownListener.onSwipeDown(speed);
            }
        } else if (gesture == 0x15) {
            if (position == 1) {
                if (gestureListener != null) {
                    gestureListener.onHover(ZxSensor.HoverPosition.RIGHT);
                }
                if (hoverListener != null) {
                    hoverListener.onHover(ZxSensor.HoverPosition.RIGHT);
                }
            } else if (position == 2) {
                if (gestureListener != null) {
                    gestureListener.onHover(ZxSensor.HoverPosition.LEFT);
                }
                if (hoverListener != null) {
                    hoverListener.onHover(ZxSensor.HoverPosition.LEFT);
                }
            } else {
                if (gestureListener != null) {
                    gestureListener.onHover(ZxSensor.HoverPosition.CENTER);
                }
                if (hoverListener != null) {
                    hoverListener.onHover(ZxSensor.HoverPosition.CENTER);
                }
            }
        }
    }

    /**
     * This message is used for sensor type
     * identification.
     *
     * @param sensorType      sensor type
     * @param hardwareVersion hardware version
     * @param firmwareVersion firmware version
     */
    private void handleIdMessage(byte sensorType, byte hardwareVersion, byte firmwareVersion) {
        String id = sensorType + "/" + hardwareVersion + "/" + firmwareVersion;
        if (messageListener != null) {
            messageListener.onIdMessage(id);
        }
        if (idMessageListener != null) {
            idMessageListener.onIdMessage(id);
        }
    }

    @Override
    public void stopMonitoringGestures() {
        mDevice.unregisterUartDeviceCallback(onUartBusHasData);
    }

    @Override
    public void close() {
        idMessageListener = null;
        rangeListener = null;
        zCoordinateListener = null;
        xCoordinateListener = null;
        gestureEventListener = null;
        penUpEventListener = null;
        swipeLeftListener = null;
        swipeRightListener = null;
        swipeDownListener = null;
        hoverListener = null;
        messageListener = null;
        gestureListener = null;
        deviceErrorListener = null;
        try {
            mDevice.close();
        } catch (IOException e) {
            throw new IllegalStateException("Could not close the UART: " + bus + " connection. " +
                                                    "You may see errors if you do not power cycle the device.", e);
        }
    }
}
