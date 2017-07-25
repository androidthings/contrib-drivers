package com.google.android.things.contrib.driver.zxsensor;

import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class ZxSensorUartTest {

    @Mock
    private UartDevice mockDevice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void startMonitor_registersWithBus() throws Exception {
        ZxSensorUart sensor = new ZxSensorUart("any", mockDevice);

        sensor.startMonitoringGestures();

        verify(mockDevice).registerUartDeviceCallback(any(UartDeviceCallback.class));
    }
}
