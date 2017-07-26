package com.google.android.things.contrib.driver.zxsensor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

class ZxSensorI2c implements ZxSensor {

    /**
     * 7 ReadOnly HeartBeat: This bit will toggle every time the status register has been read
     * 6 ReadOnly Not Used
     * 5 ReadOnly Edge Detection Event: currently unused, reads 0
     * 4 ReadOnly Hover-Move Gesture Available: X = gesture Y = gesture
     * 3 ReadOnly Hover Gesture Available: X = gesture Y = gesture
     * 2 ReadOnly Swipe Gesture Available: X = gesture Y = gesture
     * 1 ReadOnly Brightness value overflow: currently unused, reads 0
     * 0 ReadOnly Position Data Available: X = position Y = coordinate
     * <p>
     * Bits, 0,2,3,4. A 1 indicates that new X data is available in the Y registers
     * Bits, 0,2,3,4. Automatically resets to zero after being read
     */
    private static final int STATUS = 0x00;
    /**
     * 0x01 = data ready enable
     * <p>
     * 7 ReadOnly Not Used
     * 6 ReadOnly Not Used
     * 5 ReadWrite Edge Detection
     * 4 ReadWrite Hover-Move Gestures
     * 3 ReadWrite Hover Gestures
     * 2 ReadWrite Swipe Gestures
     * 1 ReadWrite Coordinate Data available
     * 0 ReadWrite Ranging Data available
     * <p>
     * A '1' in any of these bits will allow the DR pin to assert
     * when the respective event or gesture occurs.
     */
    private static final int DATA_READ_ENABLE = 0x01;
    private static final byte ENABLE_ALL = (byte) 0b00111111;
    private static final byte DISABLE_ALL = (byte) 0b00000000;
    /**
     * 7 ReadWrite Enable DR: 1 = DR enabled, 0 = DR always negated
     * 6 ReadWrite Force DR pin to assert (this bit auto-clears): 1 = Force DR pin to assert, 0 = normal DR operation
     * 5 ReadOnly NotUsed
     * 4 ReadOnly NotUsed
     * 3 ReadOnly NotUsed
     * 2 ReadOnly NotUsed
     * 1 ReadWrite DataReady pin Edge/Level Select: 1 = DR pin asserts for 1 pulse, 0 = DR pin asserts until STATUS is read
     * 0 ReadWrite DataReady pin Polarity Select: 1 = DR pin is active-high, 0 = DR pin is active-low
     */
    private static final int DATA_READY_CONFIG = 0x02;
    /**
     * 7-0 ReadOnly Gesture
     * <p>
     * 0x01	Right Swipe
     * 0x02	Left Swipe
     * 0x03	Up Swipe
     * 0x05	Hover
     * 0x06	Hover-Left
     * 0x07	Hover-Right
     * 0x08	Hover-Up
     * <p>
     * The most recent gesture appears in this register.
     * The gesture value remains until a new gesture is detected
     * The gesture bits in the status register can be used to determine
     * when to read a new value from this register
     */
    private static final int LAST_DETECTED_GESTURE = 0x04;
    /**
     * 7-0 ReadOnly Gesture Speed
     * <p>
     * The speed of the most recently detected gesture is stored here.
     * The value remains until a new gesture is detected.
     */
    private static final int LAST_DETECTED_GESTURE_SPEED = 0x05;
    /**
     * 7-0 ReadOnly X Position
     * <p>
     * The most recently calcuated X position is stored in this register
     */
    private static final int X_POSITION = 0x08;
    /**
     * 7-0 ReadOnly Z Position
     * <p>
     * The most recently calcuated Z position is stored in this register
     */
    private static final int Z_POSITION = 0x0a;
    /**
     * 7-0 ReadOnly Left Emitter Ranging Data
     * <p>
     * The left emitter ranging data is stored in this register.
     */
    private static final int LEFT_EMITTER_RANGING_DATA = 0x0c;
    /**
     * 7-0 ReadOnly Right Emitter Ranging Data
     * <p>
     * The right emitter ranging data is stored in this register.
     */
    private static final int RIGHT_EMITTER_RANGING_DATA = 0x0e;
    /**
     * 7-0 ReadOnly Register Map Version
     * <p>
     * This register is used to identify the register map version of attached sensor
     * All sensors share a register map.
     * Sensors with the same register map have the same data arrangement
     * 0x01 = Register Map v1
     */
    private static final int REGISTER_MAP_VERSION = 0xfe;
    /**
     * 7-0 ReadOnly Sensor Model ID
     * <p>
     * This register is used to identify the type of sensor attached.
     * 0x01 = XZ01
     */
    private static final int SENSOR_MODEL = 0xff;

    private final String i2cBus;
    private final String dataNotifyPinName;
    private final I2cDevice mDevice;
    private final Gpio mDataNotifyBus;

    @Nullable
    private RangeListener rangeListener;
    @Nullable
    private ZCoordinateListener zCoordinateListener;
    @Nullable
    private XCoordinateListener xCoordinateListener;
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

    private GpioCallback onI2cDataAvailable;

    ZxSensorI2c(String i2cBus, String gpioDataNotifyPin) throws IOException {
        this.i2cBus = i2cBus;
        this.dataNotifyPinName = gpioDataNotifyPin;
        PeripheralManagerService pioService = new PeripheralManagerService();
        this.mDevice = pioService.openI2cDevice(i2cBus, 0x10);
        this.mDataNotifyBus = pioService.openGpio(gpioDataNotifyPin);
        configure();
    }

    private void configure() {
        try {
            mDataNotifyBus.setActiveType(Gpio.ACTIVE_HIGH);
            mDataNotifyBus.setDirection(Gpio.DIRECTION_IN);
            mDataNotifyBus.setEdgeTriggerType(Gpio.EDGE_RISING);
        } catch (IOException e) {
            throw new IllegalStateException(dataNotifyPinName + " cannot be configured.", e);
        }
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

    public void startMonitoringGestures() {
        onI2cDataAvailable = createCallback();
        try {
            mDataNotifyBus.registerGpioCallback(onI2cDataAvailable);
            mDevice.writeRegByte(DATA_READ_ENABLE, ENABLE_ALL);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot listen for input from " + i2cBus, e);
        }
    }

    /**
     * We create it like this because otherwise the class is untestable
     * <p>
     * https://issuetracker.google.com/issues/37133681
     */
    @NonNull
    private GpioCallback createCallback() {
        return new GpioCallback() {
            @Override
            public boolean onGpioEdge(Gpio gpio) {
                try {
                    byte dataReadyConfiguration = mDevice.readRegByte(DATA_READY_CONFIG);
                    Log.d("TUT", "drc " + dataReadyConfiguration);

                    byte status = mDevice.readRegByte(STATUS);
                    Log.d("TUT", "Status " + status);

                    // (range) Position Data Available
                    if ((status & 0b00000001) == 0b00000001) {
                        handleRanges();
                    }
                    // Swipe Gesture Available
                    if ((status & 0b00000100) == 0b00000100) {
                        Log.d("TUT", "SWIPE GESTURE DATA AVAILABLE");
                        handleGesture();
                    }
                    // Hover Gesture Available
                    if ((status & 0b00001000) == 0b000001000) {
                        Log.d("TUT", "HOVER GESTURE DATA AVAILABLE");
                        handleGesture();
                    }
                    // Hover-Move Gesture Available
                    if ((status & 0b00001000) == 0b000001000) {
                        Log.d("TUT", "HOVER-Move GESTURE DATA AVAILABLE");
                        handleGesture();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot read device data.", e);
                }

                return true;
            }
        };
    }

    private void handleRanges() throws IOException {
        Log.d("TUT", "RANGE POSITION DATA AVAILABLE");
        byte zpos = mDevice.readRegByte(Z_POSITION);
        Log.d("TUT", "ZPos : " + zpos);
        if (messageListener != null) {
            messageListener.onZCoordinateUpdate(zpos);
        }
        if (zCoordinateListener != null) {
            zCoordinateListener.onZCoordinateUpdate(zpos);
        }
        byte xpos = mDevice.readRegByte(X_POSITION);
        Log.d("TUT", "XPos : " + xpos);
        if (messageListener != null) {
            messageListener.onXCoordinateUpdate(xpos);
        }
        if (xCoordinateListener != null) {
            xCoordinateListener.onXCoordinateUpdate(xpos);
        }
        byte left = mDevice.readRegByte(LEFT_EMITTER_RANGING_DATA);
        Log.d("TUT", "LRNG : " + left);
        byte right = mDevice.readRegByte(RIGHT_EMITTER_RANGING_DATA);
        Log.d("TUT", "RRNG : " + right);

        if (messageListener != null) {
            messageListener.onRangeUpdate(left, right);
        }
        if (rangeListener != null) {
            rangeListener.onRangeUpdate(left, right);
        }
    }

    private void handleGesture() throws IOException {
        if (messageListener != null) {
            messageListener.onGestureEvent();
        }
        byte gesture = mDevice.readRegByte(LAST_DETECTED_GESTURE);
        Log.d("TUT", "New gesture! " + gesture);
        byte speed = mDevice.readRegByte(LAST_DETECTED_GESTURE_SPEED);
        Log.d("TUT", "Speed! " + speed);

        if (gesture == 0x01) {
            Log.d("TUT", "Swipe Right");
            if (gestureListener != null) {
                gestureListener.onSwipeRight(speed);
            }
            if (swipeRightListener != null) {
                swipeRightListener.onSwipeRight(speed);
            }
        } else if (gesture == 0x02) {
            Log.d("TUT", "Swipe Left");
            if (gestureListener != null) {
                gestureListener.onSwipeLeft(speed);
            }
            if (swipeLeftListener != null) {
                swipeLeftListener.onSwipeLeft(speed);
            }
        } else if (gesture == 0x03) {
            Log.d("TUT", "Swipe Down");
            if (gestureListener != null) {
                gestureListener.onSwipeDown(speed);
            }
            if (swipeDownListener != null) {
                swipeDownListener.onSwipeDown(speed);
            }
        } else if (gesture == 0x05) {
            Log.d("TUT", "Hover");
            if (gestureListener != null) {
                gestureListener.onHover(ZxSensor.HoverPosition.CENTER);
            }
            if (hoverListener != null) {
                hoverListener.onHover(ZxSensor.HoverPosition.CENTER);
            }
        } else if (gesture == 0x06) {
            Log.d("TUT", "Hover Left");
            if (gestureListener != null) {
                gestureListener.onHover(ZxSensor.HoverPosition.LEFT);
            }
            if (hoverListener != null) {
                hoverListener.onHover(ZxSensor.HoverPosition.LEFT);
            }
        } else if (gesture == 0x07) {
            Log.d("TUT", "Hover Right");
            if (gestureListener != null) {
                gestureListener.onHover(ZxSensor.HoverPosition.RIGHT);
            }
            if (hoverListener != null) {
                hoverListener.onHover(ZxSensor.HoverPosition.RIGHT);
            }
        } else if (gesture == 0x08) {
            Log.d("TUT", "Hover Up");
            if (gestureListener != null) {
                gestureListener.onHover(ZxSensor.HoverPosition.CENTER);
            }
            if (hoverListener != null) {
                hoverListener.onHover(ZxSensor.HoverPosition.CENTER);
            }
        }
    }

    public void forceAnUpdate() {
        try {
            mDevice.writeRegByte(DATA_READY_CONFIG, (byte) 0b01000001);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte getRegisterVersion() {
        try {
            return mDevice.readRegByte(REGISTER_MAP_VERSION);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte getModelId() {
        try {
            return mDevice.readRegByte(SENSOR_MODEL);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void stopMonitoringGestures() {
        try {
            mDataNotifyBus.unregisterGpioCallback(onI2cDataAvailable);
            mDevice.writeRegByte(DATA_READ_ENABLE, DISABLE_ALL);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot listen for input from " + i2cBus, e);
        }
    }

    @Override
    public void close() throws Exception {
        rangeListener = null;
        zCoordinateListener = null;
        xCoordinateListener = null;
        swipeLeftListener = null;
        swipeRightListener = null;
        swipeDownListener = null;
        hoverListener = null;
        messageListener = null;
        gestureListener = null;
        mDevice.close();
        mDataNotifyBus.close();
    }
}
