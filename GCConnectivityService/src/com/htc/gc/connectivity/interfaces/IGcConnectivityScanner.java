package com.htc.gc.connectivity.interfaces;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;



public interface IGcConnectivityScanner {
	
	public enum ScanState {

		SCAN_STATE_NONE,
		SCAN_STATE_STANDBY,
		SCAN_STATE_SCANNING,
	}
	
	public enum ScanResult {

		SCAN_RESULT_HIT,
		SCAN_RESULT_HIT_CONNECTED,
		SCAN_RESULT_COMPLETE,
		SCAN_RESULT_ERROR,
	}
	
	/// Interfaces
	public boolean gcOpen();
	public boolean gcClose();
	public boolean gcScan(int period);
	public boolean gcStopScan();
	
	/// Callback
	public static final int CB_BLE_SCAN_RESULT 							= IGcConnectivityService.CB_BLE_SCAN_RESULT;
	
	/// Parameters
	public static final String PARAM_RESULT								= IGcConnectivityService.PARAM_RESULT;
	public static final String PARAM_BLUETOOTH_DEVICE					= IGcConnectivityService.PARAM_BLUETOOTH_DEVICE;
	public static final String PARAM_BLUETOOTH_DEVICE_VERSION			= "bluetooth_device_version";
}
