package com.google.brillo.driver.tm1637;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NumericDisplay extends Tm1637 {
    private ByteBuffer mBuffer = ByteBuffer.allocate(4);
    private boolean mColonEnabled = false;

    /**
     * Create a new driver for a TM1637 numeric display connected on the given GPIO pins.
     * @throws IOException
     */
    public NumericDisplay(String dataPin, String clockPin) throws IOException {
        super(dataPin, clockPin);
    }

    /**
     * Clear the display memory.
     */
    public void clear() throws IOException {
        writeData(new byte[]{0x00, 0x00, 0x00, 0x00});
    }

    /**
     * Enable or disable colon display after second digit of subsequently displayed value.
     */
    public void setColonEnabled(boolean b) {
        mColonEnabled = b;
    }

    /**
     * Return if colon display is enabled after second digit.
     */
    public boolean getColonEnabled() {
        return mColonEnabled;
    }

    /**
     * Display a integer number.
     * @param n number value
     */
    public void display(int n) throws IOException {
        display(String.format("%4s", n));
    }

    /**
     * Display a string number.
     * @param s string value
     */
    public void display(String s) throws IOException {
        mBuffer.clear();
        for (char c : s.toCharArray()) {
            // truncate string to the size of the display
            if (mBuffer.position() == mBuffer.limit()) {
                break;
            }
            if (c == ' ') {
                mBuffer.put((byte) 0);
            } else if (c >= '0' && c <= '9') {
                // extract character data from font.
                mBuffer.put(Font.DATA[c-'0']);
            } else {
                throw new IllegalArgumentException("unsupported digit character: " + c);
            }
        }
        if (mColonEnabled) {
            mBuffer.put(1, (byte)((mBuffer.get(1)&0xff)|Font.COLON));
        }
        mBuffer.flip();
        writeData(mBuffer.array());
    }
}
