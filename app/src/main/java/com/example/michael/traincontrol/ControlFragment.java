package com.example.michael.traincontrol;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.TypedArrayUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class ControlFragment extends Fragment implements ConnectFragment.ConnectAbortHandler,
        ConnectErrorFragment.ConnectErrorHandler, ControlListFragment.IControlListFragmentEvent {

    private final CRC8 crc = new CRC8();
    List<Byte> mReadBuffer = new ArrayList<>();
    private static final String ARG_BLE_DEVICE_NAME = "devname";
    private static final String ARG_BLE_DEVICE_ADDR = "devaddr";
    private static final String ARG_BLE_SERVICE_UUID = "bleservice";
    private static final String ARG_BLE_RX_UUID = "blerx";
    private static final String ARG_BLE_TX_UUID = "bletx";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private String mBleDeviceName;
    private String mBleDeviceAddr;
    private String mBleServiceUuid;
    private String mBleRxCharacteristicUuid;
    private String mBleTxCharacteristicUuid;
    private BluetoothDevice mBleDevice;
    private BluetoothGatt mBleGatt;
    private BluetoothGattCharacteristic mBleRxCharacteristic;
    private BluetoothGattCharacteristic mBleTxCharacteristic;
    private BluetoothGattCallback mBleGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final Activity activity = getActivity();
            if(BluetoothGatt.GATT_SUCCESS == status) {
                if (BluetoothProfile.STATE_CONNECTED == newState) {
                    gatt.discoverServices();
                } else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
                    if (null != activity) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onDisconnected();
                            }
                        });
                    }
                }
            } else {
                if (null != activity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onConnectionFailed();
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final Activity activity = getActivity();
            if(BluetoothGatt.GATT_SUCCESS == status && null != activity) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onConnected();
                    }
                });
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(characteristic == mBleRxCharacteristic) {
                final Activity activity = getActivity();
                if(null != activity) {
                    byte[] value = characteristic.getValue();
                    final byte[] data = new byte[value.length];
                    System.arraycopy(value, 0, data, 0, value.length);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onReceived(data);
                        }
                    });
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(characteristic == mBleRxCharacteristic) {
                final Activity activity = getActivity();
                if(null != activity) {
                    byte[] value = characteristic.getValue();
                    final byte[] data = new byte[value.length];
                    System.arraycopy(value, 0, data, 0, value.length);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onReceived(data);
                        }
                    });
                }
            }
        }
    };
    public ControlFragment() {
        // Required empty public constructor
    }

    public static ControlFragment newInstance(String bleDeviceName, String bleDeviceAddr, String bleServiceUuid,
                                              String bleRxCharacteristicUuid, String bleTxCharacteristicUuid) {
        ControlFragment fragment = new ControlFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BLE_DEVICE_NAME, bleDeviceName);
        args.putString(ARG_BLE_DEVICE_ADDR, bleDeviceAddr);
        args.putString(ARG_BLE_SERVICE_UUID, bleServiceUuid);
        args.putString(ARG_BLE_RX_UUID, bleRxCharacteristicUuid);
        args.putString(ARG_BLE_TX_UUID, bleTxCharacteristicUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void retryConnect() {
        disconnect();
        connect();
    }

    @Override
    public void scanForOtherDevice() {
        if(getActivity() instanceof ScanInitiator) {
            ((ScanInitiator) getActivity()).startScan();
        }
    }

    private void onConnectionFailed() {
        disconnect();
        showConnectionError("Connection error");
    }

    private void onReceived(byte[] data) {
        // TODO: NOT TESTED!

        // Fill buffer.
        for (byte b : data) {
            mReadBuffer.add(b);
        }

        // Remove all elements until a header is found.
        while (mReadBuffer.size() > 0 && mReadBuffer.get(0) != (byte)0xFF) {
            mReadBuffer.remove(0);
        }

        // Check if readBuffer has the correct length & header.
        if (mReadBuffer.size() >= 4 && mReadBuffer.get(0) == (byte)0xFF) {
            // Check CRC checksum.
            byte[] subset = new byte[] {mReadBuffer.get(0),
                    mReadBuffer.get(1), mReadBuffer.get(2)}; // Only Header + ID + "Payload".
            this.crc.reset();
            this.crc.update(subset, 0, subset.length);
            if ((byte)this.crc.getValue() == mReadBuffer.get(3)) {
                // Extract packet from readBuffer.
                byte[] packet = new byte[] { mReadBuffer.get(1), mReadBuffer.get(2) };
                for (int i = 0; i <= 3; i++) {
                    mReadBuffer.remove(i);
                }
                // TODO: packet has to be further processed ...
            }
        }
    }

    private void send(byte[] data) {
        if(null != mBleGatt) {
            mBleTxCharacteristic.setValue(data);
            mBleGatt.writeCharacteristic(mBleTxCharacteristic);
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBleDeviceName = getArguments().getString(ARG_BLE_DEVICE_NAME);
            mBleDeviceAddr = getArguments().getString(ARG_BLE_DEVICE_ADDR);
            mBleServiceUuid = getArguments().getString(ARG_BLE_SERVICE_UUID);
            mBleRxCharacteristicUuid = getArguments().getString(ARG_BLE_RX_UUID);
            mBleTxCharacteristicUuid = getArguments().getString(ARG_BLE_TX_UUID);
        } else {
            throw new IllegalStateException(this + " needs to have the device addr argument set");
        }
    }

    @Override
    public void onPause() {
        disconnect();

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        connect();
    }

    private void connect() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        mBleDevice = bluetoothAdapter.getRemoteDevice(mBleDeviceAddr);
        mBleGatt = mBleDevice.connectGatt(getContext(), false, mBleGattCallback);

        showConnecting();
    }

    private void onConnected() {
        BluetoothGattService gattService = mBleGatt.getService(UUID.fromString(mBleServiceUuid));
        if(null == gattService) {
            disconnect();
            showConnectionError("Service not supported");
            return;
        }

        mBleRxCharacteristic = gattService.getCharacteristic(UUID.fromString(mBleRxCharacteristicUuid));
        mBleTxCharacteristic = gattService.getCharacteristic(UUID.fromString(mBleTxCharacteristicUuid));

        if(null == mBleTxCharacteristic || null == mBleRxCharacteristic) {
            disconnect();
            showConnectionError("Required characteristics missing");
            return;
        } else if((mBleTxCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0 ||
                (((mBleRxCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                        != (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY)))) {
            disconnect();
            showConnectionError("Required characteristics missing");
            return;
        }

        mBleGatt.setCharacteristicNotification(mBleRxCharacteristic, true);
        BluetoothGattDescriptor descriptor = mBleRxCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleGatt.writeDescriptor(descriptor);

        showControlList();
        Toast.makeText(getContext(), "Connected with device " + mBleDeviceName, Toast.LENGTH_SHORT).show();
    }

    private void onDisconnected() {
        Toast.makeText(getContext(), "Disconnected", Toast.LENGTH_SHORT).show();
        showConnectionError("Device disconnected");
        disconnect();
    }

    private void disconnect() {
        if(null != mBleGatt) {
            mBleGatt.close();
            mBleGatt = null;
        }
        mBleDevice = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    private void showConnectionError(String msg) {
        if(isResumed()) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.control_container,
                            ConnectErrorFragment.newInstance(mBleDeviceName, msg))
                    .commit();
        }
    }

    private void showConnecting() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.control_container,
                        ConnectFragment.newInstance(mBleDeviceName, mBleDeviceAddr))
                .commit();
    }

    private void showControlList() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.control_container, new ControlListFragment())
                .commit();
    }

    @Override
    public void abortConnecting() {
        showConnectionError("Aborted by user");
        disconnect();
    }

    @Override
    public void listFragmentEventOccurred(ControllableObject controllableObject) {
        // Message consists of:
        // 1 byte header: 0xFF.
        // 1 byte object id.
        // 1 byte payload (different for LED/Turnout/TrackSignal.
        // 1 byte CRC-8.
        byte[] message = new byte[] {(byte)0xFF, controllableObject.getId(), (byte)0x00, (byte)0x00};

        // LED.
        if (controllableObject instanceof LED) {
            switch (((LED)controllableObject).getLedState()) {
                case OFF:
                    message[2] = (byte)0;
                    break;
                case BLINKING:
                    message[2] = (byte)1;
                    break;
                case ON:
                    message[2] = (byte)2;
                    break;
            }
        }
        // Turnout.
        else if(controllableObject instanceof Turnout) {
            message[2] = (byte)(((Turnout)controllableObject).getStraight() ? 0 : 1);
        }
        // Tracksignal.
        else if (controllableObject instanceof TrackSignal) {
            message[2] = (byte)( ((TrackSignal)controllableObject).getSpeed()
                    + (((TrackSignal)controllableObject).getForward() ? 0 : 128) );
        }

        // CRC-8.
        byte[] subset = Arrays.copyOfRange(message, 0, 3); // Only Header + ID + "Payload".
        this.crc.reset();
        this.crc.update(subset, 0, subset.length);
        message[3] = (byte)this.crc.getValue();

        this.send(message);
    }

    public interface ScanInitiator {
        void startScan();
    }
}
