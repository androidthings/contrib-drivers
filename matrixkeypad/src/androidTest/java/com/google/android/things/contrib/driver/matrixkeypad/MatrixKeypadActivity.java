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

import android.app.Activity;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import android.util.Log;
import android.view.KeyEvent;
import com.google.android.things.pio.Gpio;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MatrixKeypadActivity extends Activity {
    private static final String TAG = MatrixKeypadActivity.class.getSimpleName();

    private static final Gpio[] mRowPins = new Gpio[] {Mockito.mock(Gpio.class)};
    private static final Gpio[] mColPins = new Gpio[] {Mockito.mock(Gpio.class)};
    /* package */ static final int[] mKeyCodes = new int[] {KeyEvent.KEYCODE_NUMPAD_0};

    private BlockingQueue<KeyEvent> mKeyDownEvents;
    private BlockingQueue<KeyEvent> mKeyUpEvents;
    private MatrixKeypadInputDriver mMatrixKeypadDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mMatrixKeypadDriver = new MatrixKeypadInputDriver(mRowPins, mColPins, mKeyCodes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Keypad", e);
        }
        mKeyDownEvents = new LinkedBlockingQueue<>();
        mKeyUpEvents = new LinkedBlockingQueue<>();
        registerKeypad();
    }

    public void registerKeypad() {
        mMatrixKeypadDriver.register();
    }

    void sendMockKeyEvent(int key, boolean press) {
        if (press) {
            mMatrixKeypadDriver.keyDown(key);
        } else {
            mMatrixKeypadDriver.keyUp(key);
        }
    }

    KeyEvent getNextKeyDownEvent() throws InterruptedException {
        return mKeyDownEvents.poll(1L, TimeUnit.SECONDS);
    }

    KeyEvent getNextKeyUpEvent() throws InterruptedException {
        return mKeyUpEvents.poll(1L, TimeUnit.SECONDS);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mKeyDownEvents.offer(event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mKeyUpEvents.offer(event);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Closing activity");
        try {
            mMatrixKeypadDriver.unregister();
            mMatrixKeypadDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing Keypad", e);
        }
    }
}
