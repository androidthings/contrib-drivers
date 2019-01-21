package com.google.android.things.contrib.driver.mcp23017;

import android.os.Handler;
import android.os.Looper;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Looper.class})
public class MCP23017Test {

    private MCP23017 mcp23017;
    private I2cDevice i2cDevice;
    private Registers registers;
    private MCP23017Pin pin;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        i2cDevice = mock(I2cDevice.class);
        mcp23017 = new MCP23017(i2cDevice);
        registers = mock(Registers.class);
        pin = mock(MCP23017Pin.class);
        when(pin.getRegisters()).thenReturn(registers);
    }

    @Test
    public void testInitialization() throws Exception {
        doNothing().when(i2cDevice).writeRegByte(anyInt(), anyByte());

        verify(i2cDevice).writeRegByte(IODIR_A, (byte) 0);
        verify(i2cDevice).writeRegByte(IODIR_B, (byte) 0);
        verify(i2cDevice).writeRegByte(IPOL_A, (byte) 0);
        verify(i2cDevice).writeRegByte(IPOL_B, (byte) 0);
        verify(i2cDevice).writeRegByte(GPPU_A, (byte) 0);
        verify(i2cDevice).writeRegByte(GPPU_B, (byte) 0);
        verify(i2cDevice).writeRegByte(GPIO_A, (byte) 0);
        verify(i2cDevice).writeRegByte(GPIO_B, (byte) 0);
        verify(i2cDevice).writeRegByte(GPINTEN_A, (byte) 0);
        verify(i2cDevice).writeRegByte(GPINTEN_B, (byte) 0);
        verify(i2cDevice).writeRegByte(INTCON_A, (byte) 0);
        verify(i2cDevice).writeRegByte(INTCON_B, (byte) 0);
        verify(i2cDevice).writeRegByte(DEFVAL_A, (byte) 0);
        verify(i2cDevice).writeRegByte(DEFVAL_B, (byte) 0);
    }

    @Test
    public void testOpenGpio() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper mockMainThreadLooper = mock(Looper.class);
        when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);

        Gpio gpio = mcp23017.openGpio(MCP23017GPIO.A0);

        assertTrue(gpio.getName().contains(MCP23017GPIO.A0.getName()));
    }

    @Test
    public void testOpenGpioWhenGpioIsOpened() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper mockMainThreadLooper = mock(Looper.class);
        when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);

        thrown.expect(IOException.class);
        thrown.expectMessage(" is already in use");

        mcp23017.openGpio(MCP23017GPIO.A0);
        mcp23017.openGpio(MCP23017GPIO.A0);
    }

    @Test
    public void testClose() throws Exception {
        PowerMockito.mockStatic(Looper.class);
        Looper mockMainThreadLooper = mock(Looper.class);
        when(Looper.getMainLooper()).thenReturn(mockMainThreadLooper);
        mcp23017.openGpio(MCP23017GPIO.A0);

        mcp23017.close();

        verify(i2cDevice).close();
    }

    @Test
    public void testSetValueWhenValueIsTrue() throws Exception {
        int register = 0;
        when(i2cDevice.readRegByte(register)).thenReturn((byte) 0);
        when(registers.getGPIO()).thenReturn(register);
        when(pin.getAddress()).thenReturn(1);
        when(pin.getName()).thenReturn("GPA0");

        mcp23017.setValue(pin, true);

        verify(i2cDevice).writeRegByte(register, (byte) pin.getAddress());
    }

    @Test
    public void testSetValueWhenValueIsFalse() throws Exception {
        int register = 0;
        when(i2cDevice.readRegByte(register)).thenReturn((byte) 3);
        when(registers.getGPIO()).thenReturn(register);
        when(pin.getAddress()).thenReturn(2);
        when(pin.getName()).thenReturn("GPA0");

        mcp23017.setValue(pin, false);

        verify(i2cDevice).writeRegByte(register, (byte) 1);
    }

    @Test
    public void testGetValue() throws Exception {
        int register = 0;
        when(i2cDevice.readRegByte(register)).thenReturn((byte) 7);
        when(registers.getGPIO()).thenReturn(register);
        when(pin.getAddress()).thenReturn(2);

        assertTrue(mcp23017.getValue(pin));
    }

    @Test
    public void testSetDirectionWhenDirectionIsInput() throws Exception {
        when(registers.getIODIR()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(i2cDevice.readRegByte(registers.getIODIR())).thenReturn((byte) 0);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setDirection(pin, Gpio.DIRECTION_IN);

        verify(i2cDevice).writeRegByte(registers.getIODIR(), (byte) 1);
        verify(i2cDevice).writeRegByte(registers.getGPIO(), (byte) 0);
    }

    @Test
    public void testSetDirectionWhenDirectionIsLowOutput() throws Exception {
        when(registers.getIODIR()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(i2cDevice.readRegByte(registers.getIODIR())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setDirection(pin, Gpio.DIRECTION_OUT_INITIALLY_LOW);

        verify(i2cDevice).writeRegByte(registers.getIODIR(), (byte) 0);
        verify(i2cDevice).writeRegByte(registers.getGPIO(), (byte) 0);
    }

    @Test
    public void testSetDirectionWhenDirectionIsHighOutput() throws Exception {
        when(registers.getIODIR()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(i2cDevice.readRegByte(registers.getIODIR())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setDirection(pin, Gpio.DIRECTION_OUT_INITIALLY_HIGH);

        verify(i2cDevice).writeRegByte(registers.getIODIR(), (byte) 0);
        verify(i2cDevice).writeRegByte(registers.getGPIO(), (byte) 1);
    }

    @Test
    public void testSetDirectionWhenDirectionIsUnknown() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown direction");

        mcp23017.setDirection(pin, 100);
    }

    @Test
    public void testSetActiveTypeWhenActiveTypeIsHigh() throws Exception {
        when(registers.getIPOL()).thenReturn(1000);
        when(i2cDevice.readRegByte(registers.getIPOL())).thenReturn((byte) 1);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setActiveType(pin, Gpio.ACTIVE_HIGH);

        verify(i2cDevice).writeRegByte(registers.getIPOL(), (byte) 0);
    }

    @Test
    public void testSetActiveTypeWhenActiveTypeIsLow() throws Exception {
        when(registers.getIPOL()).thenReturn(1000);
        when(i2cDevice.readRegByte(registers.getIPOL())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setActiveType(pin, Gpio.ACTIVE_LOW);

        verify(i2cDevice).writeRegByte(registers.getIPOL(), (byte) 1);
    }

    @Test
    public void testSetActiveTypeWhenActiveTypeIsUnknown() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown active state");

        mcp23017.setActiveType(pin, 100);
    }

    @Test
    public void testSetEdgeTriggerTypeWhenTriggerTypeIsNone() throws Exception {
        when(registers.getGRIPTEN()).thenReturn(1000);
        when(i2cDevice.readRegByte(registers.getGRIPTEN())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setEdgeTriggerType(pin, Gpio.EDGE_FALLING);

        verify(i2cDevice).writeRegByte(registers.getGRIPTEN(), (byte) 1);
    }

    @Test
    public void testSetEdgeTriggerTypeWhenTriggerTypeIsEdgeFalling() throws Exception {
        when(registers.getGRIPTEN()).thenReturn(1000);
        when(registers.getDEFVAL()).thenReturn(1001);
        when(registers.getINTCON()).thenReturn(1002);
        when(i2cDevice.readRegByte(registers.getGRIPTEN())).thenReturn((byte) 0);
        when(i2cDevice.readRegByte(registers.getDEFVAL())).thenReturn((byte) 0);
        when(i2cDevice.readRegByte(registers.getINTCON())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setEdgeTriggerType(pin, Gpio.EDGE_FALLING);

        verify(i2cDevice).writeRegByte(registers.getGRIPTEN(), (byte) 1);
        verify(i2cDevice).writeRegByte(registers.getDEFVAL(), (byte) 1);
        verify(i2cDevice).writeRegByte(registers.getINTCON(), (byte) 1);
    }

    @Test
    public void testSetEdgeTriggerTypeWhenTriggerTypeIsEdgeRising() throws Exception {
        when(registers.getGRIPTEN()).thenReturn(1000);
        when(registers.getDEFVAL()).thenReturn(1001);
        when(registers.getINTCON()).thenReturn(1002);
        when(i2cDevice.readRegByte(registers.getGRIPTEN())).thenReturn((byte) 0);
        when(i2cDevice.readRegByte(registers.getDEFVAL())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getINTCON())).thenReturn((byte) 0);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setEdgeTriggerType(pin, Gpio.EDGE_RISING);

        verify(i2cDevice).writeRegByte(registers.getGRIPTEN(), (byte) 1);
        verify(i2cDevice).writeRegByte(registers.getDEFVAL(), (byte) 0);
        verify(i2cDevice).writeRegByte(registers.getINTCON(), (byte) 1);
    }

    @Test
    public void testSetEdgeTriggerTypeWhenTriggerTypeIsEdgeBoth() throws Exception {
        when(registers.getGRIPTEN()).thenReturn(1000);
        when(registers.getINTCON()).thenReturn(1002);
        when(i2cDevice.readRegByte(registers.getGRIPTEN())).thenReturn((byte) 0);
        when(i2cDevice.readRegByte(registers.getINTCON())).thenReturn((byte) 1);
        when(pin.getAddress()).thenReturn(1);

        mcp23017.setEdgeTriggerType(pin, Gpio.EDGE_BOTH);

        verify(i2cDevice).writeRegByte(registers.getGRIPTEN(), (byte) 1);
        verify(i2cDevice).writeRegByte(registers.getINTCON(), (byte) 0);
    }

    @Test
    public void testSetActiveTypeWhenTriggerTypeIsUnknown() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown trigger type");

        mcp23017.setEdgeTriggerType(pin, 100);
    }

    @Test
    public void testIsInterruptedWhenFalse() throws Exception {
        when(registers.getINTF()).thenReturn(1000);
        when(i2cDevice.readRegByte(registers.getINTF())).thenReturn((byte) 0);

        assertFalse(mcp23017.isInterrupted(pin));
    }

    @Test
    public void testIsInterruptedWhenInterruptedChannelA() throws Exception {
        when(registers.getINTF()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(pin.getName()).thenReturn("GPA0");
        when(pin.getAddress()).thenReturn(1);
        when(i2cDevice.readRegByte(registers.getINTF())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 1);

        assertTrue(mcp23017.isInterrupted(pin));
    }

    @Test
    public void testIsInterruptedWhenNotInterruptedChannelA() throws Exception {
        when(registers.getINTF()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(pin.getName()).thenReturn("GPA0");
        when(pin.getAddress()).thenReturn(1);
        when(i2cDevice.readRegByte(registers.getINTF())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 0);

        assertFalse(mcp23017.isInterrupted(pin));
    }

    @Test
    public void testIsInterruptedWhenInterruptedChannelB() throws Exception {
        when(registers.getINTF()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(pin.getName()).thenReturn("GPB0");
        when(pin.getAddress()).thenReturn(1);
        when(i2cDevice.readRegByte(registers.getINTF())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 1);

        assertTrue(mcp23017.isInterrupted(pin));
    }

    @Test
    public void testIsInterruptedWhenNotInterruptedChannelB() throws Exception {
        when(registers.getINTF()).thenReturn(1000);
        when(registers.getGPIO()).thenReturn(1001);
        when(pin.getName()).thenReturn("GPB0");
        when(pin.getAddress()).thenReturn(1);
        when(i2cDevice.readRegByte(registers.getINTF())).thenReturn((byte) 1);
        when(i2cDevice.readRegByte(registers.getGPIO())).thenReturn((byte) 0);

        assertFalse(mcp23017.isInterrupted(pin));
    }
}
