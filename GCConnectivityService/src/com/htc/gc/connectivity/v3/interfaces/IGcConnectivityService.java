package com.htc.gc.connectivity.v3.interfaces;

import java.util.Calendar;
import java.util.List;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase;

import android.bluetooth.BluetoothDevice;



public interface IGcConnectivityService extends IGcConnectivityServiceBase {
	
	public enum BootUpType{

		BOOTUP_RTOS ((byte)0x00),
		BOOTUP_LINUX((byte)0x01);

		private final byte type;
		BootUpType (byte bootup_os)
		{
			this.type = bootup_os;
		}

		public byte getType()
		{
			return this.type;
		}
	}
	
	public enum FWUpdatePart{

		UPDATE_RTOS ((byte)0x01),
		UPDATE_MODEM((byte)0x02),
		UPDATE_MCU  ((byte)0x04);

		private final byte part;
		FWUpdatePart (byte update_part)
		{
			this.part = update_part;
		}

		public byte getPart()
		{
			return this.part;
		}
	}

	public enum TriggerFWUpdateResult{
		SUCCESS_TO_START    ((byte)0x00),
		FAIL_NO_UPDATE_IMAGE((byte)0x01),
		FAIL_LOW_BATTERY    ((byte)0x02);

		private final byte error;
		TriggerFWUpdateResult (byte result_error)
		{
			this.error = result_error;
		}

		public static TriggerFWUpdateResult findError(byte status)
		{
			for (TriggerFWUpdateResult error_selected:TriggerFWUpdateResult.values())
			{
				if ( error_selected.error == status)
				{
					return error_selected;
				}
			}
			//Should not be here
			return null;
		}

		public byte getError()
		{
			return this.error;
		}
	}

	public enum FWUpdateResult{

		NONE_UPDATE    ((byte)0x00),
		UPDATING	   ((byte)0x01),
		UPDATED        ((byte)0x02),
		UPDATE_ERROR   ((byte)0x03);

		private final byte error;
		FWUpdateResult (byte result_error)
		{
			this.error = result_error;
		}

		public byte getError()
		{
			return this.error;
		}
	}

	public enum WifiConfigureType {
		WIFI_SOFTAP((byte)0x00),
		WIFI_STATION((byte)0x01),
		WIFI_CONN_AP((byte)0x11),
		WIFI_DISCONN((byte)0xff);

		private final byte type;
		WifiConfigureType(byte type)
		{
			this.type = type;
		}

		public byte getType()
		{
			return this.type;
		}
	}
	
	public enum WifiConfigBand {
		WIFI_24G ((byte)0x00),
		WIFI_5G ((byte)0x01);

		private final byte band;
		WifiConfigBand (byte band)
		{
			this.band = band;
		}

		public byte getBand()
		{
			return this.band;
		}
	}

	public enum WifiConfigSecurity {
		WIFI_OPEN      ((byte)0x00),
		WIFI_WEP       ((byte)0x01),
		WIFI_WPA       ((byte)0x02),
		WIFI_WPA_EAP   ((byte)0x03),
		WIFI_WPA2      ((byte)0x04),
		WIFI_WPA_AES   ((byte)0x05),
		WIFI_WPA2_TKIP ((byte)0x06),
		WIFI_WPA2_EAP  ((byte)0x07);

		private final byte type;
		WifiConfigSecurity (byte security)
		{
			this.type = security;
		}

		public byte getSecurity()
		{
			return this.type;
		}
	}

	public enum BackupProviderIdIndex {
		PROVIDER_NONE         ((byte)0x00),
		PROVIDER_DROPBOX      ((byte)0x01),
		PROVIDER_GOOGLEDRIVE  ((byte)0x02),
		PROVIDER_AUTOSAVE     ((byte)0x03),
		PROVIDER_BAIDU        ((byte)0x04);

		private final byte pid;
		BackupProviderIdIndex (byte id)
		{
			this.pid = id;
		}
		
		public static BackupProviderIdIndex findProvider(byte id)
		{
			for (BackupProviderIdIndex provider:BackupProviderIdIndex.values())
			{
				if ( provider.pid == id)
				{
					return provider;
				}
			}
			//Should not be here
			return null;
		}

		public byte getID()
		{
			return this.pid;
		}
	}

	public enum BackupTokenType {
		TOKENTYPE_ACCESS      ((byte)0x00),
		TOKENTYPE_REFLESH     ((byte)0x01),
		TOKENTYPE_CLIENTID    ((byte)0x02),
		TOKENTYPE_CLIENTSELECT((byte)0x03);

		private final byte token_type;
		BackupTokenType (byte type)
		{
			this.token_type = type;
		}
		
		public static BackupTokenType findToken(byte type) 
		{			
			for (BackupTokenType tokenType : BackupTokenType.values()) {
				if (tokenType.getType() == type) {
					return tokenType;
				}
			}
			//Should not be here
			return null; 
		}

		public byte getType()
		{
			return this.token_type;
		}
	}
	
	public enum BackupProcessStatus {
		PROCESSING_STOPPED      ((byte)0x00),
		PROCESSING_UPDATING     ((byte)0x01),
		PROCESSING_FINISH       ((byte)0x02),
		PROCESSING_ERROR        ((byte)0x03);

		private final byte backup_status;
		BackupProcessStatus (byte status)
		{
			this.backup_status = status;
		}
		
		public static BackupProcessStatus findStatus(byte status)
		{
			for (BackupProcessStatus backupStatus:BackupProcessStatus.values())
			{
				if ( backupStatus.backup_status == status)
				{
					return backupStatus;
				}
			}
			//Should not be here
			return null;
		}

		public byte getStatus()
		{
			return this.backup_status;
		}
	}
	
	public enum MCUBatteryLevel {
		MCU_BATTERY_INSUFFICIENT		(1),
		MCU_BATTERY_NEAR_INSUFFICIENT	(2),
		MCU_BATTERY_LOW					(3),
		MCU_BATTERY_HALF				(4),
		MCU_BATTERY_NEAR_FULL			(5),
		MCU_BATTERY_FULL				(6);
		
		private int level;
		MCUBatteryLevel(int level) {
			this.level = level;
		}
		
		public static MCUBatteryLevel findLevel(int level) {
			for (MCUBatteryLevel mcuBatteryLevel : MCUBatteryLevel.values()) {
				if (mcuBatteryLevel.getLevel() == level) {
					return mcuBatteryLevel;
				}
			}
			//Should not be here
			return null;
		}
		
		public int getLevel() {
			return this.level;
		}
	}
	
	public enum BroadcastSetting {
		BROADCAST_SETTING_OFF	((byte)0x0), 
		BROADCAST_SETTING_ON	((byte)0x1);
		
		private byte setting;
		BroadcastSetting(byte setting) {
			this.setting = setting;
		}
		
		public static BroadcastSetting findSetting(byte setting) {
			for (BroadcastSetting broadcastSetting : BroadcastSetting.values()) {
				if (broadcastSetting.getSetting() == setting) {
					return broadcastSetting;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getSetting() {
			return this.setting;
		}
	}
	
	public enum BroadcastPlatform {
		BROADCAST_PLATFORM_NONE		((byte)0x0),
		BROADCAST_PLATFORM_YOUTUBE	((byte)0x1),
		BROADCAST_PLATFORM_LL		((byte)0x2);
		
		private byte platform;
		BroadcastPlatform(byte platform) {
			this.platform = platform;
		}
		
		public static BroadcastPlatform findPlatform(byte platform) {
			for (BroadcastPlatform broadcastPlatform : BroadcastPlatform.values()) {
				if (broadcastPlatform.getPlatform() == platform) {
					return broadcastPlatform;
				}	
			}
			//Should not be here
			return null;
		}
		
		public byte getPlatform() {
			return this.platform;
		}
	}
	
	public enum BroadcastTokenType {
		TOKENTYPE_ACCESS      ((byte)0x0),
		TOKENTYPE_REFLESH     ((byte)0x1);

		private final byte token_type;
		BroadcastTokenType(byte type) {
			this.token_type = type;
		}
		
		public static BroadcastTokenType findToken(byte type) {
			for (BroadcastTokenType tokenType : BroadcastTokenType.values()) {
				if (tokenType.getType() == type) {
					return tokenType;
				}
			}
			//Should not be here
			return null; 
		}

		public byte getType() {
			return this.token_type;
		}
	}
	
	public enum BroadcastPrivacy {
		BROADCASTPRIVACY_NONPUBLIC	((byte)0x0),
		BROADCASTPRIVACY_PUBLIC		((byte)0x1);
		
		private final byte privacy;
		private BroadcastPrivacy(byte privacy) {
			this.privacy = privacy;
		}
		
		public static BroadcastPrivacy findPrivacy(byte privacy) {
			for (BroadcastPrivacy broadcastPrivacy : BroadcastPrivacy.values()) {
				if (broadcastPrivacy.getPrivacy() == privacy) {
					return broadcastPrivacy;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getPrivacy() {
			return privacy;
		}
	}
	
	public enum BroadcastStatus {
		BROADCASTSTATUS_STARTED	((byte)0x0),
		BROADCASTSTATUS_STOPPED	((byte)0x1);
		
		private final byte status;
		private BroadcastStatus(byte status) {
			this.status = status;
		}
		
		public static BroadcastStatus findStatus(byte status) {
			for (BroadcastStatus broadcastStatus : BroadcastStatus.values()) {
				if (broadcastStatus.getStatus() == status) {
					return broadcastStatus;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getStatus() {
			return status;
		}
	}
	
	public enum SimHwStatus {
		SIMHWSTATUS_PLUG_OUT		((byte)0x0),
		SIMHWSTATUS_PLUG_IN			((byte)0x1),
		SIMHWSTATUS_NO_IN_LTE_MODE	((byte)0x2);
		
		private final byte status;
		private SimHwStatus(byte status) {
			this.status = status;
		}
		
		public static SimHwStatus findStatus(byte status) {
			for (SimHwStatus simHwStatus : SimHwStatus.values()) {
				if (simHwStatus.getStatus() == status) {
					return simHwStatus;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getStatus() {
			return status;
		}
	}
	
	public enum CameraMode {
		CAMERAMODE_NORMAL		((byte)0x1),
		CAMERAMODE_SLOW_MOTION	((byte)0x2),
		CAMERAMODE_LET_MODE		((byte)0x3);
		
		private final byte mode;
		private CameraMode(byte mode) {
			this.mode = mode;
		}
		
		public static CameraMode findMode(byte mode) {
			for (CameraMode cameraMode : CameraMode.values()) {
				if (cameraMode.getMode() == mode) {
					return cameraMode;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getMode() {
			return mode;
		}
	}
	
	public enum LTECampingStatus {
		LTECAMPINGSTATUS_SUCCESS       ((byte)0x00),
		LTECAMPINGSTATUS_WRONG_APN     ((byte)0x01),
		LTECAMPINGSTATUS_CONNECT_FAIL  ((byte)0x02),
		LTECAMPINGSTATUS_UNKNOWN_ERROR ((byte)0x03);
		
		private final byte status;
		private LTECampingStatus(byte status) {
			this.status = status;
		}
		
		public static LTECampingStatus findStatus(byte status) {
			for (LTECampingStatus lteCampingStatus : LTECampingStatus.values()) {
				if (lteCampingStatus.getStatus() == status) {
					return lteCampingStatus;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getStatus() {
			return status;
		}
	}
	
	public enum SimLockType {
		SIMLOCKTYPE_UNKNOWN ((byte)0x00),
		SIMLOCKTYPE_NONE	((byte)0x01),
		SIMLOCKTYPE_PIN		((byte)0x02),
		SIMLOCKTYPE_PUK		((byte)0x03);
		
		private final byte type;
		private SimLockType(byte type) {
			this.type = type;
		}
		
		public static SimLockType findType(byte type) {
			for (SimLockType simLockType : SimLockType.values()) {
				if (simLockType.getType() == type) {
					return simLockType;
				}
			}
			//Should not be here
			return null;
		}
		
		public byte getType() {
			return type;
		}
	}
	
	
	
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
		OPERATION_GET_TIME_LAPS_SETTING,
		OPERATION_BROADCAST_START,
		OPERATION_BROADCAST_STOP,
	}

	
	
	public enum OperationEvent {

		OPEVENT_NONE,
		OPEVENT_START_CAPTURING,
		OPEVENT_COMPLETE_CAPTURING,
		OPEVENT_START_RECORDING,
		OPEVENT_STOP_RECORDING,
		OPEVENT_COMPLETE_RECORDING,
		OPEVENT_TIME_LAPSE_RECORDING_START,
		OPEVENT_TIME_LAPSE_CAPTURE_ONE,
		OPEVENT_TIME_LAPSE_RECORDING_PAUSE,
		OPEVENT_TIME_LAPSE_RECORDING_RESUME,
		OPEVENT_TIME_LAPSE_RECORDING_STOP,
		OPEVENT_TIME_LAPSE_COMPLETE_RECORDING,
		OPEVENT_GET_TIMELAPSE_SETTING,
		OPEVENT_GET_FREE_SPACE,
		OPEVENT_GET_DR_STATUS
	}
	
	
	/// Callback
	public static final int CB_OPEN_RESULT 									= 7000;
	public static final int CB_CLOSE_RESULT 								= 7001;
	public static final int CB_LONG_TERM_EVENT_RESULT						= 7002;
	public static final int CB_PERFORMANCE_RESULT							= 7003;
	public static final int CB_BLE_SCAN_RESULT 								= 8000;
	public static final int CB_CREATE_WIFI_P2P_GROUP_RESULT 				= 8001;
	public static final int CB_REMOVE_WIFI_P2P_GROUP_RESULT 				= 8002;
	public static final int CB_REMOVE_WIFI_P2P_GROUP_FORCE_RESULT 			= 8003;
	public static final int CB_BLE_CONNECT_RESULT							= 8100;
	public static final int CB_BLE_DISCONNECT_RESULT						= 8101;
	public static final int CB_BLE_DISCONNECT_FORCE_RESULT					= 8102;
	public static final int CB_WIFI_CONNECT_RESULT							= 8200;
	public static final int CB_WIFI_DISCONNECT_RESULT						= 8201;
	public static final int CB_SET_POWER_ONOFF_RESULT						= 8300;
	public static final int CB_GET_POWER_ONOFF_RESULT						= 8301;
	public static final int CB_SET_HW_STATUS_LTEVENT_RESULT					= 8302;
	public static final int CB_CLR_HW_STATUS_LTEVENT_RESULT					= 8303;
	public static final int CB_GET_HW_STATUS_RESULT							= 8304;
	public static final int CB_SET_FLIGHT_MODE_RESULT						= 8305;
	public static final int CB_SET_AUTOSLEEP_TIMER_OFFSET_RESULT			= 8306;
	public static final int CB_TRIGGER_FWUPDATE_RESULT						= 8307;
	public static final int CB_FWUPDATE_RESULT								= 8308;
	public static final int CB_SET_SIM_HW_STATUS_LTEVENT_RESULT				= 8309;
	public static final int CB_CLR_SIM_HW_STATUS_LTEVENT_RESULT				= 8310;
	public static final int CB_GET_SIM_HW_STATUS_RESULT						= 8311;
	public static final int CB_SET_OPERATION_LTEVENT_RESULT					= 8400;
	public static final int CB_CLR_OPERATION_LTEVENT_RESULT					= 8401;
	public static final int CB_SET_OPERATION_RESULT							= 8402;
	public static final int CB_SET_CAMERA_MODE_LTEVENT_RESULT				= 8403;
	public static final int CB_CLR_CAMERA_MODE_LTEVENT_RESULT				= 8404;
	public static final int CB_SET_CAMERA_MODE_RESULT						= 8405;
	public static final int CB_GET_CAMERA_MODE_RESULT						= 8406;
	public static final int CB_SET_DATE_TIME_RESULT							= 8500;
	public static final int CB_SET_NAME_RESULT								= 8501;
	public static final int CB_GET_NAME_RESULT								= 8502;
	public static final int CB_SET_GPS_INFO_LTEVENT_RESULT					= 8503;
	public static final int CB_CLR_GPS_INFO_LTEVENT_RESULT					= 8504;
	public static final int CB_SET_GPS_INFO_RESULT							= 8505;
	public static final int CB_GET_BLE_FW_VERSION_RESULT					= 8600;
	public static final int CB_GET_ALL_FW_VERSION_RESULT					= 8601;
	public static final int CB_VERIFY_PASSWORD_RESULT						= 8700;
	public static final int CB_CHANGE_PASSWORD_RESULT						= 8701;
	public static final int CB_SET_AUTO_BACKUP_LTEVENT_RESULT				= 8800;
	public static final int CB_CLR_AUTO_BACKUP_LTEVENT_RESULT				= 8801;
	public static final int CB_SET_AUTO_BACKUP_AP_RESULT					= 8802;
	public static final int CB_CLR_AUTO_BACKUP_AP_RESULT					= 8803;
	public static final int CB_SET_AUTO_BACKUP_PROXY_RESULT					= 8804;
	public static final int CB_GET_AUTO_BACKUP_PROXY_RESULT					= 8805;
	public static final int CB_SET_AUTO_BACKUP_AP_SCAN_START_RESULT			= 8806;
	public static final int CB_SET_AUTO_BACKUP_AP_SCAN_STOP_RESULT			= 8807;
	public static final int CB_GET_AUTO_BACKUP_STATUS_RESULT				= 8808;
	public static final int CB_SET_AUTO_BACKUP_TOKEN_RESULT					= 8809;
	public static final int CB_SET_AUTO_BACKUP_PREFERENCE_RESULT			= 8810;
	public static final int CB_GET_AUTO_BACKUP_PREFERENCE_RESULT			= 8811;
	public static final int CB_GET_AUTO_BACKUP_IS_AVAILABLE_RESULT			= 8812;
	public static final int CB_SET_AUTO_BACKUP_ACCOUNT_RESULT				= 8813;
	public static final int CB_GET_AUTO_BACKUP_ACCOUNT_RESULT				= 8814;
	public static final int CB_SET_METADATA_LTEVENT_RESULT					= 8900;
	public static final int CB_CLR_METADATA_LTEVENT_RESULT					= 8901;
	public static final int CB_SET_CAMERA_ERROR_LTEVENT_RESULT				= 9000;
	public static final int CB_CLR_CAMERA_ERROR_LTEVENT_RESULT				= 9001;
	public static final int CB_SET_LTNOTIFY_RESULT							= 9100;
	public static final int CB_CLR_LTNOTIFY_RESULT							= 9101;
	public static final int CB_SET_BROADCAST_SETTING_RESULT					= 9200;
	public static final int CB_GET_BROADCAST_SETTING_RESULT					= 9201;
	public static final int CB_SET_BROADCAST_PLATFORM_RESULT				= 9202;
	public static final int CB_SET_BROADCAST_INVITATION_LIST_RESULT			= 9203;
	public static final int CB_SET_BROADCAST_PRIVACY_RESULT					= 9204;
	public static final int CB_GET_BROADCAST_STATUS_RESULT					= 9205;
	public static final int CB_GET_BROADCAST_INVITATION_LIST_RESULT			= 9206;
	public static final int CB_GET_BROADCAST_PRIVACY_RESULT					= 9207;
	public static final int CB_GET_BROADCAST_PLATFORM_RESULT				= 9208;
	public static final int CB_GET_BROADCAST_VIDEO_URL_RESULT				= 9209;
	public static final int CB_GET_BROADCAST_ERROR_LIST_RESULT				= 9210;
	public static final int CB_SET_BROADCAST_USER_NAME_RESULT				= 9211;
	public static final int CB_SET_BROADCAST_SMS_CONTENT_RESULT				= 9212;
	public static final int CB_GET_BROADCAST_USER_NAME_RESULT				= 9213;
	public static final int CB_GET_BROADCAST_SMS_CONTENT_RESULT				= 9214;
	public static final int CB_SET_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT	= 9300;
	public static final int CB_CLR_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT	= 9301;
	public static final int CB_GET_LTE_CAMPING_STATUS_RESULT				= 9400;
	public static final int CB_SET_LTE_CAMPING_STATUS_LTEVENT_RESULT		= 9401;
	public static final int CB_CLR_LTE_CAMPING_STATUS_LTEVENT_RESULT		= 9402;
	public static final int CB_GET_MODEM_STATUS_RESULT						= 9500;
	public static final int CB_UNLOCK_SIM_PIN_RESULT						= 9501;
	
	
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
	public static final String PARAM_SIM_HW_STATUS						= "sim_hw_status";
	public static final String PARAM_GC_POWER							= "gc_power";
	public static final String PARAM_OPERATION							= "operation";
	public static final String PARAM_OPERATION_ERROR_CODE				= "operation_error_code";
	public static final String PARAM_CAMERA_MODE						= "camera_mode";
	public static final String PARAM_A12_FW_VERSION						= "a12_fw_version";
	public static final String PARAM_MOD_FW_VERSION						= "modem_fw_version";
	public static final String PARAM_MCU_FW_VERSION						= "mcu_fw_version";
	public static final String PARAM_BLE_FW_VERSION						= "ble_fw_version";
    public static final String PARAM_TRIGGER_FWUPDATE_RESULT			= "trigger_fw_update_result";
    public static final String PARAM_FWUPDATE_RESULT					= "fw_update_result";
	public static final String PARAM_VERIFY_PASSWORD_STATUS				= "verify_password_status";
	public static final String PARAM_AUTO_BACKUP_ERROR_TYPE				= "auto_backup_error_type";
	public static final String PARAM_AUTO_BACKUP_ERROR_CODE				= "auto_backup_error_code";
	public static final String PARAM_AUTO_BACKUP_PROCESS_STATUS			= "auto_backup_process_status";
	public static final String PARAM_AUTO_BACKUP_PROVIDER_INDEX			= "auto_backup_provider_index";
	public static final String PARAM_AUTO_BACKUP_UNBACKUP_ITEM_NUMBER	= "auto_backup_unbackup_item_number";
	public static final String PARAM_AUTO_BACKUP_TOTAL_ITEM_NUMBER		= "auto_backup_total_item_number";
	public static final String PARAM_AUTO_BACKUP_LAST_BACKUP_DATE_TIME	= "auto_backup_last_backup_date_time";
	public static final String PARAM_AUTO_BACKUP_IS_ENABLE_BACKUP		= "auto_backup_is_enable_backup";
	public static final String PARAM_AUTO_BACKUP_IS_DELETE_AFTER_BACKUP	= "auto_backup_is_delete_after_backup";
	public static final String PARAM_AUTO_BACKUP_IS_BACKUP_WITHOUT_AC	= "auto_backup_is_backup_without_ac";
	public static final String PARAM_AUTO_BACKUP_IS_AVAILABLE			= "auto_backup_is_available";
	public static final String PARAM_AUTO_BACKUP_ACCOUNT				= "auto_backup_account";
	public static final String PARAM_AUTO_BACKUP_ERROR2_MESSAGE			= "auto_backup_error2_message";
	public static final String PARAM_BROADCAST_ERROR_CODE				= "broadcast_error_code";
	public static final String PARAM_BROADCAST_ERROR_TIMESTAMP			= "broadcast_error_timestamp";
	public static final String PARAM_BROADCAST_SETTING					= "broadcast_setting";
	public static final String PARAM_BROADCAST_STATUS					= "broadcast_status";
	public static final String PARAM_BROADCAST_INVITATION_LIST			= "broadcast_invitation_list";
	public static final String PARAM_BROADCAST_PRIVACY					= "broadcast_privacy";
	public static final String PARAM_BROADCAST_PLATFORM					= "broadcast_platform";
	public static final String PARAM_BROADCAST_VIDEO_URL				= "broadcast_video_url";
	public static final String PARAM_BROADCAST_ERROR_LIST				= "broadcast_error_list";
	public static final String PARAM_BROADCAST_USER_NAME				= "broadcast_user_name";
	public static final String PARAM_BROADCAST_SMS_CONTENT				= "broadcast_sms_content";
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
	public static final String PARAM_AP_ANY_SCAN_RESULT					= "ap_any_scan_result";
	public static final String PARAM_AP_END_OF_SCAN_LIST				= "ap_end_of_scan_list";
	public static final String PARAM_AP_INDEX_OF_SCAN_LIST				= "ap_index_of_scan_list";
	public static final String PARAM_AP_RSSI							= "ap_rssi";
	public static final String PARAM_AP_SECURITY						= "ap_security";
	public static final String PARAM_AP_AUTHORIZATION					= "ap_authorization";
	public static final String PARAM_AP_SSID							= "ap_ssid";
	public static final String PARAM_AP_PROXY							= "ap_proxy";
	public static final String PARAM_AP_PORT							= "ap_port";
	public static final String PARAM_SMS_DATE_TIME						= "sms_date_time";
	public static final String PARAM_SMS_PHONE_NUMBER					= "sms_phone_number";
	public static final String PARAM_SMS_MESSAGE_CONTENT				= "sms_message_content";
	public static final String PARAM_LTE_CAMPING_STATUS					= "lte_camping_status";
	public static final String PARAM_SIM_LOCK_TYPE						= "sim_lock_type";
	public static final String PARAM_SIM_PIN_RETRY_COUNT				= "sim_pin_retry_count";
	public static final String PARAM_SIM_PUK_RETRY_COUNT				= "sim_puk_retry_count";
	public static final String PARAM_SIM_UNLOCK_PIN_RESULT				= "sim_unlock_pin_result";
	
	
	
	/// Interfaces
	public boolean gcBootUp(BluetoothDevice device);
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
	public boolean gcTriggerFWUpdate(BluetoothDevice device, boolean update_rtos, boolean update_modem, boolean update_mcu, String firmwareVersion);
	public boolean gcSetHwStatusLTEvent(BluetoothDevice device);
	public boolean gcClrHwStatusLTEvent(BluetoothDevice device);
	public boolean gcGetHwStatus(BluetoothDevice device);
	public boolean gcGetSimHwStatus(BluetoothDevice device);
	public boolean gcSetFlightMode(BluetoothDevice device);
	public boolean gcSetAutoSleepTimerOffset(BluetoothDevice device, int offset_sec);
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
	public boolean gcSetCameraModeLTEvent(BluetoothDevice device);
	public boolean gcClrCameraModeLTEvent(BluetoothDevice device);
	public boolean gcSetCameraMode(BluetoothDevice device, CameraMode mode);
	public boolean gcGetCameraMode(BluetoothDevice device);
	public boolean gcSetMetadataLTEvent(BluetoothDevice device);
	public boolean gcClrMetadataLTEvent(BluetoothDevice device);
	public boolean gcSetCameraErrorLTEvent(BluetoothDevice device);
	public boolean gcClrCameraErrorLTEvent(BluetoothDevice device);
	public boolean gcSoftAPConnect(BluetoothDevice device, String passwd);
	public boolean gcSetAutoBackupLTEvent(BluetoothDevice device);
	public boolean gcClrAutoBackupLTEvent(BluetoothDevice device);
	public boolean gcSetAutoBackupAP(BluetoothDevice device, String ssid, String passwd, byte security);
	public boolean gcClrAutoBackupAP(BluetoothDevice device, byte security, String ssid);
	public boolean gcSetLTNotify(BluetoothDevice device);
	public boolean gcClrLTNotify(BluetoothDevice device);
	public boolean gcSetAutoBackupToken(BluetoothDevice device, BackupProviderIdIndex pidx, BackupTokenType type, String token);
	public boolean gcSetAutoBackupAPScan(BluetoothDevice device, int startORstop , int option);
	public boolean gcSetAutoBackupProxy(BluetoothDevice device, int port, byte security, String ssid, String proxy);
	public boolean gcGetAutoBackupProxy(BluetoothDevice device, byte security, String ssid);
	public boolean gcGetAllFwVersion(BluetoothDevice device);
	public boolean gcGetAutoBackupStatus(BluetoothDevice device);
	public boolean gcGetAutoBackupIsAvailable(BluetoothDevice device);
	public boolean gcSetAutoBackupAccount(BluetoothDevice device, String name);
	public boolean gcGetAutoBackupAccount(BluetoothDevice device);
	public boolean gcSetAutoBackupPreference(BluetoothDevice device, boolean enableBackup, boolean deleteAfterBackup, boolean backupWithoutAC);
	public boolean gcGetAutoBackupPreference(BluetoothDevice device);
	public boolean gcSetBroadcastSetting(BluetoothDevice device, BroadcastSetting setting);
	public boolean gcGetBroadcastSetting(BluetoothDevice device);
	public boolean gcSetBroadcastPlatform(BluetoothDevice device, BroadcastPlatform platform, BroadcastTokenType tokenType, String token);
	public boolean gcSetBroadcastInvitationList(BluetoothDevice device, List<String> invitationList);
	public boolean gcSetBroadcastPrivacy(BluetoothDevice device, BroadcastPrivacy privacy);
	public boolean gcGetBroadcastStatus(BluetoothDevice device);
	public boolean gcGetBroadcastInvitationList(BluetoothDevice device);
	public boolean gcGetBroadcastPrivacy(BluetoothDevice device);
	public boolean gcGetBroadcastPlatform(BluetoothDevice device);
	public boolean gcGetBroadcastVideoUrl(BluetoothDevice device);
	public boolean gcGetBroadcastErrorList(BluetoothDevice device);
	public boolean gcSetBroadcastUserName(BluetoothDevice device, String userName);
	public boolean gcSetBroadcastSMSContent(BluetoothDevice device, String smsContent);
	public boolean gcGetBroadcastUserName(BluetoothDevice device);
	public boolean gcGetBroadcastSMSContent(BluetoothDevice device);
	public boolean gcSetGeneralPurposeCommandLTNotify(BluetoothDevice device);
	public boolean gcClrGeneralPurposeCommandLTNotify(BluetoothDevice device);
	public boolean gcGetLTECampingStatus(BluetoothDevice device);
	public boolean gcSetLTECampingStatusLTEvent(BluetoothDevice device);
	public boolean gcClrLTECampingStatusLTEvent(BluetoothDevice device);
	public boolean gcGetModemStatus(BluetoothDevice device);
	public boolean gcUnlockSimPin(BluetoothDevice device, String pinCode);
}
