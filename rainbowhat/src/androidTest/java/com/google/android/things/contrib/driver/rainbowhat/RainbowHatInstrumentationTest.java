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

package com.google.android.things.contrib.driver.rainbowhat;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import android.graphics.Color;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RainbowHatInstrumentationTest {
    /**
     * Verify that the LED can be opened and closed.
     */
    @Test
    @UiThreadTest
    public void testOpenLed() throws IOException {
        Gpio red = RainbowHat.openLedRed();
        Gpio green = RainbowHat.openLedGreen();
        Gpio blue = RainbowHat.openLedBlue();
        Assert.assertNotNull(red);
        Assert.assertNotNull(green);
        Assert.assertNotNull(blue);
        red.close();
        green.close();
        blue.close();
    }

    /**
     * Verify that the buttons are not null.
     */
    @Test
    @UiThreadTest
    public void testOpenButton() throws IOException {
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    Button buttonA = RainbowHat.openButtonA();
                    Button buttonB = RainbowHat.openButtonB();
                    Button buttonC = RainbowHat.openButtonC();
                    Assert.assertNotNull(buttonA);
                    Assert.assertNotNull(buttonB);
                    Assert.assertNotNull(buttonC);
                    buttonA.close();
                    buttonB.close();
                    buttonC.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Accesses Bmx280 sensors and reads from them.
     */
    @Test
    public void testSensor() throws IOException {
        Bmx280 bmx280 = RainbowHat.openSensor();
        bmx280.setPressureOversampling(Bmx280.OVERSAMPLING_1X);
        bmx280.setTemperatureOversampling(Bmx280.OVERSAMPLING_1X);
        // Reading values from the chip without an exception indicates a success
        bmx280.readTemperatureAndPressure();
        bmx280.close();
    }

    /**
     * Opens display and writes values to it.
     */
    @Test
    public void testDisplay() throws IOException {
        AlphanumericDisplay display = RainbowHat.openDisplay();
        display.display("TEST");
        display.clear();
        display.close();
    }

    /**
     * Opens Apa102 LED strip and writes colors to it.
     */
    @Test
    public void testLedStrip() throws IOException {
        Apa102 ledStrip = RainbowHat.openLedStrip();
        ledStrip.write(new int[] {Color.BLUE, Color.BLUE, Color.BLUE, Color.BLUE,
            Color.BLUE, Color.BLUE, Color.BLUE});
        ledStrip.write(new int[] {Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK,
            Color.BLACK, Color.BLACK, Color.BLACK});
        ledStrip.close();
    }

    /**
     * Opens PWM buzzer and sends values to it.
     */
    @Test
    public void testPwmBuzzer() throws IOException {
        Speaker speaker = RainbowHat.openPiezo();
        speaker.play(200);
        speaker.stop();
        speaker.close();
    }

    /**
     * Opens PWM buzzer and sends values to it.
     */
    @Test
    public void testPwmServo() throws IOException {
        Servo servo = RainbowHat.openServo();
        servo.close();
    }
}
