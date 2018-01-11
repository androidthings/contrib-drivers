package com.google.android.things.contrib.driver.inkyphat;

import android.graphics.Bitmap;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

class InkyPhatV2 implements InkyPhat {

    private static final boolean SPI_COMMAND = false;
    private static final boolean SPI_DATA = true;

    private static final byte RESET = (byte) 0x12;
    private static final byte DRIVER_OUTPUT_CONTROL = (byte) 0x01;
    private static final byte DUMMY_LINE_PERIOD = (byte) 0x03;
    private static final byte GATE_LINE_WIDTH = (byte) 0x3b;
    private static final byte DATA_ENTRY_MODE = (byte) 0x11;
    private static final byte RAM_X_ADDRESS = (byte) 0x44;
    private static final byte RAM_Y_ADDRESS = (byte) 0x45;
    private static final byte RAM_X_COUNTER = (byte) 0x4e;
    private static final byte RAM_Y_COUNTER = (byte) 0x4f;
    private static final byte DATA_START_TRANSMISSION_1 = (byte) 0x24;
    private static final byte DATA_START_TRANSMISSION_2 = (byte) 0x26;
    private static final byte DISPLAY_UPDATE_SETTING = (byte) 0x22;
    private static final byte DISPLAY_UPDATE_ACTIVATE = (byte) 0x20;
    private static final byte SEND_LUTS = (byte) 0x32;
    private static final byte BORDER_CONTROL = (byte) 0x3c;
    private static final byte SOURCE_DRIVING_VOLTAGE = (byte) 0x04;
    private static final byte VCOM_REGISTER = (byte) 0x2c;

    private static final byte BORDER_WHITE = (byte) 0xFF;
    private static final byte BORDER_BLACK = (byte) 0x00;
    private static final byte BORDER_RED = (byte) 0x33;

    private final SpiDevice spiBus;
    private final Gpio chipBusyPin;
    private final Gpio chipResetPin;
    private final Gpio chipCommandPin;
    private final PixelBuffer pixelBuffer;
    private final ImageConverter imageConverter;
    private final ColorConverter colorConverter;

    private byte border = BORDER_WHITE;

    InkyPhatV2(SpiDevice spiBus,
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
            chipBusyPin.setActiveType(Gpio.ACTIVE_LOW);
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
        } catch (IOException e) {
            throw new IllegalStateException("cannot init", e);
        }
    }

    private void turnDisplayOn() throws IOException {
        sendCommand(RESET);
        busyWait();

        sendData(new byte[]{0x74, 0x54}); // Set analog control block
        sendData(new byte[]{0x75, 0x3b}); // Sent by dev board but undocumented in datasheet

        // Driver output control
        sendCommand(DRIVER_OUTPUT_CONTROL, new byte[]{(byte) 0xd3, 0x00, 0x00});
        // Dummy line period Default value:0b---- - 011 See page 22 of datasheet
        sendCommand(DUMMY_LINE_PERIOD, new byte[]{0x07});
        // Gate line width
        sendCommand(GATE_LINE_WIDTH, new byte[]{0x04});
        // Data entry mode
        sendCommand(DATA_ENTRY_MODE, new byte[]{0x03});
    }

    private void update() throws IOException {
        sendCommand(RAM_X_ADDRESS, new byte[]{0x00, 0x0c});
        sendCommand(RAM_Y_ADDRESS, new byte[]{0x00, 0x00, (byte) 0xD3, 0x00, 0x00}); // + erroneous extra byte?
        sendCommand(SOURCE_DRIVING_VOLTAGE, new byte[]{0x2d, (byte) 0xb2, 0x22});
        sendCommand(VCOM_REGISTER, new byte[]{0x3c});         // 0x3c = -1.5v?

        sendCommand(BORDER_CONTROL, new byte[]{border});

        sendCommand(SEND_LUTS, new byte[]{
        /*Phase 0 Phase 1 Phase 2 Phase 3 Phase 4 Phase 5 Phase 6*/
        /*A B C D A B C D A B C D A B C D A B C D A B C D A B C D*/
                0b01001000, (byte) 0b10100000, 0b00010000, 0b00010000, 0b00010011, 0b00000000, 0b00000000,/*0b00000000, #LUT0 - Black*/
                0b01001000, (byte) 0b10100000, (byte) 0b10000000, 0b00000000, 0b00000011, 0b00000000, 0b00000000,/*0b00000000, #LUTT1 - White*/
                0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,/*0b00000000, #IGNORE*/
                0b01001000, (byte) 0b10100101, 0b00000000, (byte) 0b10111011, 0b00000000, 0b00000000, 0b00000000,/*0b00000000, #LUT3 - Red*/
                0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000, 0b00000000,/*0b00000000, #LUT4 - VCOM*/
        /*0xA5, 0x89, 0x10, 0x10, 0x00, 0x00, 0x00, #LUT0 - Black
        #0xA5, 0x19, 0x80, 0x00, 0x00, 0x00, 0x00, #LUT1 - White
        #0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, #LUT2 - Red - NADA !
        #0xA5, 0xA9, 0x9B, 0x9B, 0x00, 0x00, 0x00, #LUT3 - Red
        #0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, #LUT4 - VCOM*/

/*Duration | Repeat*/
/*A B C D   |*/
                67, 10, 31, 10, 4,  /*0 Flash*/
                16, 8, 4, 4, 6,  /*1 clear*/
                4, 8, 8, 32, 16,  /*2 bring in the black*/
                4, 8, 8, 64, 32, /*3 time for red*/
                6, 6, 6, 2, 2,  /*4 final black sharpen phase*/
                0, 0, 0, 0, 0,  /*4*/
                0, 0, 0, 0, 0,  /*5*/
                0, 0, 0, 0, 0,  /*6*/
                0, 0, 0, 0, 0   /*7*/
        });

        sendCommand(RAM_X_ADDRESS, new byte[]{0x00, 0x0c});
        sendCommand(RAM_Y_ADDRESS, new byte[]{0x00, 0x00, (byte) 0xd3, 0x00});
        sendCommand(RAM_X_COUNTER, new byte[]{0x00});
        sendCommand(RAM_Y_COUNTER, new byte[]{0x00, 0x00});

        sendCommand(DATA_START_TRANSMISSION_1, pixelBuffer.getDisplayPixelsForColor(Palette.BLACK));

        sendCommand(RAM_X_ADDRESS, new byte[]{0x00, 0x0c});
        sendCommand(RAM_Y_ADDRESS, new byte[]{0x00, 0x00, (byte) 0xd3, 0x00});
        sendCommand(RAM_X_COUNTER, new byte[]{0x00});
        sendCommand(RAM_Y_COUNTER, new byte[]{0x00, 0x00});

        sendCommand(DATA_START_TRANSMISSION_2, pixelBuffer.getDisplayPixelsForColor(Palette.RED));

        sendCommand(DISPLAY_UPDATE_SETTING, new byte[]{(byte) 0xc7});
        sendCommand(DISPLAY_UPDATE_ACTIVATE);

        busyWait();
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
