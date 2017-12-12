/*
 * Copyright 2016 Google Inc.
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

package com.google.android.things.contrib.driver.voicehat;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VoiceHatPeripheralInstrumentationTest {
    /**
     * Verify that the manage input drivers permission is granted.
     */
    @Test
    public void testInputPermissionGranted() {
        String manageInputDrivers = "com.google.android.things.permission.MANAGE_INPUT_DRIVERS";
        Context context = InstrumentationRegistry.getContext();
        Assert.assertEquals(PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(manageInputDrivers));
    }

    /**
     * Verify that the LED is null if not passed as a parameter.
     */
    @Test
    @UiThreadTest
    public void testOpenLed() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        Gpio led = VoiceHat.openLed();
        Assert.assertNotNull(led);
        led.close();
    }

    /**
     * Verify that the button is null if not passed as a parameter.
     */
    @Test
    @UiThreadTest
    public void testButtonNull() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Button button = VoiceHat.openButton();
                    Assert.assertNotNull(button);
                    button.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Registers button as an InputDriver
     */
    @Test
    @UiThreadTest
    public void testButtonInputDriver() throws IOException {
        InstrumentationTestUtils.assertRaspberryPiOnly();
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    ButtonInputDriver driver =
                        VoiceHat.createButtonInputDriver(KeyEvent.KEYCODE_ENTER);
                    driver.register();
                    driver.unregister();
                    driver.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
