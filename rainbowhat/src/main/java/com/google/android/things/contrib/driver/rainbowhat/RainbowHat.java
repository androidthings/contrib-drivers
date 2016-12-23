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

import android.support.annotation.StringDef;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

/**
 * Driver factory for the Rainbow Hat.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RainbowHat {
    public static final String BUS_SENSOR = "I2C1";
    public static final String BUS_DISPLAY = "I2C1";
    public static final String BUS_LEDSTRIP = "SPI0.0";
    public static final String PWM_PIEZO = "PWM1";
    public static final String PWM_SERVO = "PWM0";
    @StringDef({BUTTON_A, BUTTON_B, BUTTON_C})
    public @interface ButtonPin {}
    public static final String BUTTON_A = "BCM21";
    public static final String BUTTON_B = "BCM20";
    public static final String BUTTON_C = "BCM16";
    public static final Button.LogicState BUTTON_LOGIC_STATE = Button.LogicState.PRESSED_WHEN_LOW;
    @StringDef({LED_RED, LED_GREEN, LED_BLUE})
    public @interface LedPin {}
    public static final String LED_RED = "BCM6";
    public static final String LED_GREEN = "BCM19";
    public static final String LED_BLUE = "BCM26";
    public static final int LEDSTRIP_LENGTH = 7;

    public static Bmx280 openSensor() throws IOException {
        return new Bmx280(BUS_SENSOR);
    }

    public static Bmx280SensorDriver createSensorDriver() throws IOException {
        return new Bmx280SensorDriver(BUS_SENSOR);
    }

    public static AlphanumericDisplay openDisplay() throws IOException {
        return new AlphanumericDisplay(BUS_SENSOR);
    }

    public static Speaker openPiezo() throws IOException {
        return new Speaker(PWM_PIEZO);
    }

    public static Servo openServo() throws IOException {
        return new Servo(PWM_SERVO);
    }

    public static Button openButton(@ButtonPin String pin) throws IOException {
        return new Button(pin, BUTTON_LOGIC_STATE);
    }

    static ButtonInputDriver createButtonInputDriver(@ButtonPin String pin, int keycode) throws IOException {
        return new ButtonInputDriver(pin, BUTTON_LOGIC_STATE, keycode);
    }

    public static Gpio openLed(@LedPin String pin) throws IOException {
        PeripheralManagerService pioService = new PeripheralManagerService();
        Gpio ledGpio = pioService.openGpio(pin);
        ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return ledGpio;
    }

    public static Apa102 openLedStrip() throws IOException {
        return new Apa102(BUS_LEDSTRIP, Apa102.Mode.BGR);
    }
}
