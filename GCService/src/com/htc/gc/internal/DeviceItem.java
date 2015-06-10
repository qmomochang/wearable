package com.htc.gc.internal;

import android.bluetooth.BluetoothDevice;

import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.IDeviceItem;

public class DeviceItem implements IDeviceItem {
	
	private String mIP;
	private String mPassword;
	private String mName;
	private final BluetoothDevice mDevice;
	private final Common.DeviceVersion mDeviceVersion;
	
	public DeviceItem(BluetoothDevice device, Common.DeviceVersion deviceVersion) {
		mDevice = device;
		mDeviceVersion = deviceVersion;
		
		if(mDevice != null) {
			mName = mDevice.getName();	
		} else {
			mName = "Dummy";
		}
		
	}
	
	@Override
	public String getIP() {
		return mIP;
	}

	@Override
	public void setIP(String ip) {
		mIP = ip;
	}
	
	public void setDeviceName(String name) {
		mName = name;
	}

	@Override
	public String getDeviceName() {
		return mName;
	}
	
	@Override
	public String getDeviceBluetoothAddress() {
		if(mDevice != null) {
			return mDevice.getAddress();
		}
		
		return "00:00:00:00:00:00";
	}

	public BluetoothDevice getDevice() {
		return mDevice;
	}

	@Override
	public String getPassword() {
		return mPassword;
	}

	@Override
	public void setPassword(String pass) {
		mPassword = pass;
	}

	@Override
	public Common.DeviceVersion getDeviceVersion() {
		return mDeviceVersion;
	}

}
