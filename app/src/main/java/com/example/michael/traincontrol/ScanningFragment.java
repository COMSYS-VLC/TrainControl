package com.example.michael.traincontrol;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class ScanningFragment extends Fragment {
    public interface BluetoothScanResultUser {
        void bleDeviceFound(BluetoothDevice device);
    }

    private static final String ARG_BLE_SERVICE_UUID = "bleservice";

    private static final int REQUEST_ENABLE_LOCATION = 1;

    private static final long SCAN_PERIOD = 10000; // Scanning time for bluetooth devices.

    private String mBleServiceUuid;
    private BluetoothLeScanner mBLeScanner;
    private Handler mHandler = new Handler();
    private LeScanCallback mScanCallback = new LeScanCallback();
    private boolean mScanning = false;

    private ProgressBar mProgressBar;
    private TextView mNoDeviceText;
    private Button mRetryButton;

    public ScanningFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bleServiceUuid Bluetooth LE Service Id to scan for.
     * @return A new instance of fragment ScanningFragment.
     */
    public static ScanningFragment newInstance(String bleServiceUuid) {
        ScanningFragment fragment = new ScanningFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BLE_SERVICE_UUID, bleServiceUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBleServiceUuid = getArguments().getString(ARG_BLE_SERVICE_UUID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_scanning, container, false);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mNoDeviceText = (TextView) view.findViewById(R.id.no_device_found);
        mRetryButton = (Button) view.findViewById(R.id.retry);

        mRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        mBLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        mProgressBar.setVisibility(View.GONE);
        mNoDeviceText.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);

        startScan();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopScan();
        mBLeScanner = null;
    }

    private void startScan() {
        if(mScanning) {
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mNoDeviceText.setVisibility(View.GONE);
        mRetryButton.setVisibility(View.GONE);

        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("BLEScan", "No permission");
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
            return;
        }

        List<ScanFilter> filters = null;
        if(null != mBleServiceUuid) {
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            filterBuilder.setServiceUuid(ParcelUuid.fromString(mBleServiceUuid));
            filters = Collections.singletonList(filterBuilder.build());
        }

        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            settingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
            settingsBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);
        }

        mScanning = true;
        mBLeScanner.startScan(filters, settingsBuilder.build(), mScanCallback);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                abortScan();
            }
        }, SCAN_PERIOD);
        Log.d("BLEScan", "Started...");
    }

    private void abortScan() {
        stopScan();
        mProgressBar.setVisibility(View.GONE);
        mNoDeviceText.setVisibility(View.VISIBLE);
        mRetryButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(REQUEST_ENABLE_LOCATION == requestCode) {
            Log.d("BLEScan", "Perm result");
            if(0 == grantResults.length || PackageManager.PERMISSION_GRANTED != grantResults[0]) {
                Log.d("BLEScan", "Denied");
                mProgressBar.setVisibility(View.GONE);
                mNoDeviceText.setVisibility(View.VISIBLE);
                mRetryButton.setVisibility(View.VISIBLE);
            } else {
                Log.d("BLEScan", "Granted");
                startScan();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void stopScan() {
        mHandler.removeCallbacksAndMessages(null);
        if(null != mBLeScanner && mScanning) {
            mBLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }
    }

    private class LeScanCallback extends ScanCallback {
        @Override
        public void onScanFailed(int errorCode) {
            Log.d("BLEScan", "Error: " + errorCode);
            abortScan();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(ScanSettings.CALLBACK_TYPE_ALL_MATCHES == callbackType || ScanSettings.CALLBACK_TYPE_FIRST_MATCH == callbackType) {
                stopScan();
                if (getActivity() instanceof BluetoothScanResultUser) {
                    ((BluetoothScanResultUser) getActivity()).bleDeviceFound(result.getDevice());
                }
            }
        }
    }
}
