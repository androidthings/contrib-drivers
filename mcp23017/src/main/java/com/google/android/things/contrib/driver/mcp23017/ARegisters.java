package com.google.android.things.contrib.driver.mcp23017;

public class ARegisters implements Registers {

    private static final int IODIR_A = 0x00;
    private static final int IPOL_A = 0x02;
    private static final int GPINTEN_A = 0x04;
    private static final int DEFVAL_A = 0x06;
    private static final int INTCON_A = 0x08;
    private static final int GPPU_A = 0x0C;
    private static final int INTF_A = 0x0E;
    private static final int GPIO_A = 0x12;

    @Override
    public int getIODIR() {
        return IODIR_A;
    }

    @Override
    public int getIPOL() {
        return IPOL_A;
    }

    @Override
    public int getGRIPTEN() {
        return GPINTEN_A;
    }

    @Override
    public int getDEFVAL() {
        return DEFVAL_A;
    }

    @Override
    public int getINTCON() {
        return INTCON_A;
    }

    @Override
    public int getGPPU() {
        return GPPU_A;
    }

    @Override
    public int getINTF() {
        return INTF_A;
    }

    @Override
    public int getGPIO() {
        return GPIO_A;
    }
}
