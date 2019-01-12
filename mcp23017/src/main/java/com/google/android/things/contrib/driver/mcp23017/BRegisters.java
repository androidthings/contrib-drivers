package com.google.android.things.contrib.driver.mcp23017;

public class BRegisters implements Registers {

    private static final int IODIR_B = 0x01;
    private static final int IPOL_B = 0x03;
    private static final int GPINTEN_B = 0x05;
    private static final int DEFVAL_B = 0x07;
    private static final int INTCON_B = 0x09;
    private static final int GPPU_B = 0x0D;
    private static final int INTF_B = 0x0F;
    private static final int GPIO_B = 0x13;

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
