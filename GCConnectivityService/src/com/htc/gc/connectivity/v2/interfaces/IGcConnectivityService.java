package com.htc.gc.connectivity.v2.interfaces;

import java.util.Calendar;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase;

import android.bluetooth.BluetoothDevice;



public interface IGcConnectivityService extends IGcConnectivityServiceBase {
	
	public enum Operation {

		OPERATION_NONE,
		OPERATION_CAPTURE_START,
		OPERATION_VIDEO_RECORDING_NORMAL_START,
		OPERATION_VIDEO_RECORDING_NORMAL_STOP,
		OPERATION_VIDEO_RECORDING_SLOW_MOTION_START,
		OPERATION_VIDEO_RECORDING_SLOW_MOTION_STOP,
		OPERATION_TIME_LAPS_RECORDING_START,
		OPERATION_TIME_LAPS_RECORDING_STOP,
		OPERATION_TIME_LAPS_RECORDING_PAUSE,
		OPERATION_TIME_LAPS_RECORDING_RESUME,
		OPERATION_GET_DR_STATUS,
		OPERATION_GET_FREE_SPACE,
	}

	
	
	public enum OperationEvent {

		OPEVENT_NONE,
		OPEVENT_START_CAPTURING,
		OPEVENT_COMPLETE_CAPTURING,
		OPEVENT_START_RECORDING,
		OPEVENT_COMPLETE_RECORDING,
		OPEVENT_TIME_LAPSE_CAPTURE_ONE,
	}
	
	
	/// Callback
	public static final int CB_OPEN_RESULT 								= 7000;
	public static final int CB_CLOSE_RESULT 							= 7001;
	public static final int CB_LONG_TERM_EVENT_RESULT					= 7002;
	public static final int CB_PERFORMANCE_RESULT						= 7003;
	public static final int CB_BLE_SCAN_RESULT 							= 8000;
	public static final int CB_CREATE_WIFI_P2P_GROUP_RESULT 			= 8001;
	public static final int CB_REMOVE_WIFI_P2P_GROUP_RESULT 			= 8002;
	public static final int CB_REMOVE_WIFI_P2P_GROUP_FORCE_RESULT 		= 8003;
	public static final int CB_REMOVE_WIFI_P2P_GROUP_IN_FINISH_RESULT	= 8004;
	public static final int CB_BLE_CONNECT_RESULT						= 8100;
	public static final int CB_BLE_DISCONNECT_RESULT					= 8101;
	public static final int CB_BLE_DISCONNECT_FORCE_RESULT				= 8102;
	public static final int CB_WIFI_CONNECT_RESULT						= 8200;
	public static final int CB_WIFI_DISCONNECT_RESULT					= 8201;
	public static final int CB_SET_POWER_ONOFF_RESULT					= 8300;
	public static final int CB_GET_POWER_ONOFF_RESULT					= 8301;
	public static final int CB_SET_HW_STATUS_LTEVENT_RESULT				= 8302;
	public static final int CB_CLR_HW_STATUS_LTEVENT_RESULT				= 8303;
	public static final int CB_GET_HW_STATUS_RESULT						= 8304;
	public static final int CB_SET_OPERATION_LTEVENT_RESULT				= 8400;
	public static final int CB_CLR_OPERATION_LTEVENT_RESULT				= 8401;
	public static final int CB_SET_OPERATION_RESULT						= 8402;
	public static final int CB_SET_DATE_TIME_RESULT						= 8500;
	public static final int CB_SET_NAME_RESULT							= 8501;
	public static final int CB_GET_NAME_RESULT							= 8502;
	public static final int CB_SET_GPS_INFO_LTEVENT_RESULT				= 8503;
	public static final int CB_CLR_GPS_INFO_LTEVENT_RESULT				= 8504;
	public static final int CB_SET_GPS_INFO_RESULT						= 8505;
	public static final int CB_GET_BLE_FW_VERSION_RESULT				= 8600;
	public static final int CB_GET_ALL_FW_VERSION_RESULT				= 8601;
	public static final int CB_VERIFY_PASSWORD_RESULT					= 8700;
	public static final int CB_CHANGE_PASSWORD_RESULT					= 8701;
	public static final int CB_SET_AUTO_BACKUP_LTEVENT_RESULT			= 8800;
	public static final int CB_CLR_AUTO_BACKUP_LTEVENT_RESULT			= 8801;
	public static final int CB_SET_AUTO_BACKUP_AP_RESULT				= 8802;
	public static final int CB_CLR_AUTO_BACKUP_AP_RESULT				= 8803;
	public static final int CB_SET_AUTO_BACKUP_PROXY_RESULT				= 8804;
	public static final int CB_GET_AUTO_BACKUP_PROXY_RESULT				= 8805;
	public static final int CB_SET_AUTO_BACKUP_AP_SCAN_RESULT			= 8806;
	public static final int CB_GET_AUTO_BACKUP_STATUS_RESULT			= 8807;
	public static final int CB_SET_METADATA_LTEVENT_RESULT				= 8900;
	public static final int CB_CLR_METADATA_LTEVENT_RESULT				= 8901;
	public static final int CB_SET_CAMERA_ERROR_LTEVENT_RESULT			= 9000;
	public static final int CB_CLR_CAMERA_ERROR_LTEVENT_RESULT			= 9001;
	public static final int CB_SET_LTNOTIFY_RESULT						= 9100;
	public static final int CB_CLR_LTNOTIFY_RESULT						= 9101;
	
	
	
	/// Parameters
	public static final String PARAM_RESULT								= "result";
	public static final String PARAM_RESULT_SOFTAP						= "result_softap";
	public static final String PARAM_LONG_TERM_EVENT					= "long_term_event";
	public static final String PARAM_TASK_NAME							= "task_name";
	public static final String PARAM_TIME_COST_MS						= "time_cost_ms";
	public static final String PARAM_BLUETOOTH_DEVICE					= "bluetooth_device";
	public static final String PARAM_WIFI_ERROR_CODE					= "wifi_error_code";
	public static final String PARAM_DEVICE_IP_ADDRESS					= "device_ip_address";
	public static final String PARAM_MODULE								= "module";
	public static final String PARAM_MODULE_POWER_STATE					= "module_power_state";
	public static final String PARAM_GC_NAME							= "gc_name";
	public static final String PARAM_REQUEST_GPS_INFO_SWITCH			= "request_gps_info_switch";
	public static final String PARAM_BATTERY_LEVEL						= "battery_level";
	public static final String PARAM_USB_STORAGE						= "usb_storage";
	public static final String PARAM_ADAPTER_PLUGIN						= "adapter_plugin";
	public static final String PARAM_GC_POWER							= "gc_power";
	public static final String PARAM_OPERATION							= "operation";
	public static final String PARAM_OPERATION_ERROR_CODE				= "operation_error_code";
	public static final String PARAM_MAIN_FW_VERSION					= "main_fw_version";
	public static final String PARAM_BOOT_FW_VERSION					= "boot_fw_version";
	public static final String PARAM_MCU_FW_VERSION						= "mcu_fw_version";
	public static final String PARAM_BLE_FW_VERSION						= "ble_fw_version";
	public static final String PARAM_VERIFY_PASSWORD_STATUS				= "verify_password_status";
	public static final String PARAM_AUTO_BACKUP_ERROR_TYPE				= "auto_backup_error_type";
	public static final String PARAM_AUTO_BACKUP_ERROR_CODE				= "auto_backup_error_code";
	public static final String PARAM_FILE_ID							= "file_id";
	public static final String PARAM_FOLDER_NAME						= "folder_name";
	public static final String PARAM_FILE_NAME							= "file_name";
	public static final String PARAM_FILE_TYPE							= "file_type";
	public static final String PARAM_FILE_CREATE_TIME					= "file_create_time";
	public static final String PARAM_FILE_SIZE							= "file_size";
	public static final String PARAM_VIDEO_DURATION						= "video_duration";
	public static final String PARAM_DR_STATUS							= "dr_status";
	public static final String PARAM_DR_STATUS_COUNT					= "dr_status_count";
	public static final String PARAM_FREE_SPACE							= "free_space";
	public static final String PARAM_TOTAL_SPACE						= "total_space";
	public static final String PARAM_OPERATION_EVENT					= "operation_event";
	public static final String PARAM_READY_BIT							= "ready_bit";
	public static final String PARAM_IMAGE_REMAIN_COUNT					= "image_remain_count";
	public static final String PARAM_VIDEO_REMAIN_SECOND				= "video_remain_second";
	public static final String PARAM_TIME_LAPSE_REMAIN_COUNT			= "time_lapse_remain_count";
	public static final String PARAM_SLOW_MOTION_REMAIN_SECOND			= "slow_motion_remain_second";
	public static final String PARAM_TIME_LAPSE_CURRENT_COUNT			= "time_lapse_current_count";
	public static final String PARAM_TIME_LAPSE_TOTAL_COUNT				= "time_lapse_total_count";
	public static final String PARAM_VIDEO_CURRENT_SECOND				= "video_current_second";
	public static final String PARAM_CAMERA_ERROR_INDEX					= "camera_error_index";
	public static final String PARAM_CAMERA_ERROR_CODE					= "camera_error_code";
	public static final String PARAM_SWITCH_ON_OFF						= "switch_on_off";
	public static final String PARAM_REMAIN_FILE_COUNT					= "remain_file_count";
	public static final String PARAM_TOTAL_FILE_COUNT					= "total_file_count";
	public static final String PARAM_AP_END_OF_SCAN_LIST				= "ap_end_of_scan_list";
	public static final String PARAM_AP_INDEX_OF_SCAN_LIST				= "ap_index_of_scan_list";
	public static final String PARAM_AP_RSSI							= "ap_rssi";
	public static final String PARAM_AP_SECURITY						= "ap_security";
	public static final String PARAM_AP_AUTHORIZATION					= "ap_authorization";
	public static final String PARAM_AP_SSID							= "ap_ssid";
	public static final String PARAM_AP_PROXY							= "ap_proxy";
	public static final String PARAM_AP_PORT							= "ap_port";
	
	
	
	/// Interfaces
	public boolean gcOpen();
	public boolean gcClose();
	public boolean gcCreateWifiP2pGroup();
	public boolean gcRemoveWifiP2pGroup();
	public boolean gcRemoveWifiP2pGroupForce();
	public boolean gcBleConnect(BluetoothDevice device);
	public boolean gcBleDisconnect(BluetoothDevice device);
	public boolean gcBleDisconnectForce(BluetoothDevice device);
	public boolean gcWifiConnect(BluetoothDevice device);
	public boolean gcWifiDisconnect(BluetoothDevice device);
	public boolean gcSetPowerOnOff(BluetoothDevice device, Module module, SwitchOnOff onoff);
	public boolean gcGetPowerOnOff(BluetoothDevice device, Module module);
	public boolean gcSetHwStatusLTEvent(BluetoothDevice device);
	public boolean gcClrHwStatusLTEvent(BluetoothDevice device);
	public boolean gcGetHwStatus(BluetoothDevice device);
	public boolean gcSetOperationLTEvent(BluetoothDevice device);
	public boolean gcClrOperationLTEvent(BluetoothDevice device);
	public boolean gcSetOperation(BluetoothDevice device, Operation operation);
	public boolean gcSetDateTime(BluetoothDevice device, Calendar calendar);
	public boolean gcSetName(BluetoothDevice device, String name);
	public boolean gcGetName(BluetoothDevice device);
	public boolean gcSetGpsInfoLTEvent(BluetoothDevice device);
	public boolean gcClrGpsInfoLTEvent(BluetoothDevice device);
	public boolean gcSetGpsInfo(BluetoothDevice device, Calendar calendar, double longitude, double latitude, double altitude);
	public boolean gcGetBleFWVersion(BluetoothDevice device);
	public boolean gcVerifyPassword(BluetoothDevice device, String password);
	public boolean gcChangePassword(BluetoothDevice device, String password);
	public boolean gcSetMetadataLTEvent(BluetoothDevice device);
	public boolean gcClrMetadataLTEvent(BluetoothDevice device);
	public boolean gcSetCameraErrorLTEvent(BluetoothDevice device);
	public boolean gcClrCameraErrorLTEvent(BluetoothDevice device);
	public boolean gcSoftAPConnect(BluetoothDevice device, String passwd);
	public boolean gcSetAutoBackupLTEvent(BluetoothDevice device);
	public boolean gcClrAutoBackupLTEvent(BluetoothDevice device);
	public boolean gcSetAutoBackupAP(BluetoothDevice device, WifiAP ap, String ssid, String passwd, byte security);
	public boolean gcClrAutoBackupAP(BluetoothDevice device, byte security, String ssid);
	public boolean gcSetLTNotify(BluetoothDevice device);
	public boolean gcClrLTNotify(BluetoothDevice device);
	public boolean gcSetAutoBackupAPScan(BluetoothDevice device, int option);
	public boolean gcSetAutoBackupProxy(BluetoothDevice device, int port, byte security, String ssid, String proxy);
	public boolean gcGetAutoBackupProxy(BluetoothDevice device, byte security, String ssid);
	public boolean gcGetAllFwVersion(BluetoothDevice device);
	public boolean gcGetAutoBackupStatus(BluetoothDevice device);
}
