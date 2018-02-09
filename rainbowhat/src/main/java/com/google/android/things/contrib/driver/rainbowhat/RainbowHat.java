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

import android.os.Build;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmservo.Servo;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Driver factory for the Rainbow Hat.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class RainbowHat {

    private interface BoardDefaults {
        String getI2cBus();
        String getSpiBus();
        String getPiezoPwm();
        String getServoPwm();
        String getButtonA();
        String getButtonB();
        String getButtonC();
        String getLedR();
        String getLedG();
        String getLedB();
    }
    private static final class Rpi3BoardDefaults implements BoardDefaults {
        public String getI2cBus() { return "I2C1";}
        public String getSpiBus() { return "SPI0.0";}
        public String getPiezoPwm() { return "PWM1";}
        public String getServoPwm() { return "PWM0";}
        public String getButtonA() { return "BCM21";}
        public String getButtonB() { return "BCM20";}
        public String getButtonC() { return "BCM16";}
        public String getLedR() { return "BCM6";}
        public String getLedG() { return "BCM19";}
        public String getLedB() { return "BCM26";}
    }
    private static final class Imx7BoardDefaults implements BoardDefaults {
        public String getI2cBus() { return "I2C1";}
        public String getSpiBus() { return "SPI3.1";}
        public String getPiezoPwm() { return "PWM2";}
        public String getServoPwm() { return "PWM1";}
        public String getButtonA() { return "GPIO6_IO14";}
        public String getButtonB() { return "GPIO6_IO15";}
        public String getButtonC() { return "GPIO2_IO07";}
        public String getLedR() { return "GPIO2_IO02";}
        public String getLedG() { return "GPIO2_IO00";}
        public String getLedB() { return "GPIO2_IO05";}
    }

    private static final BoardDefaults BOARD = Build.DEVICE.equals("rpi3") ?
            new Rpi3BoardDefaults() : new Imx7BoardDefaults();
    public static final Button.LogicState BUTTON_LOGIC_STATE = Button.LogicState.PRESSED_WHEN_LOW;
    public static final int LEDSTRIP_LENGTH = 7;

    public static Bmx280 openSensor() throws IOException {
        return new Bmx280(BOARD.getI2cBus());
    }

    public static Bmx280SensorDriver createSensorDriver() throws IOException {
        return new Bmx280SensorDriver(BOARD.getI2cBus());
    }

    public static AlphanumericDisplay openDisplay() throws IOException {
        return new AlphanumericDisplay(BOARD.getI2cBus());
    }

    public static Speaker openPiezo() throws IOException {
        return new Speaker(BOARD.getPiezoPwm());
    }

    public static Servo openServo() throws IOException {
        return new Servo(BOARD.getServoPwm());
    }

    public static Button openButtonA() throws IOException {
        return openButton(BOARD.getButtonA());
    }
    public static Button openButtonB() throws IOException {
        return openButton(BOARD.getButtonB());
    }
    public static Button openButtonC() throws IOException {
        return openButton(BOARD.getButtonC());
    }
    public static Button openButton(String pin) throws IOException {
        return new Button(pin, BUTTON_LOGIC_STATE);
    }

    public static ButtonInputDriver createButtonAInputDriver(int keycode) throws IOException {
        return createButtonInputDriver(BOARD.getButtonA(), keycode);
    }
    public static ButtonInputDriver createButtonBInputDriver(int keycode) throws IOException {
        return createButtonInputDriver(BOARD.getButtonB(), keycode);
    }
    public static ButtonInputDriver createButtonCInputDriver(int keycode) throws IOException {
        return createButtonInputDriver(BOARD.getButtonC(), keycode);
    }
    public static ButtonInputDriver createButtonInputDriver(String pin, int keycode) throws IOException {
        return new ButtonInputDriver(pin, BUTTON_LOGIC_STATE, keycode);
    }

    public static Gpio openLedRed() throws IOException {
        return openLed(BOARD.getLedR());
    }
    public static Gpio openLedGreen() throws IOException {
        return openLed(BOARD.getLedG());
    }
    public static Gpio openLedBlue() throws IOException {
        return openLed(BOARD.getLedB());
    }
    public static Gpio openLed(String pin) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        Gpio ledGpio = pioService.openGpio(pin);
        ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return ledGpio;
    }

    public static Apa102 openLedStrip() throws IOException {
        return new Apa102(BOARD.getSpiBus(), Apa102.Mode.BGR);
    }
}
