package com.google.android.things.contrib.driver.mcp23017;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class MCP23017 {

    private static final String LOG_TAG = MCP23017.class.getSimpleName();
    private static final byte DEFAULT_REGISTER_VALUE = 0;
    private static final int DEFAULT_ADDRESS = 0x20;

    private static final int IODIR_A = 0x00;
    private static final int IPOL_A = 0x02;
    private static final int GPINTEN_A = 0x04;
    private static final int DEFVAL_A = 0x06;
    private static final int INTCON_A = 0x08;
    private static final int GPPU_A = 0x0C;
    private static final int INTF_A = 0x0E;
    private static final int GPIO_A = 0x12;
    private static final int IODIR_B = 0x01;
    private static final int IPOL_B = 0x03;
    private static final int GPINTEN_B = 0x05;
    private static final int DEFVAL_B = 0x07;
    private static final int INTCON_B = 0x09;
    private static final int GPPU_B = 0x0D;
    private static final int INTF_B = 0x0F;
    private static final int GPIO_B = 0x13;

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
        device.writeRegByte(IODIR_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IODIR_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IPOL_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(IPOL_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPPU_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPPU_B, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPIO_A, DEFAULT_REGISTER_VALUE);
        device.writeRegByte(GPPU_B, DEFAULT_REGISTER_VALUE);
    }

    public Gpio openGpio(MCP23017GPIO gpio) {
        return new MCP23017PinImpl(gpio.getName(), gpio.getAddress(), gpio.getRegister(), this);
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

    public int getAddress() {
        return address;
    }

    public void close() throws IOException {
        if (device != null) {
            device.close();
        }
    }
}
