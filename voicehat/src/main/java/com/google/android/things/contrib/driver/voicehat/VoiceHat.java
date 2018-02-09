/*
 * Copyright 2017, The Android Open Source Project
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

import android.os.Build;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.Button.LogicState;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import java.io.IOException;

/**
 * This class provides a driver to use the VoiceHat, which enables connection to a microphone and
 * speaker for audio input and output with the {@link Max98357A} DAC. It also exposes access to an
 * LED on the board.
 *
 * In order to use the pushbutton as an input driver, an additional permission must be added:
 *
 * <pre>
 * &lt;uses-permission android:name=&quot;com.google.android.things.permission.MANAGE_INPUT_DRIVERS&quot; /&gt;
 * </pre>
 */
public class VoiceHat {
    private static final int BUTTON_DEBOUNCE_DELAY_MS = 20;

    private static final String RPI_TRIGGER_GPIO = "BCM16";
    private static final String RPI_BUTTON_GPIO = "BCM23";
    private static final String RPI_LED_GPIO = "BCM25";

    /* package */ static void assertRaspberryPi3() {
        if (!Build.DEVICE.equals("rpi3")) {
            throw new RuntimeException("This device is not a Raspberry Pi 3, so it cannot use the d"
                + "efault pin names");
        }
    }

    /**
     * Opens the button using the default pin on the Raspberry Pi.
     *
     * @return Pushbutton on the VoiceHat
     */
    public static Button openButton() throws IOException {
        assertRaspberryPi3();
        return openButton(RPI_BUTTON_GPIO);
    }

    /**
     * Opens the button using a specified pin.
     *
     * @param pinName The pin attached to the button.
     * @return Pushbutton on the VoiceHat
     */
    public static Button openButton(String pinName) throws IOException {
        Button pushButton = new Button(pinName, LogicState.PRESSED_WHEN_LOW);
        pushButton.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS);
        return pushButton;
    }

    /**
     * Opens the LED using the default pin on the Raspberry Pi.
     *
     * @return LED on the VoiceHat
     */
    public static Gpio openLed() throws IOException {
        assertRaspberryPi3();
        return openLed(RPI_LED_GPIO);
    }

    /**
     * Opens the LED using a specified pin.
     *
     * @return LED on the VoiceHat
     */
    public static Gpio openLed(String ledGpioPin) throws IOException {
        PeripheralManager pioService = PeripheralManager.getInstance();
        Gpio led = pioService.openGpio(ledGpioPin);
        led.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        return led;
    }

    /**
     * Opens an InputDriver using the default pin on the Raspberry Pi.
     *
     * @return InputDriver for the pushbutton on the VoiceHat
     */
    public static ButtonInputDriver createButtonInputDriver(int keyCode) throws IOException {
        assertRaspberryPi3();
        return createButtonInputDriver(RPI_BUTTON_GPIO, keyCode);
    }

    /**
     * Opens an InputDriver using a specified.
     *
     * @return InputDriver for the pushbutton on the VoiceHat
     */
    public static ButtonInputDriver createButtonInputDriver(String buttonGpioPin, int keyCode)
        throws IOException {
        return new ButtonInputDriver(buttonGpioPin, LogicState.PRESSED_WHEN_LOW, keyCode);
    }

    /**
     * Obtains the driver for the VoiceHat DAC using the default pin for the trigger on the
     * Raspberry Pi.
     *
     * @return Driver for the DAC.
     */
    public static Max98357A openDac() throws IOException {
        // On the VoiceHat, the GAIN_SLOT pin on the DAC is not connected.
        Max98357A dac = new Max98357A(RPI_TRIGGER_GPIO, null);
        return dac;
    }

    /**
     * Obtains the driver for the VoiceHat DAC.
     *
     * @param ampEnablePin The pin attached to the DAC trigger.
     * @return Driver for the DAC.
     */
    public static Max98357A openDac(String ampEnablePin) throws IOException {
        // On the VoiceHat, the GAIN_SLOT pin on the DAC is not connected.
        Max98357A dac = new Max98357A(ampEnablePin, null);
        return dac;
    }

    private VoiceHat() {
        // Private constructor
    }
}