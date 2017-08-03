package com.google.android.things.contrib.driver.zxsensor;

import com.google.android.things.pio.UartDevice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class ZxSensorUartTest {

    @Mock
    private UartDevice mockDevice;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void baudRateMatchesDatasheet() throws Exception {
        new ZxSensorUart("", mockDevice);

        verify(mockDevice).setBaudrate(115200);
    }

    @Test
    public void commsDataSizeMatchesDatasheet() throws Exception {
        new ZxSensorUart("", mockDevice);

        verify(mockDevice).setDataSize(8);
    }

    @Test
    public void parityBitMatchesDatasheet() throws Exception {
        new ZxSensorUart("", mockDevice);

        verify(mockDevice).setParity(UartDevice.PARITY_NONE);
    }

    @Test
    public void stopBitMatchesDatasheet() throws Exception {
        new ZxSensorUart("", mockDevice);

        verify(mockDevice).setStopBits(1);
    }

    @Test
    public void deviceIsClosedWhenClosing() throws Exception {
        ZxSensorUart uart = new ZxSensorUart("", mockDevice);

        uart.close();

        verify(mockDevice).close();
    }
}
