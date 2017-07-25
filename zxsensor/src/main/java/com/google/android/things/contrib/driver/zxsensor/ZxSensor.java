package com.google.android.things.contrib.driver.zxsensor;

import java.io.IOException;

/**
 * https://cdn.sparkfun.com/assets/learn_tutorials/3/4/5/XYZ_Interactive_Technologies_-_ZX_SparkFun_Sensor_Datasheet.pdf
 * https://cdn.sparkfun.com/assets/learn_tutorials/3/4/5/XYZ_I2C_Registers_v1.zip
 */
public interface ZxSensor extends AutoCloseable {

    interface IdMessageListener {
        /**
         * This message is used for sensor type identification.
         * Including hardware and firmware versions.
         */
        void onIdMessage(String id);
    }

    interface RangeListener {
        /**
         * Each range is between 0 to 240
         * the higher the number the closer the object to the
         * left or right sensor
         */
        void onRangeUpdate(int left, int right);
    }

    interface ZCoordinateListener {
        /**
         * Z coordinate is between 0 to 240
         */
        void onZCoordinateUpdate(int coordinate);
    }

    interface XCoordinateListener {
        /**
         * X coordinate is between 0 to 240
         */
        void onXCoordinateUpdate(int coordinate);
    }

    interface GestureEventListener {
        /**
         * Some sort of gesture was detected
         */
        void onGestureEvent();
    }

    interface PenUpEventListener {
        /**
         * Indicates that the reflector moved out of range.
         */
        void onPenUp();
    }

    interface MessageListener extends
        IdMessageListener,
        RangeListener,
        ZCoordinateListener,
        XCoordinateListener,
        GestureEventListener,
        PenUpEventListener {
        // Helper interface if you want all the messages
    }

    interface SwipeRightListener {
        /**
         * Swipe is the simplest gestural
         * interaction: the reflector moves in the
         * right direction over the sensor.
         * <p>
         * Speed is reported as a normalized
         * value between 1 to 10
         *
         * @param speed 1 = slow 10 = fast
         */
        void onSwipeRight(int speed);
    }

    interface SwipeLeftListener {
        /**
         * Swipe is the simplest gestural
         * interaction: the reflector moves in the
         * left direction over the sensor.
         * <p>
         * Speed is reported as a normalized
         * value between 1 to 10
         *
         * @param speed 1 = slow 10 = fast
         */
        void onSwipeLeft(int speed);
    }

    interface SwipeDownListener {
        /**
         * * Swipe is the simplest gestural
         * interaction: the reflector moves in a
         * downward direction over the sensor.
         * <p>
         * Speed is reported as a normalized
         * value between 1 to 10
         *
         * @param speed 1 = slow 10 = fast
         */
        void onSwipeDown(int speed);
    }

    interface HoverListener {
        /**
         * This gesture is sent when the reflector
         * stops (hover) over the sensor for few
         * seconds, then moves in a certain direction.
         *
         * @param position where the hover was detected left, right or center
         */
        void onHover(HoverPosition position);
    }

    interface GestureListener extends
        SwipeRightListener,
        SwipeLeftListener,
        SwipeDownListener,
        HoverListener {
        // Helper interface if you want all the gestures
    }

    /**
     * A descriptor for where the hover was detected
     */
    public enum HoverPosition {
        LEFT, RIGHT, CENTER
    }

    interface DeviceErrorListener {
        void onDeviceError(int error);
    }

    class Factory {
        public ZxSensorUart openViaUart(String uartBus) throws IOException {
            return new ZxSensorUart(uartBus);
        }

        public ZxSensorI2c openViaI2c(String i2cBus, String gpioDataNotifyPin) throws IOException {
            return new ZxSensorI2c(i2cBus, gpioDataNotifyPin);
        }
    }
}
