package de.rwthaachen.comsys.laboratory.vlc.traincontrol;


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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class ControlFragment extends Fragment implements ConnectFragment.ConnectAbortHandler,
        ConnectErrorFragment.ConnectErrorHandler, SendingFragment {

    private static final String ARG_BLE_DEVICE_NAME = "devname";
    private static final String ARG_BLE_DEVICE_ADDR = "devaddr";
    private static final String ARG_BLE_SERVICE_UUID = "bleservice";
    private static final String ARG_BLE_RX_UUID = "blerx";
    private static final String ARG_BLE_TX_UUID = "bletx";

    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private final CRC8 mCRC = new CRC8();

    private final List<Byte> mReadBuffer = new ArrayList<>();
    private final List<byte[]> mSendBuffer = new ArrayList<>();

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
                Log.d("BleGatt", "Error: Status: " + status + " State: " + newState);
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
                        onDiscovered();
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
            if(characteristic == mBleTxCharacteristic && BluetoothGatt.GATT_SUCCESS == status) {
                synchronized (mSendBuffer) {
                    if(!mSendBuffer.isEmpty()) {
                        mSendBuffer.remove(0);

                        if (!mSendBuffer.isEmpty()) {
                            characteristic.setValue(mSendBuffer.get(0));
                            gatt.writeCharacteristic(characteristic);
                        }
                    }
                }
            } else if(BluetoothGatt.GATT_FAILURE == status) {
                Log.d("Control", "Failed to write characteristic: " + characteristic.getUuid().toString());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(descriptor.getCharacteristic() == mBleRxCharacteristic) {
                final Activity activity = getActivity();
                if(null != activity) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onConnected();
                        }
                    });
                }
            }
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

    private BroadcastReceiver mBondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != mBleDevice) {
                if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    if(mBleDevice.equals(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))) {
                        final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                        if (state == BluetoothDevice.BOND_BONDED) {
                            mBleGatt = mBleDevice.connectGatt(getContext(), false, mBleGattCallback);
                        } else if (state == BluetoothDevice.BOND_NONE) {
                            disconnect();
                            showConnectionError(context.getString(R.string.bonding_failed));
                        }
                    }
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
        if(isResumed()) {
            showConnectionError(getContext().getString(R.string.connection_error));
        }
    }

    private void onReceived(byte[] data) {
        // Fill buffer.
        for (byte b : data) {
            mReadBuffer.add(b);
        }

        // We might have a packet...
        while(mReadBuffer.size() >= 4) {
            // Remove all elements until a header is found.
            while (mReadBuffer.size() > 0 && mReadBuffer.get(0) != (byte) 0xFF) {
                mReadBuffer.remove(0);
            }

            // Check if readBuffer has still the correct length
            if (mReadBuffer.size() >= 4) {
                // Check CRC-8 checksum.
                mCRC.reset();
                for(int i = 0; i < 3; ++i) {
                    mCRC.update(mReadBuffer.get(i));
                }

                if (mCRC.getValue() == mReadBuffer.get(3)) {
                    // Extract packet from readBuffer and handle it
                    Fragment fragment = getChildFragmentManager().findFragmentByTag("panel");
                    if(fragment instanceof ReceivingFragment) {
                        ((ReceivingFragment) fragment).handlePayload(new byte[]{mReadBuffer.get(1), mReadBuffer.get(2)});
                    }

                    for (int i = 0; i < 4; ++i) {
                        mReadBuffer.remove(0);
                    }
                } else {
                    mReadBuffer.remove(0);
                }
            }
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

        getContext().unregisterReceiver(mBondReceiver);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(mBondReceiver, filter);

        connect();
    }

    private void connect() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        mBleDevice = bluetoothAdapter.getRemoteDevice(mBleDeviceAddr);
        if(BluetoothDevice.BOND_NONE == mBleDevice.getBondState()) {
            mBleDevice.createBond();
        } else if(BluetoothDevice.BOND_BONDED == mBleDevice.getBondState()) {
            mBleGatt = mBleDevice.connectGatt(getContext(), false, mBleGattCallback);
        }

        showConnecting();
    }

    private void onDiscovered() {
        if(null == mBleGatt) {
            return;
        }
        BluetoothGattService gattService = mBleGatt.getService(UUID.fromString(mBleServiceUuid));
        if(null == gattService) {
            disconnect();
            showConnectionError(getContext().getString(R.string.service_unsupported));
            return;
        }

        mBleRxCharacteristic = gattService.getCharacteristic(UUID.fromString(mBleRxCharacteristicUuid));
        mBleTxCharacteristic = gattService.getCharacteristic(UUID.fromString(mBleTxCharacteristicUuid));

        if(null == mBleTxCharacteristic || null == mBleRxCharacteristic) {
            disconnect();
            showConnectionError(getContext().getString(R.string.missing_characteristic));
            return;
        } else if((mBleTxCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0 ||
                (((mBleRxCharacteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                        != (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY)))) {
            disconnect();
            showConnectionError(getContext().getString(R.string.missing_characteristic));
            return;
        }

        mBleGatt.setCharacteristicNotification(mBleRxCharacteristic, true);
        BluetoothGattDescriptor descriptor = mBleRxCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleGatt.writeDescriptor(descriptor);

        mBleTxCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }

    private void onConnected() {
        if(null == mBleGatt) {
            return;
        }
        showControlPanel();
        Toast.makeText(getContext(), getContext().getString(R.string.connected_with_device, mBleDeviceName), Toast.LENGTH_SHORT).show();

        // Send first pending message
        if(!mSendBuffer.isEmpty()) {
            mBleTxCharacteristic.setValue(mSendBuffer.get(0));
            mBleGatt.writeCharacteristic(mBleTxCharacteristic);
        }
    }

    private void onDisconnected() {
        if(isResumed()) {
            Toast.makeText(getContext(), R.string.disconnected, Toast.LENGTH_SHORT).show();
            showConnectionError(getContext().getString(R.string.device_disconnected));
        }
        disconnect();
    }

    private void disconnect() {
        if(null != mBleGatt) {
            mBleGatt.close();
            mBleGatt = null;
            mBleTxCharacteristic = null;
            mBleRxCharacteristic = null;
        }
        mBleDevice = null;
        mSendBuffer.clear();
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

    private void showControlPanel() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.control_container, new ControlPanelFragment(), "panel")
                .commit();
    }

    @Override
    public void abortConnecting() {
        showConnectionError(getContext().getString(R.string.aborted_by_user));
        disconnect();
    }

    @Override
    public void sendPayload(byte[] data) {
        if(2 != data.length) {
            throw new IllegalArgumentException("Payload must consist of 2 bytes");
        }
        // Construct packet: 0xFF Header, Payload, CRC-8
        byte[] packet = new byte[4];
        packet[0] = (byte) 0xFF;
        System.arraycopy(data, 0, packet, 1, 2);
        // CRC-8.
        this.mCRC.reset();
        this.mCRC.update(packet, 0, 3);
        packet[3] = mCRC.getValue();

        synchronized (mSendBuffer) {
            mSendBuffer.add(packet);

            if (1 == mSendBuffer.size() && null != mBleGatt && null != mBleTxCharacteristic) {
                mBleTxCharacteristic.setValue(packet);
                mBleGatt.writeCharacteristic(mBleTxCharacteristic);
            }
        }
    }

    public interface ScanInitiator {
        void startScan();
    }
}
