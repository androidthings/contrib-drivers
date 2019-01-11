package com.google.android.things.contrib.driver.mcp23017;

class Registers {

    static final int REGISTER_IODIR_A = 0x00;
    static final int REGISTER_IODIR_B = 0x01;
    static final int REGISTER_IPOL_A = 0x02;
    static final int REGISTER_IPOL_B = 0x03;
    static final int REGISTER_GPINTEN_A = 0x04;
    static final int REGISTER_GPINTEN_B = 0x05;
    static final int REGISTER_DEFVAL_A = 0x06;
    static final int REGISTER_DEFVAL_B = 0x07;
    static final int REGISTER_INTCON_A = 0x08;
    static final int REGISTER_INTCON_B = 0x09;
    static final int REGISTER_GPPU_A = 0x0C;
    static final int REGISTER_GPPU_B = 0x0D;
    static final int REGISTER_INTF_A = 0x0E;
    static final int REGISTER_INTF_B = 0x0F;
    static final int REGISTER_GPIO_A = 0x12;
    static final int REGISTER_GPIO_B = 0x13;
}
