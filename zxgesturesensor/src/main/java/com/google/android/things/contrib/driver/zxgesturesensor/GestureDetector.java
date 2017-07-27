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

/**
 * A base gesture detector class. A gesture detector gets input from sensor and fires a callback
 * when a gesture is detected.
 */
public abstract class GestureDetector {
    protected ZXGestureSensor mSensor;
    protected ZXGestureSensor.OnGestureEventListener mListener;

    protected int mXpos;
    protected int mZpos;
    protected int[] mRanges = new int[2];
    protected ZXGestureSensor.Gesture mGesture;
    protected int mGestureParams;

    public static final int POSITION_PENUP = 999;

    /**
     * Sets x position and puts it to gesture detector
     * @param xpos
     */
    void setXpos(int xpos) {
        this.mXpos = xpos;
        updateXpos(xpos);
    }

    /**
     * Sets z position and puts it to gesture detector
     * @param zpos
     */
    void setZpos(int zpos) {
        this.mZpos = zpos;
        updateZpos(zpos);
    }

    /**
     * Sets sensor in pen-up state (nothing is detected)
     */
    void penUp() {
        mXpos = mZpos = POSITION_PENUP;
        updatePenUp();
    }

    /**
     * Sets ranges of two IR LEDs on the sensor
     * @param rangeL left LED
     * @param rangeR right LED
     */
    void setRanges(int rangeL, int rangeR) {
        this.mRanges[0] = rangeL;
        this.mRanges[1] = rangeR;
    }

    /**
     * Sets gesture and its parameter, puts them to gesture detector
     * @param gesture gesture to be set
     * @param gestureParams param associated with the gesture
     */
    void setGesture(ZXGestureSensor.Gesture gesture, int gestureParams) {
        this.mGesture = gesture;
        this.mGestureParams = gestureParams;
        updateGesture(gesture, gestureParams);
    }

    public int getXpos() {
        return mXpos;
    }

    public int getZpos() {
        return mZpos;
    }

    public int[] getRanges() {
        return mRanges;
    }

    public int getGestureParams() {
        return mGestureParams;
    }

    public ZXGestureSensor.Gesture getGesture() {
        return mGesture;
    }

    protected abstract void updateXpos(int xpos);
    protected abstract void updateZpos(int zpos);
    protected abstract void updateGesture(ZXGestureSensor.Gesture gesture, int gestureParams);
    protected abstract void updatePenUp();

    public void setListener(ZXGestureSensor.OnGestureEventListener listener) {
        mListener = listener;
    }
}
