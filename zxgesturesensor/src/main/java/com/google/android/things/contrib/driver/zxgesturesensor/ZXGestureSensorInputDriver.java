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

package com.google.android.things.contrib.driver.zxgesturesensor;

import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import com.google.android.things.userdriver.input.InputDriverEvent;
import java.io.IOException;
import java.util.EnumMap;

public class ZXGestureSensorInputDriver implements AutoCloseable {
    private static final String TAG = "ZXGESTURESENSOR";
    public static final String DRIVER_NAME = "ZX Gesture Sensor";
    public static final int DRIVER_VERSION = 1;

    private ZXGestureSensor mSensor;
    private EnumMap<ZXGestureSensor.Gesture, Integer> mKeyMap;
    private InputDriver mDriver;

    public ZXGestureSensor getSensor() {
        return mSensor;
    }

    /**
     * Interface on which the sensor is connected.
     */
    public enum ConnectionType {
        UART,
        I2C
    }

    /**
     * Create a new input driver for Sparkfun ZX Gesture Sensor connected on given port
     * via I2C or UART connection.
     * When one of the predefined gestures are detected, the driver emits corresponding
     * {@link android.view.KeyEvent}.
     * <p>Keycodes for gestures:
     * <ul>
     * <li>SWIPE_UP => DPAD_UP
     * <li>SWIPE_LEFT => DPAD_LEFT
     * <li>SWIPE_RIGHT => DPAD_RIGHT
     * <li>HOVER => ENTER
     * <li>HOVER_UP => PAGE_UP
     * <li>HOVER_LEFT => MOVE_END
     * <li>HOVER_RIGHT => MOVE_HOME
     * </ul>
     *
     * @param port port identifier
     * @param conType connection type. either UART or I2C
     * @see #register()
     */
    public ZXGestureSensorInputDriver(String port, ConnectionType conType) {
        this(port, conType, null, null);
    }

    /**
     * Create a new input driver for Sparkfun ZX Gesture Sensor connected on given port
     * and address via I2C connection
     * When one of the predefined gestures are detected, the driver emits corresponding
     * {@link android.view.KeyEvent}.
     * <p>Keycodes for gestures:
     * <ul>
     * <li>SWIPE_UP => DPAD_UP
     * <li>SWIPE_LEFT => DPAD_LEFT
     * <li>SWIPE_RIGHT => DPAD_RIGHT
     * <li>HOVER => ENTER
     * <li>HOVER_UP => PAGE_UP
     * <li>HOVER_LEFT => MOVE_END
     * <li>HOVER_RIGHT => MOVE_HOME
     * </ul>
     *
     * @param port port identifier
     * @param i2cAddress I2C address that sensor listens on. either 0x10 or 0x11
     * @see #register()
     */
    public ZXGestureSensorInputDriver(String port, int i2cAddress) {
        this(port, i2cAddress, null, null);
    }

    /**
     * create a new input driver for Sparkfun ZX Gesture Sensor connected on given port
     * via I2C or UART connection.
     * When one of the predefined gestures are detected, the driver emits corresponding
     * {@link android.view.KeyEvent}.
     *
     * @param port      port identifier
     * @param conType   connection type. either UART or I2C
     * @param handler   handler to be used for handling timer functions
     * @param keyMap    key mapping for gestures. unmapped gestures will be ignored.
     * @see #register()
     */
    public ZXGestureSensorInputDriver(String port,
                                      ConnectionType conType,
                                      Handler handler,
                                      EnumMap<ZXGestureSensor.Gesture, Integer> keyMap) {
        setKeyMap(keyMap);

        try {
            if (conType == ConnectionType.I2C) {
                mSensor = ZXGestureSensor.getI2cSensor(port, handler);
            } else {
                mSensor = ZXGestureSensor.getUartSensor(port, handler);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while connecting to the sensor.", e);
        }
    }


    /**
     * create a new input driver for Sparkfun ZX Gesture Sensor connected on given port
     * and address via I2C connection.
     * When one of the predefined gestures are detected, the driver emits corresponding
     * {@link android.view.KeyEvent}.
     *
     * @param port      port identifier
     * @param i2cAddress I2C address that sensor listens on. either 0x10 or 0x11
     * @param handler   handler to be used for handling timer functions
     * @param keyMap    key mapping for gestures. unmapped gestures will be ignored.
     * @see #register()
     */
    public ZXGestureSensorInputDriver(String port,
                                      int i2cAddress,
                                      Handler handler,
                                      EnumMap<ZXGestureSensor.Gesture, Integer> keyMap) {
        setKeyMap(keyMap);

        try {
            mSensor = ZXGestureSensor.getI2cSensor(port, i2cAddress, handler);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while connecting to the sensor.", e);
        }
    }

    private void setKeyMap(EnumMap<ZXGestureSensor.Gesture, Integer> keyMap) {
        if (keyMap == null) {
            mKeyMap = new EnumMap<ZXGestureSensor.Gesture, Integer>(ZXGestureSensor.Gesture.class);
            mKeyMap.put(ZXGestureSensor.Gesture.SWIPE_UP, KeyEvent.KEYCODE_DPAD_UP);
            mKeyMap.put(ZXGestureSensor.Gesture.SWIPE_LEFT, KeyEvent.KEYCODE_DPAD_LEFT);
            mKeyMap.put(ZXGestureSensor.Gesture.SWIPE_RIGHT, KeyEvent.KEYCODE_DPAD_RIGHT);
            mKeyMap.put(ZXGestureSensor.Gesture.HOVER, KeyEvent.KEYCODE_ENTER);
            mKeyMap.put(ZXGestureSensor.Gesture.HOVER_LEFT, KeyEvent.KEYCODE_MOVE_HOME);
            mKeyMap.put(ZXGestureSensor.Gesture.HOVER_RIGHT, KeyEvent.KEYCODE_MOVE_END);
            mKeyMap.put(ZXGestureSensor.Gesture.HOVER_UP, KeyEvent.KEYCODE_PAGE_UP);
        } else {
            mKeyMap = keyMap;
        }
    }

    /**
     * Register the driver in the framework.
     */
    public void register() {
        if (mSensor == null) {
            throw new IllegalStateException("Cannot register closed driver");
        }
        if (mDriver == null) {
            mDriver = build(mSensor, mKeyMap);
            UserDriverManager.getInstance().registerInputDriver(mDriver);
        }
    }

    /**
     * Unregister the driver from the framework.
     */
    public void unregister() {
        if (mDriver != null) {
            UserDriverManager.getInstance().unregisterInputDriver(mDriver);
            mDriver = null;
        }
    }

    private static InputDriver build(ZXGestureSensor sensor,
                             final EnumMap<ZXGestureSensor.Gesture, Integer> keyMap) {
        int[] keys = new int[keyMap.size()];
        Object[] oKeys = keyMap.values().toArray();
        for (int i = 0; i < keyMap.size(); i++) {
            keys[i] = (int)oKeys[i];
        }
        final InputDriver inputDriver = new InputDriver.Builder()
                .setName(DRIVER_NAME)
                .setSupportedKeys(keys)
                .build();
        final InputDriverEvent inputEvent = new InputDriverEvent();
        sensor.setListener(new ZXGestureSensor.OnGestureEventListener() {
            @Override
            public void onGestureEvent(ZXGestureSensor sensor,
                                       ZXGestureSensor.Gesture gesture, int params) {
                if (keyMap.containsKey(gesture)) {
                    inputEvent.clear();
                    inputEvent.setKeyPressed(keyMap.get(gesture), true);
                    inputDriver.emit(inputEvent);
                    inputEvent.clear();
                    inputEvent.setKeyPressed(keyMap.get(gesture), false);
                    inputDriver.emit(inputEvent);
                }
            }
        });
        return inputDriver;
    }

    /**
     * Close the driver and the underlying device.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        unregister();
        if (mSensor != null) {
            try {
                mSensor.close();
            } finally {
                mSensor = null;
            }
        }
    }
}
