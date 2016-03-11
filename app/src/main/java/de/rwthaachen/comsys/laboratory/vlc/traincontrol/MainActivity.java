package de.rwthaachen.comsys.laboratory.vlc.traincontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * MainActivity. Starting point of the application.
 */
public class MainActivity extends AppCompatActivity implements BluetoothEnableFragment.BluetoothEnabler, ScanningFragment.BluetoothScanResultUser, ControlFragment.ScanInitiator {

    private final static int REQUEST_ENABLE_BT = 1; // Request code for bluetooth enable intent.

    private static final String BLEBEE_SERVICE_UUID = "EF080D8C-C3BE-41FF-BD3F-05A5F4795D7F";
    private static final String BLEBEE_RX_CHARACTERISTIC = "A1E8F5B1-696B-4E4C-87C6-69DFE0B0093B";
    private static final String BLEBEE_TX_CHARACTERISTIC = "1494440E-9A58-4CC0-81E4-DDEA7F74F623";

    private static final String PREF_KEY_BLEDEV_NAME = "bledevname";
    private static final String PREF_KEY_BLEDEV_ADDR = "bledevaddr";

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);

        mPreferences = getPreferences(Context.MODE_PRIVATE);
    }

    /**
     * Check if current device has a BluetoothAdapter and Bluetooth enabled.
     */
    private void checkBluetooth() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if(null == bluetoothAdapter || !bluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
        } else {
            if(mPreferences.contains(PREF_KEY_BLEDEV_NAME) && mPreferences.contains(PREF_KEY_BLEDEV_ADDR)) {
                startConnection(mPreferences.getString(PREF_KEY_BLEDEV_NAME, ""),
                        mPreferences.getString(PREF_KEY_BLEDEV_ADDR, ""));
            } else {
                startScan();
            }
        }
    }

    /**
     * Request the user to enable Bluetooth.
     */
    private void requestBluetoothEnable() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new BluetoothEnableFragment()).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_reconnect:
                if (getSupportFragmentManager().findFragmentByTag("fragment_connect") != null) {
                    startConnection(mPreferences.getString(PREF_KEY_BLEDEV_NAME, ""),
                            mPreferences.getString(PREF_KEY_BLEDEV_ADDR, ""));
                }
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle the result for "enable bluetooth" dialogue.
     *
     * @param requestCode 1 for enable bluetooth.
     * @param resultCode  Either ok or cancelled.
     * @param data        Not used.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we are responding to.
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful.
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.bluetooth_required, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkBluetooth();
    }

    /**
     * Send an intent to the user: Politely ask to enable Bluetooth.
     */
    @Override
    public void enableBluetooth() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (null == bluetoothAdapter || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * When a BLE device is found, update the BLE preferences and start connecting to the device.
     * @param device The BLE device.
     */
    @Override
    public void bleDeviceFound(BluetoothDevice device) {
        mPreferences.edit()
                .putString(PREF_KEY_BLEDEV_NAME, device.getName())
                .putString(PREF_KEY_BLEDEV_ADDR, device.getAddress())
                .apply();
        startConnection(device.getName(), device.getAddress());
    }

    /**
     * Start a BLE scan for a device with the current BLEEBee service UUID.
     */
    @Override
    public void startScan() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ScanningFragment.newInstance(BLEBEE_SERVICE_UUID)).commit();
    }

    /**
     * Start a BLE connection to the specified BLE device.
     * @param bleDevName BLE device name.
     * @param bleDevAddr BLE device address.
     */
    private void startConnection(String bleDevName, String bleDevAddr) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ControlFragment.newInstance(bleDevName, bleDevAddr,
                        BLEBEE_SERVICE_UUID, BLEBEE_RX_CHARACTERISTIC, BLEBEE_TX_CHARACTERISTIC), "fragment_connect").commit();
    }
}
