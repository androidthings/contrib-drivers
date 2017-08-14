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

/**
 * Simple gesture detector (not really a detector) that relays gesture events from the sensor
 */
public class SimpleGestureDetector extends GestureDetector {
    private Handler mHandler;

    private final static int DEBOUNCE_DELAY_MS = 100;
    private boolean mSensorUpdated;
    private boolean mDebouncing;

    private ZXGestureSensor.Gesture mLastGesture;

    public SimpleGestureDetector(Handler handler) {
        mHandler = handler == null ? new Handler() : handler;
    }

    /**
     * Blocks immediate gesture event firings followed by a gesture event.
     */
    private final Runnable bounceBack = new Runnable() {
        @Override
        public void run() {
            mDebouncing = false;
        }
    };

    @Override
    protected void updateXpos(int xpos) {
        mSensorUpdated = true;
    }

    @Override
    protected void updateZpos(int zpos) {
        mSensorUpdated = true;
    }

    @Override
    protected void updateGesture(ZXGestureSensor.Gesture gesture, int gestureParams) {
        if (mLastGesture != null && mLastGesture != gesture) {
            mDebouncing = false;
        }
        mLastGesture = gesture;
        if(mListener != null) {
            if (mSensorUpdated && !mDebouncing) {
                mListener.onGestureEvent(mSensor, gesture, gestureParams);
                mDebouncing = true;
                mSensorUpdated = false;
                mHandler.postDelayed(bounceBack, DEBOUNCE_DELAY_MS);
            }
        }
    }

    @Override
    protected void updatePenUp() {

    }
}
