package com.htc.gc.connectivity.interfaces;

import android.bluetooth.BluetoothDevice;



public interface IGcConnectivityDevice {
    
	enum GcState {

		GCSTATE_STANDBY,
		GCSTATE_RESET_PAIRING_RECORD,
		GCSTATE_NORMAL_BLE_CONNECTING,
		GCSTATE_NORMAL_BOOT_UP_READY_CHECKING,
		GCSTATE_NORMAL_PAIRING_CHECKING,
		GCSTATE_NORMAL_PAIRING_WAITING,
		GCSTATE_NORMAL_WIFI_CONNECTING,
		GCSTATE_NORMAL_CONNECTED,
		GCSTATE_NORMAL_WIFI_DISCONNECTING,
	}

	
	
	enum BootUpReady {

		BOOTUP_UNKNOWN,
		BOOTUP_NON_READY,
		BOOTUP_READY,
	}
	
	
	
	public enum GcStateBle {

		GCSTATE_BLE_NONE,
		GCSTATE_BLE_CONNECTING,
		GCSTATE_BLE_CONNECTED,
		GCSTATE_BLE_DISCONNECTING,
		GCSTATE_BLE_DISCONNECTED,
	}

	
	
	public enum GcStateWifi {

		GCSTATE_WIFI_NONE,
		GCSTATE_WIFI_CONNECTING,
		GCSTATE_WIFI_CONNECTED,
		GCSTATE_WIFI_DISCONNECTING,
		GCSTATE_WIFI_DISCONNECTED,
	}
	
	
	
	public enum GcVersion {
		UNKNOWN,
		GC1,
		GC2,
	}

	
	
	public GcState getGcState();
		
	public String getName();
	public void setName(String name);

	public String getAddress();
	public void setAddress(String address);
	
	public GcVersion getVersion();
	public void setVersion(GcVersion version);
	
	public BluetoothDevice getBluetoothDevice();
    
}
