package com.example.michael.traincontrol;

/**
 * Created by Michael on 07.03.2016.
 * Calculate CRC-8 checksum.
 */
public class CRC8 {
    private static final byte POLYNOMIAL = 0x07;
    private byte mCrc = 0;

    public byte getValue() {
        return mCrc;
    }

    public void reset() {
        mCrc = 0;
    }

    /**
     *
     * @param buf The input buffer.
     * @param off The offset.
     * @param nbytes The length.
     */
    public void update(byte[] buf, int off, int nbytes) {
        for (int i = 0; i < nbytes; i++) {
            update(buf[off + i]);
        }
    }

    public void update(byte val) {
        mCrc ^= val;
        for (int i = 0; i < 8; ++i) {
            if (0 != (mCrc & 0x80)) {
                mCrc <<= 1;
                mCrc ^= POLYNOMIAL;
            } else {
                mCrc <<= 1;
            }
        }
    }
}
