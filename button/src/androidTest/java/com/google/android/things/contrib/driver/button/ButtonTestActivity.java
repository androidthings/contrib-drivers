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
package com.google.android.things.contrib.driver.button;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.things.contrib.driver.button.Button.LogicState;
import com.google.android.things.pio.Gpio;

import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ButtonTestActivity extends Activity {

    private static final String TAG = "ButtonTestActivity";

    public static final int KEYCODE = KeyEvent.KEYCODE_A;

    private Gpio mGpio;
    private Button mButton;
    private ButtonInputDriver mInputDriver;

    private BlockingQueue<KeyEvent> mKeyDownEvents;
    private BlockingQueue<KeyEvent> mKeyUpEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGpio = Mockito.mock(Gpio.class);
        try {
            mButton = new Button(mGpio, LogicState.PRESSED_WHEN_HIGH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Button", e);
        }
        mInputDriver = new ButtonInputDriver(mButton, KEYCODE);
        mInputDriver.register();

        mKeyDownEvents = new LinkedBlockingQueue<>();
        mKeyUpEvents = new LinkedBlockingQueue<>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mInputDriver.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing Button", e);
        }
    }

    void sendMockButtonEvent(boolean press) {
        mButton.performButtonEvent(press);
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
}
