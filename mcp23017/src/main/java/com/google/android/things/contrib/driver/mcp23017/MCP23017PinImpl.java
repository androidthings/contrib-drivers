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

import android.os.Handler;

import com.google.android.things.pio.GpioCallback;

import java.io.IOException;

class MCP23017PinImpl implements MCP23017Pin {

    private final String name;
    private final int address;
    private final Registers register;
    private final MCP23017 provider;

    public MCP23017PinImpl(String name, int address, Registers register, MCP23017 provider) {
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
    public Registers getRegisters() {
        return register;
    }

    @Override
    public void close() throws IOException {
        //not implemented
    }

    @Override
    public void setDirection(int direction) throws IOException {
        provider.setDirection(this, direction);
    }

    @Override
    public void setEdgeTriggerType(int triggerType) throws IOException {
        provider.setEdgeTriggerType(this, triggerType);
    }

    @Override
    public void setActiveType(int activeType) throws IOException {
        provider.setActiveType(this, activeType);
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
