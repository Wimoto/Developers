package com.example.bluetoothgatt;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanRecord;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.List;

/**
* Created by Dave Smith
* Double Encore, Inc.
* TemperatureBeacon
*/
class WimotoClimate {
	private static final String TAG = "BluetoothGattActivity";
	
	/* Full Bluetooth UUID that defines the Health Thermometer Service */
    public static final ParcelUuid TEMP_SERVICE = ParcelUuid.fromString("00005608-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid LIGHT_SERVICE = ParcelUuid.fromString("0000560e-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid HUM_SERVICE = ParcelUuid.fromString("00005614-0000-1000-8000-00805f9b34fb");
	

    private String mName;
    private float mCurrentTemp;
    private int mLightLevel;
    
    //Device metadata
    private int mSignal;
    private String mAddress;

    /* Builder for Lollipop+ */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WimotoClimate(ScanRecord record , String deviceName, int rssi) {
    	
    	int lux = 0;
    	float temp = 0;
        mSignal = rssi;
        mName = deviceName;
        
        //Convert raw light values to lux
        lux = ((record.getBytes()[25] << 8) | (record.getBytes()[26] & 0x00ff));
        
        //Convert raw temp values to C
        temp = parseTemp(record.getBytes());
        
        
        mCurrentTemp = temp;
        mLightLevel = lux;
        
        Log.i(TAG, String.format("Temperature: %f C", temp));
        Log.i(TAG, String.format("Light Level: %d lux",lux));
   
    }
    
    

    /* Builder for pre-Lollipop */
    public WimotoClimate(byte[] scanRecord, String deviceName, int rssi) {
        
    	int lux = 0;
    	float temp = 0;
    	mSignal = rssi;
    	mName = deviceName;
    	
    	//Convert raw light values to lux
    	lux = ((scanRecord[25] << 8) | (scanRecord[26] & 0x00FF));
    	
    	//Convert raw temp values to C
    	temp = parseTemp(scanRecord);
    	
    	Log.i(TAG, String.format("Temperature: %f C", temp));
    	Log.i(TAG, String.format("Light Level: %d lux", lux));
        
    }
    
    
    /*
     * Function to convert raw temp values into C
     */
    private float parseTemp(byte[] serviceData) {

        float temp = 0;
        
        temp = ((serviceData[23] << 8) |  (serviceData[24] & 0x00ff));
        temp = -46.85f + (175.72f * (temp/65536) );

        return temp;
    }


    public String getName() {
        return mName;
    }

    public int getSignal() {
        return mSignal;
    }

    public float getCurrentTemp() {
        return mCurrentTemp;
    }

    public int getLightLevel(){
    	return mLightLevel;
    }
    
    
    public String getAddress(){
    	return mAddress;
    }

    @Override
    public String toString() {
        return String.format("%s (%ddBm): %.1fC", mName, mSignal, mCurrentTemp);
    }
}
