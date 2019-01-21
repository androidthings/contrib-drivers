/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.things.contrib.driver.mcp23017;

/* package */ class BRegisters implements Registers {

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
