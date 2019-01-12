package com.google.android.things.contrib.driver.mcp23017;

public class BRegisters implements Registers {

    static final int IODIR_B = 0x01;
    static final int IPOL_B = 0x03;
    static final int GPINTEN_B = 0x05;
    static final int DEFVAL_B = 0x07;
    static final int INTCON_B = 0x09;
    static final int GPPU_B = 0x0D;
    static final int INTF_B = 0x0F;
    static final int GPIO_B = 0x13;

    @Override
    public int getIODIR() {
        return IODIR_B;
    }

    @Override
    public int getIPOL() {
        return IPOL_B;
    }

    @Override
    public int getGRIPTEN() {
        return GPINTEN_B;
    }

    @Override
    public int getDEFVAL() {
        return DEFVAL_B;
    }

    @Override
    public int getINTCON() {
        return INTCON_B;
    }

    @Override
    public int getGPPU() {
        return GPPU_B;
    }

    @Override
    public int getINTF() {
        return INTF_B;
    }

    @Override
    public int getGPIO() {
        return GPIO_B;
    }
}
