package com.google.android.things.contrib.driver.mcp23017;

import android.os.Handler;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MCP23017PinImplTest {

    private MCP23017PinImpl pin;

    private MCP23017 mcp23017;
    private Handler handler;
    private Handler customHandler;
    private Map<GpioCallback, Handler> handlerMap;
    private GpioCallback callback;

    @Before
    public void setup() {
        callback = mock(GpioCallback.class);
        handlerMap = mock(Map.class);
        mcp23017 = mock(MCP23017.class);
        handler = mock(Handler.class);
        customHandler = mock(Handler.class);
        pin = new MCP23017PinImpl("GPA0", 1,
                new ARegisters(), mcp23017, handlerMap, handler);
    }

    @Test
    public void testClose() throws Exception {
        pin.close();

        verify(handlerMap).clear();
    }

    @Test
    public void testSetDirection() throws Exception {
        pin.setDirection(Gpio.DIRECTION_IN);

        verify(mcp23017).setDirection(pin, Gpio.DIRECTION_IN);
    }

    @Test
    public void testSetEdgeTriggerType() throws Exception {
        pin.setEdgeTriggerType(Gpio.EDGE_BOTH);

        verify(mcp23017).setEdgeTriggerType(pin, Gpio.EDGE_BOTH);
    }

    @Test
    public void testSetActiveType() throws Exception {
        pin.setActiveType(Gpio.ACTIVE_LOW);

        verify(mcp23017).setActiveType(pin, Gpio.ACTIVE_LOW);
    }

    @Test
    public void testSetValue() throws Exception {
        pin.setValue(true);

        verify(mcp23017).setValue(pin, true);
    }

    @Test
    public void testGetValue() throws Exception {
        when(mcp23017.getValue(pin)).thenReturn(true);

        assertTrue(pin.getValue());

        verify(mcp23017).getValue(pin);
    }

    @Test
    public void testRegisterGpioCallback() throws Exception {
        when(handlerMap.containsKey(callback)).thenReturn(false);

        pin.registerGpioCallback(callback);

        verify(handlerMap).put(callback, handler);
    }

    @Test
    public void testRegisterGpioCallbackWithHandler() throws Exception {
        when(handlerMap.containsKey(callback)).thenReturn(false);

        pin.registerGpioCallback(customHandler, callback);

        verify(handlerMap).put(callback, customHandler);
    }

    @Test
    public void testUnregisterGpioCallback() throws Exception {
        pin.unregisterGpioCallback(callback);

        verify(handlerMap).remove(callback);
    }

    @Test
    public void testExecuteCallbacks() throws Exception {
        when(handlerMap.keySet()).thenReturn(Collections.singleton(callback));
        when(handlerMap.get(callback)).thenReturn(handler);

        pin.executeCallbacks();

        verify(handler).post(any(Runnable.class));
    }
}