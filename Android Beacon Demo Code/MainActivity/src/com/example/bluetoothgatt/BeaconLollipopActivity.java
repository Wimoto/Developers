package com.example.bluetoothgatt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * BeaconActivity
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BeaconLollipopActivity extends Activity {
	
    private static final String TAG = "BeaconActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    /* Collect unique devices discovered, keyed by address */
    private HashMap<String, WimotoClimate> mBeacons;
    private BeaconAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * We are going to display all the device beacons that we discover
         * in a list, using a custom adapter implementation
         */
        ListView list = new ListView(this);
        mAdapter = new BeaconAdapter(this);
        list.setAdapter(mAdapter);
        setContentView(list);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBeacons = new HashMap<String, WimotoClimate>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Begin scanning for LE devices
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Cancel scan in progress
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        stopScan();
    }

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    /*
     * Sets up the scan settings and begins a 5s scan
     */
    private void startScan() {
        //Scan for devices that advertise the climate temperature service
    	
    	ScanFilter beaconFilter = new ScanFilter.Builder().setServiceUuid(WimotoClimate.TEMP_SERVICE).build();
    	ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
    	filters.add(beaconFilter);
    	
    	Log.i(TAG, "**********STARTING SCAN**********");
    	
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        mHandler.postDelayed(mStopRunnable, 5000);
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
    }
    
    /*
     * Stops scan and sets it to start again after 250ms
     */
    private void stopScan() {
    	
    	Log.i(TAG, "**********ENDING scan**********");
        mBluetoothLeScanner.stopScan(mScanCallback);
        mHandler.postDelayed(mStartRunnable, 100);
    }
    
    /* Updated scan callback for Android Lollipop
     * Invoked when a new device is found
     * The callback function used is dependent on the ScanSettings used 
     */

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: "+results.size()+" results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "LE Scan Failed: "+errorCode);
        }

        
        /*
         * Process ad data and create a new WimotoClimate object
         * and pass it up to the main thread
         * The scanResult result contains all the raw ad data
         */
        
        private void processResult(ScanResult result) {
        
        	
            Log.i(TAG, "New LE Device: " + result.getDevice().getName() + " @ " + result.getRssi());
            //Log.i(TAG, "Temperature: 0x" + String.format("%02x%02x", result.getScanRecord().getBytes()[23], result.getScanRecord().getBytes()[24]));
            //Log.i(TAG, "Light Level: 0x" + String.format("%02x%02x", result.getScanRecord().getBytes()[25], result.getScanRecord().getBytes()[26]));


            WimotoClimate beacon = new WimotoClimate(result.getScanRecord(),
                    result.getDevice().getName(),
                    result.getRssi());
            mHandler.sendMessage(Message.obtain(null, 0, beacon));
        }
    };

    /*
     * We have a Handler to process scan results on the main thread,
     * add them to our list adapter, and update the view
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            WimotoClimate beacon = (WimotoClimate) msg.obj;
            mBeacons.put(beacon.getName(), beacon);

            mAdapter.setNotifyOnChange(false);
            mAdapter.clear();
            mAdapter.addAll(mBeacons.values());
            mAdapter.notifyDataSetChanged();
        }
    };

    /*
     * A custom adapter implementation that displays the Climate data in a table
     * Device Name - Temperature - Light Level - RSSI
     */
    private static class BeaconAdapter extends ArrayAdapter<WimotoClimate> {

        public BeaconAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_beacon_list, parent, false);
            }

            WimotoClimate beacon = getItem(position);


            TextView nameView = (TextView) convertView.findViewById(R.id.text_name);
            nameView.setText(beacon.getName());

            TextView tempView = (TextView) convertView.findViewById(R.id.text_temperature);
            tempView.setText(String.format("%.2f C" , beacon.getCurrentTemp()));

            TextView addressView = (TextView) convertView.findViewById(R.id.text_address);
            addressView.setText(String.format("%d lux", beacon.getLightLevel()));

            TextView rssiView = (TextView) convertView.findViewById(R.id.text_rssi);
            rssiView.setText(String.format("%ddBm", beacon.getSignal()));


            return convertView;
        }


    }
}