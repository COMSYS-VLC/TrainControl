package com.example.michael.traincontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // The list of objects (turnouts, tracksignals, LEDs) that are controllable.
    private final ControllableObject[] controllableObjects = new ControllableObject[]{
            new Turnout(Color.rgb(255, 255, 0), "Turnout 1", true),
            new TrackSignal(Color.rgb(255, 0, 0), "Red Signal", 0, true),
            new LED(Color.rgb(0, 0, 255), "Left Front", LED.LedState.OFF),
            new LED(Color.rgb(0, 0, 255), "Right Front", LED.LedState.BLINKING),
            new LED(Color.rgb(255, 0, 0), "Left Back", LED.LedState.BLINKING),
            new LED(Color.rgb(255, 0, 0), "Right Back", LED.LedState.ON)};

    SharedPreferences sharedPreferences;

    private ProgressBar progressBar; // Show the user that BLE scan is active.

    private final static int REQUEST_ENABLE_BT = 1; // Request code for bluetooth enable indent.
    private final static int REQUEST_ENABLE_LOCATION = 2; // Request code for location enable indent.

    private static final long SCAN_PERIOD = 10000; // Scanning time for bluetooth devices.
    private Handler handler; // Handler Threading for BLE scanning. Stop scanning after SCAN_PERIOD for energy saving.
    private Runnable runnableScanner; // Runnable which is given to the handler.

    private BluetoothAdapter bluetoothAdapter; // The reference to the bluetooth adapter of the device.
    private BluetoothDevice bluetoothDevice; // The BLE device.
    private BluetoothGatt bluetoothGATT; // Access to GATT services.
    private String bleDeviceID;
    private boolean connected; // Store if BLE device is connected.
    private static final UUID BLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothSocket bluetoothSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.progressBar = (ProgressBar) this.findViewById(R.id.progressBar);

        // Load shared preferences.
        this.sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        this.bleDeviceID = this.sharedPreferences.getString(this.getString(R.string.saved_device_id_default),
                this.getString(R.string.saved_device_id_default));

        // Handler + Runnable used for BLE device scanning.
        this.handler = new Handler();
        this.runnableScanner = new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.stopLeScan(leScanCallback);
                if (bluetoothDevice == null) {
                    Toast.makeText(getApplicationContext(), R.string.no_BLE_device_found, Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        };

        // Setup bluetooth, which is desperately needed for this app.
        this.setupBluetooth();
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
                this.scanLeDevice(true);
                break;
            case R.id.action_setBLEdeviceID:
                this.editBleDeviceId();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Edit the BLE device id.
     */
    private void editBleDeviceId() {
        final EditText editTextServerAddress = new EditText(this);
        editTextServerAddress.setText(this.bleDeviceID);
        editTextServerAddress.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("BLE Device ID")
                .setMessage("Please enter the BLE device ID!")
                .setView(editTextServerAddress)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        bleDeviceID = editTextServerAddress.getText().toString();

                        // Save new BLE device ID in preferences.
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(getString(R.string.saved_device_id_default), bleDeviceID);
                        editor.apply();
                        // Reconnect.
                        scanLeDevice(true);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    /**
     * Check if the device is able to use Bluetooth Low Energy.
     */
    private void setupBluetooth() {
        // Check if BLE is available.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            this.finish();
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        // Check if Bluetooth is enabled.
        if (this.bluetoothAdapter == null || !this.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Check if location (GPS) is enabled.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
        }

        // Scan for BLE devices.
        this.scanLeDevice(true);
    }

    /**
     * Scan for BLE devices.
     *
     * @param enable true = start scanning, false = stop scanning.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stop scanning after certain period.
            this.handler.postDelayed(this.runnableScanner, SCAN_PERIOD);
            // Start scanning now.
            this.bluetoothDevice = null;
            this.connected = false;
            this.hideListView();
            this.destroyBleConnection();
            progressBar.setVisibility(View.VISIBLE);
            this.bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            // Abort scanning for BLE devices.
            this.bluetoothAdapter.stopLeScan(leScanCallback);
            this.handler.removeCallbacks(this.runnableScanner);
        }
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
                // App does not work without Bluetooth at all, so shut down after noticing the user.
                Toast.makeText(this, R.string.bluetooth_necessary, Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    /**
     * Handle the result for "enable location" dialogue.
     * @param requestCode 2 for enable location.
     * @param permissions List of permissions, which got granted in the dialogue.
     * @param grantResults The user-selected results from the dialogue.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ENABLE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // App does not work without Location at all, so shut down after noticing the user.
                Toast.makeText(this, R.string.location_required, Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    /**
     * Fill the listView-Adapter with controllable objects and adapt them to the listView.
     */
    private void showListView() {
        ListView listView = (ListView) findViewById(R.id.listView);

        CustomAdapter adapter = new CustomAdapter(this, this.controllableObjects);
        listView.setAdapter(adapter);
    }

    /**
     * Hide the controllableObjects from the user if bluetooth device is not available.
     */
    private void hideListView() {
        ListView listView = (ListView) findViewById(R.id.listView);

        CustomAdapter adapter = new CustomAdapter(this, new ControllableObject[0]);
        listView.setAdapter(adapter);
    }

    /**
     * Device scan callback.
     */
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getAddress().equals(bleDeviceID)) {
                        scanLeDevice(false);
                        bluetoothDevice = device;
                        bluetoothGATT = bluetoothDevice.connectGatt(MainActivity.this, false, gattCallback);
                    }
                }
            });
        }
    };

    /**
     * BLE GATT callback.
     */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                // Connect with BLE device.
                case BluetoothProfile.STATE_CONNECTED:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connected();
                        }
                    });
                    break;
                // Disconnect with BLE device.
                case BluetoothProfile.STATE_DISCONNECTED:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            disconnected();
                        }
                    });
                    break;
                // Doesn't matter...
                default:
                    break;
            }
        }
    };

    /**
     * Connect with BLE device.
     */
    private void connected() {
        this.connected = true;
        progressBar.setVisibility(View.GONE);
        this.showListView();

        // TODO: work here!
        /*
        try {
            this.bluetoothSocket = this.bluetoothDevice.createRfcommSocketToServiceRecord(BLE_UUID);
            this.bluetoothSocket.connect();
        }
        catch (IOException e) {
            e.printStackTrace();
            try {
                this.bluetoothSocket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        */
    }

    /**
     * Disconnect from BLE device.
     */
    private void disconnected() {
        this.connected = false;
        progressBar.setVisibility(View.GONE);
        this.hideListView();
        Toast.makeText(MainActivity.this, R.string.ble_disconnected, Toast.LENGTH_LONG).show();
    }

    /**
     * Tidy up before leaving the app.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.handler.removeCallbacks(this.runnableScanner);

        this.destroyBleConnection();
    }

    /**
     * Cancel an ongoing BLE connection.
     */
    private void destroyBleConnection() {
        if (this.bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (this.bluetoothGATT != null) {
            this.bluetoothGATT.close();
            this.bluetoothGATT = null;
        }
    }
}
