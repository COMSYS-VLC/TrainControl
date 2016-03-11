package de.rwthaachen.comsys.laboratory.vlc.traincontrol;

/**
 * Interface used to delegate received BLE data to the ControlFragment.
 */
interface ReceivingFragment {
    void handlePayload(byte[] data);
}
