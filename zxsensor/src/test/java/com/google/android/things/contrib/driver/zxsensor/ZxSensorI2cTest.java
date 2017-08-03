package com.google.android.things.contrib.driver.zxsensor;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioDriver;
import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class ZxSensorI2cTest {

    @Mock
    private I2cDevice mockDevice;
    @Mock
    private GpioDriver mockDataNotify;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void dataNotifyPinIsActiveHigh() throws Exception {
        new ZxSensorI2c("i2c", "gpio", mockDevice, mockDataNotify);

        verify(mockDataNotify).setActiveType(Gpio.ACTIVE_HIGH);
    }

    @Test
    public void dataNotifyPinIsAnInput() throws Exception {
        new ZxSensorI2c("i2c", "gpio", mockDevice, mockDataNotify);

        verify(mockDataNotify).setDirection(Gpio.DIRECTION_IN);
    }

    @Test
    public void dataNotifyPinTriggersOnTheLeadingEdge() throws Exception {
        new ZxSensorI2c("i2c", "gpio", mockDevice, mockDataNotify);

        verify(mockDataNotify).setEdgeTriggerType(Gpio.EDGE_RISING);
    }
}
