package com.htc.gc.connectivity.internal.common;

import java.util.ArrayList;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcStateBle;
import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScanner;

import android.bluetooth.BluetoothDevice;
import android.util.Log;



public class GcConnectivityDeviceGroup {

	private final static String TAG = "GcConnectivityDeviceGroup";
	
	private static Object accessLock = new Object();
	
	private static GcConnectivityDeviceGroup sInstance;

    private ArrayList<GcConnectivityDevice> mGcConnectivityDeviceList;
    
    
    public static GcConnectivityDeviceGroup getInstance() {
    	GcConnectivityDeviceGroup instance = sInstance;
		if (instance == null) {
			synchronized (accessLock) {
				instance = sInstance;
				if (instance == null) {
					instance = sInstance = new GcConnectivityDeviceGroup();
				}
			}
		}
		return instance;
    }
    
	
    
    private GcConnectivityDeviceGroup() {

    	mGcConnectivityDeviceList = new ArrayList<GcConnectivityDevice>();
    }

    
    
    public boolean addDevice(BluetoothDevice device, byte[] scanRecord) {

    	boolean ret = false;
    	
    	if (device != null) {
    		
			boolean isGc1Device = checkGc1DeviceKey(scanRecord);
			boolean isGc2Device = checkGc2DeviceKey(scanRecord);
			
    		if ((isGc1Device) || 
				(isGc2Device) ||
    			((device.getName() != null) && (device.getName().contains("hTC GC")))) {
    			
    			GcVersion gcVersion = isGc2Device ? GcVersion.GC2 : (isGc1Device ? GcVersion.GC1 : GcVersion.UNKNOWN);

    	    	for (int cnt = 0; cnt < getCount(); cnt++) {
    	    		
    	    		if (getDeviceList().get(cnt).getBluetoothDevice().equals(device)) {
    	    			
    	    			GcConnectivityDevice gcTempDevice = getDeviceList().get(cnt);
    	    			
    	    			gcTempDevice.setVersion(gcVersion);
    	    			
    	    			if (gcTempDevice.getGcStateBle() == GcStateBle.GCSTATE_BLE_CONNECTED) {
    	    				
    	    				gcTempDevice.setGcStateBle(GcStateBle.GCSTATE_BLE_DISCONNECTED);
    	    				
    	    				Log.d(TAG, "[MGCC] addDevice just disconnected device = " + device);
    	    				
    	    				return true;
    	    				
    	    			} else {
    	    				
    	    				Log.d(TAG, "[MGCC] addDevice duplicated device = " + device);
    	    				
    	    				return false;
    	    			}
    	    		}
    	    	}

    	    	GcConnectivityDevice gcDevice = new GcConnectivityDevice(device);
    	    	gcDevice.setVersion(gcVersion);
    	    	getDeviceList().add(gcDevice);

    	    	ret = true;

    		} else {
    			
    			Log.d(TAG, "[MGCC] addDevice not matched device = " + device);
    		}
    	}
    	
    	return ret;
    }

    
    
    public boolean addDevice(BluetoothDevice device) {

    	boolean ret = false;
    	
    	if (device != null) {
    		
	    	for (int cnt = 0; cnt < getCount(); cnt++) {
	    		
	    		if (getDeviceList().get(cnt).getBluetoothDevice().equals(device)) {
	    			
	    			return false;
	    		}
	    	}

	    	GcConnectivityDevice gcDevice = new GcConnectivityDevice(device);
	    	getDeviceList().add(gcDevice);

	    	ret = true;
    	}
    	
    	return ret;
    }

    
    
    public GcConnectivityDevice getDevice(BluetoothDevice device) {

    	for (int cnt = 0; cnt < getCount(); cnt++) {
    		
    		GcConnectivityDevice gcDevice = getDeviceList().get(cnt);
    		
    		if (gcDevice.getBluetoothDevice().equals(device)) {
    			
    			return gcDevice;
    		}
    	}

    	return null;
    }

    
    
    public void removeDevice(GcConnectivityDevice gcDevice) {
    	
    	if (gcDevice != null) {
    		
    		getDeviceList().remove(gcDevice);
    	}
    }
    
    
    
    public ArrayList<GcConnectivityDevice> getDeviceList() {
    	
    	return mGcConnectivityDeviceList;
    }
    
    
    
    public void clear() {

    	mGcConnectivityDeviceList.clear();
    }

    
    
    public int getCount() {

    	return mGcConnectivityDeviceList.size();
    }
    
    
    
    private boolean checkGc1DeviceKey(byte[] scanRecord) {

    	boolean ret = false;
    	
		if ((scanRecord != null) && (scanRecord.length > 7)) {
			
    		String key = String.format("%02x%02x", scanRecord[6], scanRecord[5]);
			
    		if (key.equals("a000")) {
    			
    			ret = true;
    		}
		}
    	
		return ret;
    }
    
    
    
    private boolean checkGc2DeviceKey(byte[] scanRecord) {

    	boolean ret = false;
		
		if (scanRecord != null) {
			// scan record is composed of several sections
			// section format:
			// 1 byte: length
			// 1 byte: id
			// N bytes: section data (N == length - 1)
			byte sectionId;
			byte sectionLength;
			int i = 0;
			while (i < scanRecord.length) {
				sectionLength = scanRecord[i];
				if (sectionLength <= 0) { // end of scan record 
					break;
				}
				
				sectionId = scanRecord[i + 1];
				if (sectionId == (byte) 0xFF) { // manufacturer specific data
					if (scanRecord[i + 2] == (byte) 0x0F && 
						scanRecord[i + 3] == (byte) 0x00 && 
						scanRecord[i + 4] == (byte) 0xCF &&
						scanRecord[i + 5] == (byte) 0x00) {
						ret = true;
					}
					break;
				}
				
				i += (sectionLength + 1);
			}
		}
		return ret;
    }
}
