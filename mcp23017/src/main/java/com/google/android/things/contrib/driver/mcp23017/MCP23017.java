/*
 * Copyright 2019 Google Inc.
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

package com.google.android.things.contrib.driver.mcp23017;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.android.things.contrib.driver.mcp23017.ARegisters.DEFVAL_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.GPINTEN_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.GPIO_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.GPPU_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.INTCON_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.IODIR_A;
import static com.google.android.things.contrib.driver.mcp23017.ARegisters.IPOL_A;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.DEFVAL_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.GPINTEN_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.GPIO_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.GPPU_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.INTCON_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.IODIR_B;
import static com.google.android.things.contrib.driver.mcp23017.BRegisters.IPOL_B;

public class MCP23017 {

    private static final String LOG_TAG = MCP23017.class.getSimpleName();
    private static final byte DEFAULT_REGISTER_VALUE = 0;
    private static final int DEFAULT_ADDRESS = 0x20;
    private static final String A_CHANNEL = "A";
    private static final int DEFAULT_POLLING_TIMEOUT = 10;

    private I2cDevice device;
    private int address;
    private int pollingTimeout;
    private Set<MCP23017Pin> pins;
    private Set<MCP23017Pin> inputPins;
    private InterruptionListener interruptionListener;
    private ExecutorService executorService;

    private byte gpioA = 0;
    private byte gpioB = 0;

    public MCP23017(String bus) throws IOException {
        this(bus, DEFAULT_ADDRESS);
    }

    public MCP23017(String bus, int address) throws IOException {
        try {
            this.address = address;
            this.pollingTimeout = DEFAULT_POLLING_TIMEOUT;
            this.device = PeripheralManager.getInstance().openI2cDevice(bus, address);
            this.pins = new CopyOnWriteArraySet<>();
            defaultInitialization();

            interruptionListener = new InterruptionListener();
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(interruptionListener);
        } catch (IOException e) {
            Log.e(LOG_TAG, "MCP23017 cannot be created.", e);
            throw e;
        }
    }

    public void setPollingTimeout(int pollingTimeout) {
        this.pollingTimeout = pollingTimeout;
    }

    public Gpio openGpio(MCP23017GPIO gpio) throws IOException {
        MCP23017PinImpl pin = new MCP23017PinImpl(gpio.getName(), gpio.getAddress(),
                gpio.getRegisters(), this);
        if (pins.contains(pin)) {
            throw new IOException(gpio.getName() + " is already in use");
        }
        pins.add(pin);
        return pin;
    }

    public int getAddress() {
        return address;
    }

    public void close() throws IOException {
        interruptionListener.shutdown();
        executorService.shutdown();
        pins.forEach(this::closePin);
        pins.clear();
        if (device != null) {
            device.close();
        }
    }

    void setValue(MCP23017Pin pin, boolean value) throws IOException {
        byte state = device.readRegByte(pin.getRegisters().getGPIO());
        if (value) {
            state |= pin.getAddress();
        } else {
            state &= ~pin.getAddress();
        }
        device.writeRegByte(pin.getRegisters().getGPIO(), state);
    }

    boolean getValue(MCP23017Pin pin) throws IOException {
        byte state = device.readRegByte(pin.getRegisters().getGPIO());
        return (state & pin.getAddress()) == pin.getAddress();
    }

    void setDirection(MCP23017Pin pin, int direction) throws IOException {
        byte directionState = device.readRegByte(pin.getRegisters().getIODIR());
        byte gpioState = device.readRegByte(pin.getRegisters().getGPIO());
        if (Gpio.DIRECTION_IN == direction) {
            directionState |= pin.getAddress();
            inputPins.add(pin);
        } else if (Gpio.DIRECTION_OUT_INITIALLY_HIGH == direction) {
            directionState &= ~pin.getAddress();
            gpioState |= pin.getAddress();
            inputPins.remove(pin);
        } else if (Gpio.DIRECTION_OUT_INITIALLY_LOW == direction) {
            directionState &= ~pin.getAddress();
            gpioState &= ~pin.getAddress();
            inputPins.remove(pin);
        } else {
            throw new IllegalArgumentException("Unknown direction");
        }
        device.writeRegByte(pin.getRegisters().getIODIR(), directionState);
        device.writeRegByte(pin.getRegisters().getGPIO(), gpioState);
    }

    void setActiveType(MCP23017Pin pin, int activeType) throws IOException {
        byte activeTypeState = device.readRegByte(pin.getRegisters().getIPOL());
        if (Gpio.ACTIVE_HIGH == activeType) {
            activeTypeState &= ~pin.getAddress();
        } else if (Gpio.ACTIVE_LOW == activeType) {
            activeTypeState |= pin.getAddress();
        } else {
            throw new IllegalArgumentException("Unknown active state");
        }
        device.writeRegByte(pin.getRegisters().getIPOL(), activeTypeState);
    }

    void setEdgeTriggerType(MCP23017Pin pin, int triggerType) throws IOException {
        byte interruptionState = device.readRegByte(pin.getRegisters().getGRIPTEN());
        if (Gpio.EDGE_NONE == triggerType) {
            interruptionState &= ~pin.getAddress();
        } else if (Gpio.EDGE_FALLING == triggerType) {
            interruptionState |= pin.getAddress();
            configureFallingInterruption(pin);
        } else if (Gpio.EDGE_RISING == triggerType) {
            interruptionState |= pin.getAddress();
            configureRisingInterruption(pin);
        } else if (Gpio.EDGE_BOTH == triggerType) {
            interruptionState |= pin.getAddress();
            configureBothInterruption(pin);
        } else {
            throw new IllegalArgumentException("Unknown trigger type");
        }
        device.writeRegByte(pin.getRegisters().getGRIPTEN(), interruptionState);
    }

    boolean isInterrupted(MCP23017Pin pin) throws IOException {
        byte interruptionFlag = device.readRegByte(pin.getRegisters().getINTF());
        if (interruptionFlag > 0) {
            byte state = device.readRegByte(pin.getRegisters().getGPIO());
            return getInterruptionState(pin, state);
        }
        return false;
    }

    private void configureBothInterruption(MCP23017Pin pin) throws IOException {
        byte intconState = device.readRegByte(pin.getRegisters().getINTCON());
        intconState &= ~pin.getAddress();
        device.writeRegByte(pin.getRegisters().getINTCON(), intconState);
    }

    private void configureFallingInterruption(MCP23017Pin pin) throws IOException {
        byte defvalState = device.readRegByte(pin.getRegisters().getDEFVAL());
        defvalState |= pin.getAddress();
        device.writeRegByte(pin.getRegisters().getDEFVAL(), defvalState);
        byte intconState = device.readRegByte(pin.getRegisters().getINTCON());
        intconState |= pin.getAddress();
        device.writeRegByte(pin.getRegisters().getINTCON(), intconState);
    }

    private void configureRisingInterruption(MCP23017Pin pin) throws IOException {
        byte defvalState = device.readRegByte(pin.getRegisters().getDEFVAL());
        defvalState &= ~pin.getAddress();
        device.writeRegByte(pin.getRegisters().getDEFVAL(), defvalState);
        byte intconState = device.readRegByte(pin.getRegisters().getINTCON());
        intconState |= pin.getAddress();
        device.writeRegByte(pin.getRegisters().getINTCON(), intconState);
    }

    private synchronized boolean getInterruptionState(MCP23017Pin pin, byte state) {
        boolean result;
        if (pin.getName().contains(A_CHANNEL)) {
            result = (state & pin.getAddress()) != (gpioA & pin.getAddress());
            if ((state & pin.getAddress()) == pin.getAddress()) {
                gpioA |= pin.getAddress();
            } else {
                gpioA &= ~pin.getAddress();
            }
        } else {
            result = (state & pin.getAddress()) != (gpioB & pin.getAddress());
            if ((state & pin.getAddress()) == pin.getAddress()) {
                gpioB |= pin.getAddress();
            } else {
                gpioB &= ~pin.getAddress();
            }
        }
        return result;
    }

    private void defaultInitialization() throws IOException {
        device.writeRegByte(IODIR_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IODIR_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IPOL_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IPOL_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPPU_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPPU_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPIO_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPIO_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPINTEN_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPINTEN_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(INTCON_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(INTCON_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(DEFVAL_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(DEFVAL_B, DEFAULT_REGISTER_VALUE);
    }

    private void closePin(MCP23017Pin pin) {
        try {
            pin.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private class InterruptionListener implements Runnable {

        private final String LOG_TAG = InterruptionListener.class.getSimpleName();
        private boolean shutdown = false;

        @Override
        public void run() {
            while (!shutdown) {
                inputPins.stream()
                        .filter(this::isInterrupted)
                        .forEach(MCP23017Pin::executeCallbacks);
                try {
                    TimeUnit.MILLISECONDS.sleep(pollingTimeout);
                } catch (InterruptedException e) {
                    Log.e(this.LOG_TAG, e.getMessage(), e);
                }
            }
        }

        private boolean isInterrupted(MCP23017Pin pin) {
            try {
                return MCP23017.this.isInterrupted(pin);
            } catch (IOException e) {
                return false;
            }
        }

        void shutdown() {
            this.shutdown = true;
        }
    }
}
