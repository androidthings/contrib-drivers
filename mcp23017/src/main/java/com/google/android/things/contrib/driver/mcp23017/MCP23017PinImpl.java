package com.google.android.things.contrib.driver.mcp23017;

import android.os.Handler;

import com.google.android.things.pio.GpioCallback;

import java.io.IOException;

public class MCP23017PinImpl implements MCP23017Pin {

    private final String name;
    private final int address;
    private final int register;
    private final MCP23017 provider;

    public MCP23017PinImpl(String name, int address, int register, MCP23017 provider) {
        this.name = name;
        this.address = address;
        this.register = register;
        this.provider = provider;
    }

    @Override
    public String getName() {
        return provider.getAddress() + " " + name;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getRegister() {
        return register;
    }

    @Override
    public void close() throws IOException {
        //not implemented
    }

    @Override
    public void setDirection(int direction) throws IOException {

    }

    @Override
    public void setEdgeTriggerType(int i) throws IOException {

    }

    @Override
    public void setActiveType(int i) throws IOException {

    }

    @Override
    public void setValue(boolean value) throws IOException {
        provider.setValue(this, value);
    }

    @Override
    public boolean getValue() throws IOException {
        return provider.getValue(this);
    }

    @Override
    public void registerGpioCallback(GpioCallback callback) throws IOException {

    }

    @Override
    public void registerGpioCallback(Handler handler, GpioCallback gpioCallback) throws IOException {

    }

    @Override
    public void unregisterGpioCallback(GpioCallback gpioCallback) {

    }
}
