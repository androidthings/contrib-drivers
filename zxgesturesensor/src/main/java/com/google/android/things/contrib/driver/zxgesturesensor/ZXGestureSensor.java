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

import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Driver for Sparkfun ZX Gesture Sensor with I2C/UART Support.
 *
 * TODOS:
 *   Improved gesture recognition
 */

@SuppressWarnings({"unused"})
public abstract class ZXGestureSensor implements AutoCloseable {
    protected static final String TAG = "ZXGESTURESENSOR";

    protected PeripheralManager pioService;

    protected GestureDetector mGestureDetector;

    protected ZXGestureSensor() {
        pioService = PeripheralManager.getInstance();
        mGestureDetector = new SimpleGestureDetector(null);
    }

    protected ZXGestureSensor(Handler handler) {
        pioService = PeripheralManager.getInstance();
        mGestureDetector = new SimpleGestureDetector(handler);
    }

    public static ZXGestureSensor getI2cSensor(String port, Handler handler) throws IOException {
        return new ZXGestureSensorI2c(port, handler);
    }

    public static ZXGestureSensor getI2cSensor(String port, int i2cAddress, Handler handler) throws IOException {
        return new ZXGestureSensorI2c(port, i2cAddress, handler);
    }

    public static ZXGestureSensor getUartSensor(String port, Handler handler) throws IOException {
        return new ZXGestureSensorUart(port, handler);
    }

    public GestureDetector getGestureDetector() {
        return mGestureDetector;
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        mGestureDetector = gestureDetector;
    }

    /**
     * Gestures that the sensor supports.
     */
    public enum Gesture {
        SWIPE_RIGHT,
        SWIPE_LEFT,
        SWIPE_UP,
        HOVER,
        HOVER_LEFT,
        HOVER_RIGHT,
        HOVER_UP;

        // mapping from gesture codes to gestures
        private static final Gesture[] idMap = {
                null, SWIPE_RIGHT, SWIPE_LEFT, SWIPE_UP,
                null, HOVER, HOVER_LEFT, HOVER_RIGHT, HOVER_UP};

        static Gesture getGesture(int code) {
            if (code < 0 || code >= idMap.length) return null;
            return idMap[code];
        }
    }

    /**
     * Interface definition for a callback to be invoked when the sensor detects gestures.
     */
    public interface OnGestureEventListener {
        /**
         * Called when a gesture is detected
         *
         * @param sensor the sensor which detected gesture.
         * @param gesture detected gesture.
         * @param param additional info for gesture (speed, etc.).
         */
        void onGestureEvent(ZXGestureSensor sensor, Gesture gesture, int param);
    }

    /**
     * Set the listener to be called when a gesture event occurred.
     *
     * @param listener gesture sensor listener to be invoked.
     */
    public void setListener(OnGestureEventListener listener) {
        mGestureDetector.setListener(listener);
    }

    @Override
    public abstract void close() throws IOException;
}