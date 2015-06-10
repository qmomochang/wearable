package com.htc.gc.interfaces;


public interface IDeviceItem {
	
	String	getIP();
	void 	setIP(String ip);
	
	String	getPassword();
	void	setPassword(String pass);
	
	String	getDeviceName();
	String	getDeviceBluetoothAddress();
	
	Common.DeviceVersion getDeviceVersion();
	
}
