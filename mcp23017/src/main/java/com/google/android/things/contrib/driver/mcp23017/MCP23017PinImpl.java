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
import android.os.Looper;

import com.google.android.things.pio.GpioCallback;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class MCP23017PinImpl implements MCP23017Pin {

    private final String name;
    private final int address;
    private final Registers register;
    private final MCP23017 provider;
    private final Map<GpioCallback, CallbackListener> listenerMap;
    private final Handler handler;

    MCP23017PinImpl(String name, int address, Registers register, MCP23017 provider) {
        this.name = name;
        this.address = address;
        this.register = register;
        this.provider = provider;
        this.handler = new Handler(Looper.getMainLooper());
        this.listenerMap = new ConcurrentHashMap<>();
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
        listenerMap.values().forEach(CallbackListener::shutdown);
        listenerMap.clear();
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
        registerGpioCallback(null, callback);
    }

    @Override
    public void registerGpioCallback(Handler handler, GpioCallback callback) throws IOException {
        if (handler == null) {
            handler = this.handler;
        }
        CallbackListener listener = new CallbackListener(handler, this, callback);
        listenerMap.put(callback, listener);
        handler.post(listener);
    }

    @Override
    public void unregisterGpioCallback(GpioCallback callback) {
        CallbackListener listener = listenerMap.remove(callback);
        listener.shutdown();
    }

    private class CallbackListener implements Runnable {

        private Handler handler;
        private MCP23017Pin pin;
        private GpioCallback callback;
        private boolean shutdown = false;

        private CallbackListener(Handler handler, MCP23017Pin pin, GpioCallback callback) {
            this.handler = handler;
            this.pin = pin;
            this.callback = callback;
        }

        @Override
        public void run() {
            if (!shutdown) {
                try {
                    handleInterruption();
                } catch (IOException e) {
                    callback.onGpioError(pin, 0);
                }
            }
        }

        private void handleInterruption() throws IOException {
            if (provider.isInterrupted(pin)) {
                if (callback.onGpioEdge(pin)) {
                    handler.postDelayed(this, provider.getPollingTimeout());
                }
            } else {
                handler.postDelayed(this, provider.getPollingTimeout());
            }
        }

        void shutdown() {
            shutdown = true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCP23017PinImpl that = (MCP23017PinImpl) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
