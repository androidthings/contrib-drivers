package com.google.android.things.contrib.driver.inkyphat;

/**
 * DO NOT AUTO-FORMAT THIS CLASS
 *
 * It is set out in a readable way (and the readable way doesn't agree with any autoformatting)
 */
class LUTS {

    static byte[] data() {
        return new byte[]{
/*      Phase 0            Phase 1            Phase 2            Phase 3     Phase 4     Phase 5     Phase 6    */
/*        A B C D            A B C D            A B C D            A B C D     A B C D     A B C D     A B C D  */
        0b01001000, (byte) 0b10100000,        0b00010000,        0b00010000, 0b00010011, 0b00000000, 0b00000000,/* 0b00000000, # LUT 0 - Black    */
        0b01001000, (byte) 0b10100000, (byte) 0b10000000,        0b00000000, 0b00000011, 0b00000000, 0b00000000,/* 0b00000000, # LUT 1 - White    */
        0b00000000,        0b00000000,        0b00000000,        0b00000000, 0b00000000, 0b00000000, 0b00000000,/* 0b00000000, # IGNORE           */
        0b01001000, (byte) 0b10100101,        0b00000000, (byte) 0b10111011, 0b00000000, 0b00000000, 0b00000000,/* 0b00000000, # LUT 3 - Red      */
        0b00000000,        0b00000000,        0b00000000,        0b00000000, 0b00000000, 0b00000000, 0b00000000,/* 0b00000000, # LUT 4 - VCOM     */
/*      0xA5, 0x89, 0x10, 0x10, 0x00, 0x00, 0x00, # LUT 0 - Black         */
/*      0xA5, 0x19, 0x80, 0x00, 0x00, 0x00, 0x00, # LUT 1 - White         */
/*      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, # LUT 2 - Red - NADA !  */
/*      0xA5, 0xA9, 0x9B, 0x9B, 0x00, 0x00, 0x00, # LUT 3 - Red           */
/*      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, # LUT 4 - VCOM          */

/*      Duration                    | Repeat    */
/*      A       B       C       D   |           */
        67,     10,     31,     10,     4,      /* 0 Flash                      */
        16,     8,      4,      4,      6,      /* 1 clear                      */
        4,      8,      8,      32,     16,     /* 2 bring in the black         */
        4,      8,      8,      64,     32,     /* 3 time for red               */
        6,      6,      6,      2,      2,      /* 4 final black sharpen phase  */
        0,      0,      0,      0,      0,      /* 4                            */
        0,      0,      0,      0,      0,      /* 5                            */
        0,      0,      0,      0,      0,      /* 6                            */
        0,      0,      0,      0,      0       /* 7                            */
        };
    }

}
