package com.example.michael.traincontrol;

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

        mPreferences = getPreferences(Context.MODE_PRIVATE);
    }

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
                //TODO: this.scanLeDevice(true);
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
                Toast.makeText(this, R.string.bluetooth_necessary, Toast.LENGTH_LONG).show();
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

    @Override
    public void enableBluetooth() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (null == bluetoothAdapter || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void bleDeviceFound(BluetoothDevice device) {
        mPreferences.edit()
                .putString(PREF_KEY_BLEDEV_NAME, device.getName())
                .putString(PREF_KEY_BLEDEV_ADDR, device.getAddress())
                .apply();
        startConnection(device.getName(), device.getAddress());
    }

    @Override
    public void startScan() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ScanningFragment.newInstance(BLEBEE_SERVICE_UUID)).commit();
    }

    private void startConnection(String bleDevName, String bleDevAddr) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ControlFragment.newInstance(bleDevName, bleDevAddr,
                        BLEBEE_SERVICE_UUID, BLEBEE_RX_CHARACTERISTIC, BLEBEE_TX_CHARACTERISTIC)).commit();
    }
}
