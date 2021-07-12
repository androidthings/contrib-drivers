package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Bitmap;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

class InkyPhatV1 implements InkyPhat {

    private static final boolean SPI_COMMAND = false;
    private static final boolean SPI_DATA = true;

    private static final byte PANEL_SETTING = (byte) 0x00;
    private static final byte POWER_SETTING = (byte) 0x01;
    private static final byte POWER_OFF = (byte) 0x02;
    private static final byte POWER_ON = (byte) 0x04;
    private static final byte BOOSTER_SOFT_START = (byte) 0x06;
    private static final byte DATA_START_TRANSMISSION_1 = (byte) 0x10;
    private static final byte DATA_START_TRANSMISSION_2 = (byte) 0x13;
    private static final byte DISPLAY_REFRESH = (byte) 0x12;
    private static final byte OSCILLATOR_CONTROL = (byte) 0x30;
    private static final byte VCOM_DATA_INTERVAL_SETTING = (byte) 0x50;
    private static final byte RESOLUTION_SETTING = (byte) 0x61;
    private static final byte VCOM_DC_SETTING = (byte) 0x82;
    private static final byte PARTIAL_EXIT = (byte) 0x92;

    private static final byte BORDER_WHITE = (byte) 0b10000000;
    private static final byte BORDER_BLACK = (byte) 0b11000000;
    private static final byte BORDER_RED = (byte) 0b01000000;

    private final SpiDevice spiBus;
    private final Gpio chipBusyPin;
    private final Gpio chipResetPin;
    private final Gpio chipCommandPin;
    private final PixelBuffer pixelBuffer;
    private final ImageConverter imageConverter;
    private final ColorConverter colorConverter;

    private byte border = BORDER_WHITE;

    InkyPhatV1(SpiDevice spiBus,
               Gpio chipBusyPin, Gpio chipResetPin, Gpio chipCommandPin,
               PixelBuffer pixelBuffer,
               ImageConverter imageConverter, ColorConverter colorConverter) {
        this.spiBus = spiBus;
        this.chipBusyPin = chipBusyPin;
        this.chipResetPin = chipResetPin;
        this.chipCommandPin = chipCommandPin;
        this.pixelBuffer = pixelBuffer;
        this.imageConverter = imageConverter;
        this.colorConverter = colorConverter;
        init();
    }

    private void init() {
        try {
            spiBus.setMode(SpiDevice.MODE0);
            chipCommandPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            chipCommandPin.setActiveType(Gpio.ACTIVE_HIGH);
            chipResetPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            chipResetPin.setActiveType(Gpio.ACTIVE_HIGH);
            chipBusyPin.setDirection(Gpio.DIRECTION_IN);
            chipBusyPin.setActiveType(Gpio.ACTIVE_HIGH);
        } catch (IOException e) {
            throw new IllegalStateException("InkyPhat cannot be configured.", e);
        }
    }

    @Override
    public void setImage(int x, int y, Bitmap image, Scale scale) {
        pixelBuffer.setImage(x, y, imageConverter.convertImage(image, scale));
    }

    @Override
    public void setText(int x, int y, String text, int color) {
        pixelBuffer.setImage(x, y, imageConverter.convertText(text, color));
    }

    @Override
    public void setPixel(int x, int y, int color) {
        pixelBuffer.setPixel(x, y, colorConverter.convertARBG888Color(color));
    }

    @Override
    public void setBorder(Palette color) {
        switch (color) {
            case BLACK:
                border = BORDER_BLACK;
                break;
            case RED:
                border = BORDER_RED;
                break;
            case WHITE:
                border = BORDER_WHITE;
                break;
            default:
                throw new IllegalStateException(color + " is unsupported as a border");
        }
    }

    @Override
    public void refresh() {
        try {
            turnDisplayOn();
            update();
            turnDisplayOff();
        } catch (IOException e) {
            throw new IllegalStateException("cannot init", e);
        }
    }

    private void turnDisplayOn() throws IOException {
        busyWait();

        sendCommand(POWER_SETTING, new byte[]{0x07, 0x00, 0x0A, 0x00});
        sendCommand(BOOSTER_SOFT_START, new byte[]{0x07, 0x07, 0x07});
        sendCommand(POWER_ON);

        busyWait();

        sendCommand(PANEL_SETTING, new byte[]{(byte) 0b11001111});
        sendCommand(VCOM_DATA_INTERVAL_SETTING, new byte[]{(byte) (0b00000111 | border)});

        sendCommand(OSCILLATOR_CONTROL, new byte[]{0x29});
        sendCommand(RESOLUTION_SETTING, new byte[]{0x68, 0x00, (byte) 0xD4});
        sendCommand(VCOM_DC_SETTING, new byte[]{0x0A});

        sendCommand(PARTIAL_EXIT);
    }

    private void update() throws IOException {
        sendCommand(DATA_START_TRANSMISSION_1, pixelBuffer.getDisplayPixelsForColor(Palette.BLACK));
        sendCommand(DATA_START_TRANSMISSION_2, pixelBuffer.getDisplayPixelsForColor(Palette.RED));
        sendCommand(DISPLAY_REFRESH);
    }

    private void turnDisplayOff() throws IOException {
        busyWait();

        sendCommand(VCOM_DATA_INTERVAL_SETTING, new byte[]{0x00});
        sendCommand(POWER_SETTING, new byte[]{0x02, 0x00, 0x00, 0x00});
        sendCommand(POWER_OFF);
    }

    private void sendCommand(byte command) throws IOException {
        chipCommandPin.setValue(SPI_COMMAND);
        byte[] buffer = new byte[]{command};
        spiBus.write(buffer, buffer.length);
    }

    private void sendCommand(byte command, byte[] data) throws IOException {
        sendCommand(command);
        sendData(data);
    }

    private void sendData(byte[] data) throws IOException {
        chipCommandPin.setValue(SPI_DATA);
        spiBus.write(data, data.length);
    }

    /**
     * Wait for the e-paper driver to be ready to receive commands/data.
     *
     * @throws IOException error accessing GPIO
     */
    private void busyWait() throws IOException {
        while (true) {
            if (chipBusyPin.getValue()) {
                break;
            }
        }
    }

    @Override
    public void close() {
        try {
            spiBus.close();
            chipBusyPin.close();
            chipResetPin.close();
            chipCommandPin.close();
        } catch (IOException e) {
            throw new IllegalStateException("InkyPhat connection cannot be closed, you may experience errors on next launch.", e);
        }
    }
}
