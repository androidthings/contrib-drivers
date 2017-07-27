package com.google.android.things.contrib.driver.zxsensor;

import com.google.android.things.pio.GpioDriver;
import com.google.android.things.pio.I2cDevice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ZxSensorI2cTest {

    @Mock
    private I2cDevice mockDevice;
    @Mock
    private GpioDriver mockDriver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void name() throws Exception {
        new ZxSensorI2c("a", "b", mockDevice, mockDriver);

    }
}
