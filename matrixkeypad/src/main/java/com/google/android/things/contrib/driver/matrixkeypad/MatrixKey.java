/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.things.contrib.driver.matrixkeypad;

/**
 * A small class which contains properties related to the properties of an individual
 * key on the matrix keypad.
 */
public class MatrixKey {
    private final int mKeyCode;
    private boolean mPressed;

    // A default private implementation to prevent construction.
    private MatrixKey() { mKeyCode = -1; }

    /* package */ MatrixKey(int keyCode) {
        this.mKeyCode = keyCode;
    }

    public int getKeyCode() {
        return mKeyCode;
    }

    public boolean isPressed() {
        return mPressed;
    }

    /* package */ void setPressed(boolean pressed) {
        mPressed = pressed;
    }
}