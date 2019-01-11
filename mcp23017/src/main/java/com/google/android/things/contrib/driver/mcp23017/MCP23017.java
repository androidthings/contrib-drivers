package com.google.android.things.contrib.driver.mcp23017;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class MCP23017 {

    private static final String LOG_TAG = MCP23017.class.getSimpleName();
    private static final int DEFAULT_ADDRESS = 0x20;

    private I2cDevice device;
    private int address;

    private byte directionA = 0;
    private byte directionB = 0;
    private byte gpioA = 0;
    private byte gpioB = 0;
    private byte activationA = 0;
    private byte activationB = 0;

    public MCP23017(String bus) {
        try {
            this.address = DEFAULT_ADDRESS;
            this.device = PeripheralManager.getInstance().openI2cDevice(bus, DEFAULT_ADDRESS);
            defaultInitialization();
        } catch (IOException e) {
            Log.e(LOG_TAG, "MCP23017 cannot be created.", e);
        }
    }

    public MCP23017(String bus, int address) {
        try {
            this.address = address;
            this.device = PeripheralManager.getInstance().openI2cDevice(bus, address);
            defaultInitialization();
        } catch (IOException e) {
            Log.e(LOG_TAG, "MCP23017 cannot be created.", e);
        }
    }

    private void defaultInitialization() throws IOException {
        device.writeRegByte(Registers.REGISTER_IODIR_A, directionA);
        device.writeRegByte(Registers.REGISTER_IODIR_B, directionB);
        device.writeRegByte(Registers.REGISTER_IPOL_A, activationA);
        device.writeRegByte(Registers.REGISTER_IPOL_B, activationB);
        device.writeRegByte(Registers.REGISTER_GPIO_A, gpioA);
        device.writeRegByte(Registers.REGISTER_GPIO_B, gpioB);
    }

    public Gpio openGpio(MCP23017GPIO gpio) {
        return new MCP23017PinImpl(gpio.getName(), gpio.getAddress(), gpio.getRegister(), this);
    }

    void setValue(MCP23017Pin pin, boolean value) throws IOException {
        byte state = device.readRegByte(pin.getRegister());
        if (value) {
            state |= pin.getAddress();
        } else {
            state &= ~pin.getAddress();
        }
        device.writeRegByte(pin.getRegister(), state);
    }

    boolean getValue(MCP23017Pin pin) throws IOException {
        byte state = device.readRegByte(pin.getRegister());
        return (state & pin.getAddress()) == pin.getAddress();
    }

    public int getAddress() {
        return address;
    }

    public void close() throws IOException {
        if (device != null) {
            device.close();
        }
    }
}
