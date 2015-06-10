package com.htc.gc.connectivity.internal.common;


public interface CommonBase {

	public final static String TAG = "Common";
	public static final boolean DEBUG = true;

	public static final int ERROR_SUCCESS = 0x00;
	public static final int ERROR_P2P_SSID = 0x90;
	public static final int ERROR_P2P_GROUP = 0x91;
	public static final int ERROR_BOOT_UP_GC = 0x92;
	public static final int ERROR_P2P_GROUP_REMOVE = 0x93;
	public static final int ERROR_FW_UPDATE_SUCCESS = 0x94;
	public static final int ERROR_FW_UPDATE_FAIL = 0x95;
	public static final int ERROR_FAIL = 0x98;
	public static final int ERROR_GET_IP = 0x99;
	public static final int ERROR_WIFI_TIMEOUT = 0x9A;
	public static final int ERROR_GATT_READ = 0xA0;
	public static final int ERROR_GATT_WRITE = 0xA1;
	public static final int ERROR_GATT_SET_NOTIFICATION = 0xA2;
	public static final int ERROR_GATT_RECEIVE_NOTIFICATION = 0xA3;
	public static final int ERROR_DHCP_FAILURE = 0xA4;
	public static final int ERROR_CONN_FAILURE = 0xA5;
	public static final int ERROR_SOFTAP_NOT_FOUND = 0xA6;
	
	public static final int ALARM_SCAN_TIMEOUT = 0x8080;
	public static final int ALARM_BLE_CONNECT_CHECK = 0x8081;
	
	
	
	public enum GcBleTransceiverErrorCode {

		ERROR_NONE,
		ERROR_CONNECTION_STATE_CHANGE,
		ERROR_SERVICE_DISCOVER,
		ERROR_CHARACTERISTIC_READ,
		ERROR_CHARACTERISTIC_WRITE,
		ERROR_DESCRIPTOR_WRITE,
		ERROR_DISCONNECTED_FROM_GATT_SERVER,
		ERROR_CONNECTING,
		ERROR_BOND,
	}
	
	
	
	public enum GcWifiTransceiverErrorCode {

		ERROR_NONE,
		ERROR_WIFI_P2P_DISABLED,
		ERROR_UNKNOWN_STATE,
		ERROR_CONN_BREAK,
		ERROR_WIFI_P2P_GROUP,
		ERROR_GC_SOFTAP_NOT_FOUND,
		ERROR_WIFI_SCAN_TIMEOUT,
		ERROR_RESERVED,
		ERROR_GET_GC_IP_FAILED,
	}
}
