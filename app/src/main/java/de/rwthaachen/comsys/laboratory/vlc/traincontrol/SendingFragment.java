package de.rwthaachen.comsys.laboratory.vlc.traincontrol;

/**
 * Interface used to send user input to the BLE connection.
 */
interface SendingFragment {
    void sendPayload(byte[] data);
}
