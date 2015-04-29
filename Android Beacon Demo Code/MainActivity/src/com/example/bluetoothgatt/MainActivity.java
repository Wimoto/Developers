package com.example.bluetoothgatt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Dave Smith
 * Double Encore, Inc.
 * MainActivity
 */
public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private static final String TAG = "BluetoothGattActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevices;
    private SparseArray<byte[]> scanRecords;
    

    private BluetoothGatt mConnectedGatt;

    private TextView mTemperature, mHumidity, mPressure, deviceName;

    private ProgressDialog mProgress;
    
    private static Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);

        /*
         * We are going to display the results in some text fields
         */
        mTemperature = (TextView) findViewById(R.id.text_temperature);
        mHumidity = (TextView) findViewById(R.id.text_humidity);
        mPressure = (TextView) findViewById(R.id.text_pressure);
        deviceName = (TextView) findViewById(R.id.deviceName);

        /*
         * Bluetooth in Android 4.3 is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();
        scanRecords = new SparseArray<byte[]>();

        /*
         * A progress dialog will be needed while the connection process is
         * taking place
         */
        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);
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

        clearDisplayValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Make sure dialog is hidden
        mProgress.dismiss();
        mBluetoothAdapter.stopLeScan(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from any active tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add the "scan" option to the menu
        getMenuInflater().inflate(R.menu.main, menu);
        //Add any device elements we've discovered to the overflow menu
        for (int i=0; i < mDevices.size(); i++) {
            BluetoothDevice device = mDevices.valueAt(i);
            menu.add(0, mDevices.keyAt(i), 0, device.getName());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                mDevices.clear();
                //*************************START SCANNING FOR DEVICES******************************
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevices.get(item.getItemId());
                byte[] scanRec = scanRecords.get(item.getItemId());
                
                //Display sensor data for selected device
                mTemperature.setText(String.format("%02x%02x",scanRec[23],scanRec[24]));
                mPressure.setText(String.format("%02x%02x",scanRec[25],scanRec[26]));
                mHumidity.setText(String.format("%02x%02x",scanRec[27],scanRec[28]));
                deviceName.setText(device.getName());
      
                
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearDisplayValues() {
        mTemperature.setText("---");
        mHumidity.setText("---");
        mPressure.setText("---");
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

    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        mHandler.postDelayed(mStopRunnable, 5000);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }
    
    
    
    
    /* BluetoothAdapter.LeScanCallback */
    /*
     * Callback utilized when a device has been found
     * From here the scanRecord can be accessed and parsed for data
     */

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    	
    	
    	
        /*
         * Print AD data to log output
         */
    	
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);
        /*for (int i = 0; i<scanRecord.length; i++){
        	Log.i(TAG, i + ":" +  String.format("%02x", scanRecord[i])  );
        }*/
        
        Log.i(TAG, "Temperature: 0x" + String.format("%02x%02x", scanRecord[23],scanRecord[24]));
        Log.i(TAG, "Light Level: 0x" + String.format("%02x%02x", scanRecord[25],scanRecord[26]));
        Log.i(TAG, "Humidity: 0x" + String.format("%02x%02x", scanRecord[27],scanRecord[28]));
        

        /*
         * Add BLE device to collection 
         * Add scan data to collection
         */
            mDevices.put(device.hashCode(), device);
            scanRecords.put(device.hashCode(), scanRecord);
            //Update the overflow menu
            invalidateOptionsMenu();
                       
            
        
    }
    

    



}