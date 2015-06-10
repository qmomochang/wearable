package com.htc.gc.connectivity.internal.common;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice;



public class GcConnectivityDevice implements IGcConnectivityDevice {
    
	private final static String TAG = "GcConnectivityDevice";

	private BluetoothDevice mBluetoothDevice;
	private String mName;
	private String mAddress;
    private GcState mGcState;
    
    private GcStateBle mGcStateBle;
    
    private BootUpReady mBootUpReady;
    private String mIpAddress;
    
    private int mConnectCount;
    private int mDisconnectCount;
    private int mVersionBle;
    private GcVersion mVersion;
    
    
	
	public GcConnectivityDevice(BluetoothDevice device) {

		mBluetoothDevice = device;
		mName = device.getName();
		mAddress = device.getAddress();
		
    	mGcState = GcState.GCSTATE_STANDBY;
    	
    	mGcStateBle = GcStateBle.GCSTATE_BLE_DISCONNECTED;
    	
    	mBootUpReady = BootUpReady.BOOTUP_UNKNOWN;
    	mIpAddress = null;
    	
    	mConnectCount = 0;
    	mDisconnectCount = 0;
    	
    	mVersionBle = -1;
    	mVersion = GcVersion.UNKNOWN;
	}


    
    public void setGcState(GcState state) {

    	Log.d(TAG, "[MGCC] setGcState: " + mGcState + " --> " + state);
    	mGcState = state;
    }

    
    
    @Override
    public GcState getGcState() {

    	return mGcState;
    }
	
    
    
    public void setGcStateBle(GcStateBle state) {

    	Log.d(TAG, "[MGCC] setGcStateBle: " + mGcStateBle + " --> " + state);
    	mGcStateBle = state;
    }

    
    
    public GcStateBle getGcStateBle() {

    	return mGcStateBle;
    }
    
    
    
    public void setGcBootUpReady(BootUpReady ready) {

    	mBootUpReady = ready;
    }

    
    
    public BootUpReady getGcBootUpReady() {

    	return mBootUpReady;
    }
	
    
    
    public void setIpAddress(String ip) {

    	mIpAddress = ip;
    }

    
    
    public String getIpAddress() {

    	return mIpAddress;
    }
    
    
    
    public void setConnectCount(int count) {

    	mConnectCount = count;
    }

    
    
    public int getConnectCount() {

    	return mConnectCount;
    }
    
    
    
    public void setDisconnectCount(int count) {

    	mDisconnectCount = count;
    }

    
    
    public int getDisconnectCount() {

    	return mDisconnectCount;
    }
    
    
    
    public void setVersionBle(int version) {

    	Log.d(TAG, "[MGCC] setVersionBle() = " + version);
    	mVersionBle = version;
    }

    
    
    public int getVersionBle() {

    	Log.d(TAG, "[MGCC] getVersionBle() = " + mVersionBle);
    	return mVersionBle;
    }
    
    
    
	@Override
	public BluetoothDevice getBluetoothDevice() {
		
		return mBluetoothDevice;
	}

	
	
	@Override
	public String getName() {

		return mName;
	}

	
	
	@Override
	public void setName(String name) {
		
		mName = name;
	}
	


	@Override
	public String getAddress() {

		return mAddress;
	}



	@Override
	public void setAddress(String address) {
		
		mAddress = address;
	}



	@Override
	public GcVersion getVersion() {
		return mVersion;
	}



	@Override
	public void setVersion(GcVersion version) {
		mVersion = version;
	}
}
