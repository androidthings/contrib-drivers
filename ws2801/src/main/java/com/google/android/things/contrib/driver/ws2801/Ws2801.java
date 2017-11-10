package com.google.android.things.contrib.driver.ws2801;

import android.graphics.Color;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

public class Ws2801 {
    private SpiDevice spi_device;
    private byte[] data;
    private int nb_leds;

    public Ws2801(int nbleds, String spi_bus_name) throws IOException {
        nb_leds = nbleds;
        data = new byte[3*nb_leds];
        for(int i = 0 ; i < nb_leds ; i++) {
            data[3*i + 0] = (byte)Color.red(Color.BLUE);
            data[3*i + 1] = (byte)Color.green(Color.BLUE);
            data[3*i + 2] = (byte)Color.blue(Color.BLUE);
        }
        PeripheralManagerService service = new PeripheralManagerService();
        spi_device = service.openSpiDevice(spi_bus_name);
        spi_device.setFrequency(1_000_000);
        spi_device.setMode(SpiDevice.MODE0);
        spi_device.setBitsPerWord(8);
        spi_device.setDelay(1000);
    }

    public void SetColor(int position, int red, int green, int blue)  throws IOException {
        SetColor(position, Color.rgb(red, green, blue));
    }

    public void SetColor(int position, int color) {
        if(position > nb_leds)
            return;

        data[3*position + 0] = (byte)Color.red(color);
        data[3*position + 1] = (byte)Color.green(color);
        data[3*position + 2] = (byte)Color.blue(color);
    }

    public void Show() throws IOException {
        spi_device.write(data, data.length);
    }

    public void Close() throws IOException {
        spi_device.close();
    }
}
