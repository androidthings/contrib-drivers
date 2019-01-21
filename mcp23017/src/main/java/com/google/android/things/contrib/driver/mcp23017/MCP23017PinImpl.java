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
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.things.pio.GpioCallback;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/* package */ class MCP23017PinImpl implements MCP23017Pin {

    private static final String LOG_TAG = MCP23017PinImpl.class.getSimpleName();

    private final String name;
    private final int address;
    private final Registers register;
    private final MCP23017 provider;
    private final Map<GpioCallback, Handler> callbackToHandler;
    private final Handler handler;

    /* package */ MCP23017PinImpl(String name, int address, Registers register, MCP23017 provider) {
        this(name, address, register, provider,
                new ConcurrentHashMap<>(), new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    /* package */  MCP23017PinImpl(String name, int address, Registers register, MCP23017 provider,
                                   Map<GpioCallback, Handler> callbackToHandler, Handler handler) {

        this.name = name;
        this.address = address;
        this.register = register;
        this.provider = provider;
        this.callbackToHandler = callbackToHandler;
        this.handler = handler;
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
        callbackToHandler.clear();
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
        if (callbackToHandler.containsKey(callback)) {
            Log.w(LOG_TAG, "Ignoring callback re-registration on " + name);
            return;
        }
        if (handler == null) {
            handler = this.handler;
        }
        callbackToHandler.put(callback, handler);
    }

    @Override
    public void unregisterGpioCallback(GpioCallback callback) {
        callbackToHandler.remove(callback);
    }

    @Override
    public void executeCallbacks() {
        callbackToHandler.keySet().forEach(this::executeCallback);
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

    private void executeCallback(GpioCallback callback) {
        Optional.ofNullable(callbackToHandler.get(callback))
                .ifPresent(handler -> sendCallbackToHandler(handler, callback));
    }

    private void sendCallbackToHandler(Handler handler, GpioCallback callback) {
        handler.post(() -> {
            if (!callback.onGpioEdge(this)) {
                callbackToHandler.remove(callback);
            }
        });
    }
}
