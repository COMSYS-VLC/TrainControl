package com.example.michael.traincontrol;

import java.util.zip.Checksum;

/**
 * Created by Michael on 07.03.2016.
 * Calculate CRC-8 checksum.
 * With help from https://github.com/ggrandes/sandbox/blob/master/src/CRC8.java.
 */
public class CRC8 implements Checksum {
    private static final int POLYNOMIAL = 0xE0;
    private int crc = 0;

    @Override
    public long getValue() {
        return (crc & 0xFF);
    }

    @Override
    public void reset() {
        this.crc = 0;
    }

    /**
     *
     * @param buf The input buffer.
     * @param off The offset.
     * @param nbytes The length.
     */
    @Override
    public void update(byte[] buf, int off, int nbytes) {
        for (int i = 0; i < nbytes; i++) {
            update(buf[off + i]);
        }
    }

    @Override
    public void update(int val) {
        this.crc ^= val;
        for (int j = 0; j < 8; j++) {
            if ((this.crc & 0x80) != 0) {
                this.crc = ((this.crc << 1) ^ POLYNOMIAL);
            } else {
                this.crc <<= 1;
            }
        }
        this.crc &= 0xFF;
    }
}
