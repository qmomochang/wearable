package com.htc.gc.interfaces;

import java.nio.ByteBuffer;

import android.util.Log;

import com.htc.gc.interfaces.IGCService.ConnectionMode;

public interface Common {

	public static final String TAG = "GCService";
	public static final boolean DEBUG = true;
	public static final boolean DUMP_SENSITIVEDATA = true;

	public static final String GC_DEFAULT_PASSWORD = "00000000";
	
	public static final int ENABLE_REQUESTCALLBACK_RESPONSE_VERSION = 7550;
	
	public static final int READY_NONE 			= 0x00000000;
	public static final int READY_STILL 		= 0x00000001;
	public static final int READY_RESERVED	 	= 0x00000002;
	public static final int READY_VIDEO 		= 0x00000004;
	public static final int READY_ALL 			= READY_STILL | READY_RESERVED | READY_VIDEO;
	public static final int READY_TIMELAPSE		= READY_ALL;

	public static final int ERR_MODULE_BATTERY				= 0x01;
	public static final int	ERR_MODULE_CAPTURE				= 0x02;
	public static final int	ERR_MODULE_VIDEO				= 0x03;
	public static final int ERR_MODULE_CMOS					= 0x04;
	public static final int ERR_MODULE_CARD					= 0x05;
	public static final int ERR_MODULE_NOCARD				= 0x07;
	
	public static final int FW_FUNC_SUPPORT_AUTOBACKUP		= 0x00000001;
	public static final int	FW_FUNC_SUPPORT_BROADCASTING	= 0x00000002;
	
	public enum ErrorCode {
		ERR_SUCCESS									(0x00),
		ERR_INVALID_PARAMETER 						(0x07),
		ERR_FAIL									(0x0A),
		ERR_OPEN_FILE_FAIL 							(0x0B),
		ERR_READ_FILE_FAIL 							(0x0C),
		ERR_REQUEST_MEM_FAIL 						(0x0D),
		ERR_SEND_WIFI_EVENT_FAIL					(0x0E),
		ERR_DEVICE_NOT_READY 						(0x10),
		ERR_DEVICE_BUSY 							(0x11),
		ERR_IMCOMPLETE_TRANSFER						(0x12),
		ERR_CAPTURE_GET_QV_FAIL						(0x13),
		ERR_SD_CAPACITY_UNKNOWN						(0x14),
		ERR_INVALID_OBJECT_HANDLE 					(0x15),
		ERR_GET_THUMB_FAIL							(0x16),
		ERR_GET_HD_IMG_FAIL							(0x17),
		ERR_UPGRADE_FW_NOT_FOUND					(0x18),
		ERR_UPGRADE_FW_INVALIDE						(0x19),
		ERR_GET_OBJINFO_FAIL						(0x1A),
		ERR_INVALID_MODE 							(0x1B),
		ERR_SAME_MODE		 						(0x1C),
		ERR_GET_FILE_NOT_READY						(0x1D),
		ERR_SD_CAPACITY_FULL 						(0x1E),
		ERR_ABORT 									(0x1F),
		ERR_RECORD_SLOW_CARD						(0x20),
		ERR_RECORD_WRITE_FAIL						(0x21),
		ERR_UPGRADE_VERSION_NOT_MATCH				(0x23),
		ERR_NETDB_REQUEST_FAIL						(0x24),
		ERR_VIDEO_SEEK_FAIL							(0x25),
		ERR_NETDB_NOT_READY							(0x26),
		ERR_UPGRADE_BATTERY_LEVEL_FAIL				(0x27),
		ERR_UPGRADE_MCU_VERSION_NOT_MATCH			(0x28),
		ERR_UPGRADE_MCU_INVALID						(0x29),
		ERR_UPGRADE_BOOT_NOT_FOUND					(0x2A),
		ERR_UPGRADE_BOOT_VERSION_NOT_MATCH			(0x2B),
		ERR_UPGRADE_BOOT_INVALID					(0x2C),
		ERR_UPGRADE_BLE_INVALID						(0x2D),
		ERR_RAWDATA_DOWNLOADFAIL					(0x2E),
		ERR_RECORD_COMPRESSING_FAIL					(0x2F),
		ERR_EVENT_QUEUE_FULL						(0x30),
		ERR_NO_SD_CARD								(0x31),
		ERR_TIMELAPSE_LOW_BAT						(0x32),
		ERR_NOT_AUTOSAVE							(0x33),
		ERR_SOCKETCLOSE								(0x34),
		ERR_UNAVAILABLE_PORC_RECORDING				(0x35),
		ERR_DARK_PROTECTION							(0x36),
		ERR_NOT_PROVIDE_AUTOBACKUPLIB				(0x37),
		
		// GCSerivce define
		ERR_INVALID_SSID							(0xA0),
		ERR_INVALID_KEY								(0xA1),
		ERR_GET_CREDENTIAL_FAILED					(0xA2),
		
		ERR_SYSTEM_ERROR 							(0xFF);
		
		private final int mVal;
		ErrorCode(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static ErrorCode getKey(int val) {
			for(ErrorCode res : ErrorCode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			
			Log.e(Common.TAG, "[GCServide] Undefined ErrorCode: "+val);
			return ERR_SYSTEM_ERROR;
		}
	}
	
	public enum ConnectionErrorCode {
		WIFIMGR_ERR_SUCCESS						(0x00),
		WIFIMGR_ERR_WIFI_INIT_FAILED			(0x01),
		WIFIMGR_ERR_CONNECT_AP_FAILED			(0x02),
		WIFIMGR_ERR_CONNECT_TIMEOUT				(0x03),
		WIFIMGR_ERR_NO_INTERNET					(0x04),
		WIFIMGR_ERR_CANNOT_GET_IP				(0x05),
		WIFIMGR_ERR_NOT_CONNECTED				(0x06),
		WIFIMGR_ERR_SEARCH_AP_FAILED			(0x07),
		WIFIMGR_ERR_TRANS_FILE_FAILED			(0x08),
		WIFIMGR_ERR_CANNOT_RECONNECT			(0x09),
		WIFIMGR_ERR_START_AP_FAILED				(0x0A),
		WIFIMGR_ERR_INVALID_PARAMETER			(0x0B),
		WIFIMGR_ERR_CAPTIVE_PORTAL_NETWORK		(0x0C),
		WIFIMGR_ERR_MEMORY_INSUFFICIENCY		(0x0D),
		WIFIMGR_ERR_WPS_INVALID_PIN				(0x0E),
		WIFIMGR_ERR_WPS_INVALID_AP				(0x0F),
		WIFIMGR_ERR_WPS_OPEN_FAIL				(0x10),
		WIFIMGR_ERR_WPS_GET_CREDENTIAL_FAILED	(0x11),
		WIFIMGR_ERR_WPS_FIND_AP_FAILED			(0x12),
		WIFIMGR_ERR_WPS_FIND_AP_PBC_OVERLAP		(0x13),
		WIFIMGR_ERR_WPS_FAILED					(0x14),
		WIFIMGR_ERR_BAD_SSID					(0x15),
		WIFIMGR_ERR_BAD_SSID_LENGTH				(0x16),
		WIFIMGR_ERR_BAD_KEY						(0x17),
		WIFIMGR_ERR_BAD_KEY_LENGTH				(0x18),
		WIFIMGR_ERR_BAD_ENCRYPTION				(0x19),
		WIFIMGR_ERR_ASSOCIATION_FAILED			(0x1A),
		WIFIMGR_ERR_SEMA_WAIT_FAIL				(0x1B),
		WIFIMGR_ERR_SEMA_POST_FAIL				(0x1C),
		WIFIMGR_ERR_WIFI_BUSY					(0x1D),
		WIFIMGR_ERR_FAIL						(0x1E),
		ERROR_FAIL								(0x90),
		ERROR_P2P_GROUP							(0x91),
		ERROR_GATT_READ							(0xA0),
		ERROR_GATT_WRITE						(0xA1),
		ERROR_GATT_SET_NOTIFICATION				(0xA2),
		ERROR_GATT_RECEIVE_NOTIFICATION			(0xA3),
		BLE_DISCONNECT_FROM_GATT_SERVER			(0xB0),
		BLE_CONNECT_FAIL						(0xB1),
		BLE_DISCONNECT_FAIL						(0xB2),
		BLE_COMMON_ERROR						(0xBF),
		WIFI_UNEXPECTED_DISCONNECT				(0xC0),
		SOCKET_HEARTBEAT_DEAD					(0xC1),
		SOCKET_COMMAND_NO_RESPONSE				(0xC2),
		SOCKET_EXCEPTION						(0xC3),
		SOCKET_COMMON_ERROR						(0xCF),
		SOCKET_CONNECT_FAIL						(0xD0),
		WIFI_DISCONNECT_BY_GC_POWER_OFF			(0xD1),
		WIFI_DISCONNECT_BY_GC_DISCONNECT_SOCKET (0xD2),
		WIFI_DISCONNECT_BY_GC_WIFI_UNREACHABLE	(0xD3),
		
		ERROR_UNKNOWN							(0xFF);
		
		private final int mVal;
		ConnectionErrorCode(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static ConnectionErrorCode getKey(int val) {
			for(ConnectionErrorCode res : ConnectionErrorCode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			
			Log.e(Common.TAG, "[GCServide] Undefined ConnectionErrorCode: "+val);
			return ERROR_UNKNOWN;
		}
		
		public boolean isBleSlientReconnect() {
			if(mVal == BLE_DISCONNECT_FROM_GATT_SERVER.getVal() || 
				mVal == BLE_CONNECT_FAIL.getVal()) {
				return true;
			} else {
				return false;
			}
		}
		
		public boolean isSocketSlientReconnect() {
			if(mVal == SOCKET_HEARTBEAT_DEAD.getVal() ||
				mVal == SOCKET_COMMAND_NO_RESPONSE.getVal()) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public enum GC2WifiMgrErrorCode {
		WIFIMGR_ERR_SUCCESS						(0x00),
		WIFIMGR_ERR_FAIL						(0x01),
		WIFIMGR_ERR_WIFI_INIT_FAILED			(0x02),
		WIFIMGR_ERR_WIFI_BUSY					(0x03),
		WIFIMGR_ERR_BAD_SSID					(0x04),
		WIFIMGR_ERR_BAD_SSID_LENGTH				(0x05),
		WIFIMGR_ERR_BAD_KEY						(0x06),
		WIFIMGR_ERR_BAD_KEY_LENGTH				(0x07),
		WIFIMGR_ERR_BAD_KEYMGMT					(0x08),
		WIFIMGR_ERR_CAN_NOT_FIND_AP				(0x09),
		WIFIMGR_ERR_SEARCH_AP_TIMEOUT			(0x0A),
		WIFIMGR_ERR_AUTH_NO_PASSWORD			(0x0B),
		WIFIMGR_ERR_AUTH_PASSWORD_NOMATCH		(0x0C),
		WIFIMGR_ERR_AUTH_REQ_TIMEOUT			(0x0D),
		WIFIMGR_ERR_AUTH_RSP_TIMEOUT			(0x0E),
		WIFIMGR_ERR_ASOCIATE_REQ_TIMEOUT		(0x0F), 
		WIFIMGR_ERR_ASOCIATE_RSP_TIMEOUT		(0x10), 
		WIFIMGR_ERR_HANDSHAKE_REQ_TIMEOUT		(0x11),
		WIFIMGR_ERR_HANDSHAKE_REQ_CONF_TIMEOUT	(0x12),
		WIFIMGR_ERR_HANDSHAKE_RSP_TIMEOUT		(0x13),
		WIFIMGR_ERR_HANDSHAKE_RSP_CONF_TIMEOUT	(0x14),
		WIFIMGR_ERR_GET_IP_FAIL					(0x15),
		WIFIMGF_ERR_GET_IP_TIMEOUT				(0x16),
		WIFIMGR_ERR_START_AP_FAILED				(0x17),
		WIFIMGR_ERR_START_DHCP_FAILED			(0x18),
		
		WIFIMGR_ERR_UNKNOWN						(0xFF);
		
		private final int mVal;
		GC2WifiMgrErrorCode(int val) { mVal = val; }
		public int getVal() { return mVal; } 
		public static GC2WifiMgrErrorCode getKey(int val) {
			for(GC2WifiMgrErrorCode errorCode : GC2WifiMgrErrorCode.values()) {
				if(errorCode.getVal() == val) {
					return errorCode;
				}
			}
			
			Log.e(Common.TAG, "[GCServide] Undefined GC2WifiMgrErrorCode: "+val);
			return WIFIMGR_ERR_UNKNOWN;
		}
	}

	public enum Mode {
		None,
		Browse,
		Control,
	}

	public enum Context {
		None,
		Capturing,
		TimeLapse,
		Recording,
		SlowMotion,
	}
	
	public enum Filter {
		ALL			((byte)0x0),
		IMAGE		((byte)0x1),
		VIDEO		((byte)0x2),
		TIMELAPSE	((byte)0x3),
		UNBACKUP	((byte)0x4);
		
		private final byte mVal;
		Filter(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static Filter getKey(byte val) throws Common.NoImpException {
			for(Filter res : Filter.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum DeviceVersion {
		Unknown,
		GC1,
		GC2
	}

	public interface ICancelable {
		void cancel(OperationCallback allback);
	}

	public interface ErrorCallback {
		void error(Exception e);
	}

	public interface OperationCallback extends ErrorCallback {
		void done(Object that);
	}

	public interface DataCallback extends ErrorCallback {
		void data(ByteBuffer buffer);
		void cancel();
		void end();
	}
	
	public interface UploadCallback extends ErrorCallback {
		void progress(long bytesSent, long bytesTotal);
		void cancel();
		void end();		
	}
	
	public interface RequestCallback extends ErrorCallback {
		void requested(Object that);
		void done(Object that);
	}

	public static class CommonException extends Exception {
		private static final long serialVersionUID = 8950607718347686669L;
		
		private final ErrorCode mErrorCode;

		public String toString() {
			return "Message: " + getMessage() + ", error code: " + mErrorCode.toString() + "(0x" + Integer.toHexString(mErrorCode.getVal()) + ")";
		}

		public CommonException(String message, ErrorCode errorCode) {
			super(message);
			mErrorCode = errorCode;
		}

		public ErrorCode getErrorCode() {
			return mErrorCode;
		}
	}
	
	public static class CancelException extends Exception {
		private static final long serialVersionUID = 8950607718347686670L;
	}

	public static class StatusException extends Exception {
		private static final long serialVersionUID = 8950607718347686671L;
	}

	public static class ModeException extends Exception {
		private static final long serialVersionUID = 8950607718347686672L;
	}

	public static class NotReadyException extends Exception {
		private static final long serialVersionUID = 8950607718347686673L;
	}

	public static class NoImpException extends Exception {
		private static final long serialVersionUID = 8950607718347686674L;
	}

	public static class CursorInvalidException extends Exception {
		private static final long serialVersionUID = 8950607718347686675L;
	}
	
	public static class ConnectionException extends Exception {
		private static final long serialVersionUID = 8950607718347686676L;
		
		private final ConnectionMode mCurrentMode;
		private final int mErrorCode;
		private final String mDescription;
		private final boolean mIsSilentReconnectSocket;
		private final boolean mIsSilentReconnectBle;
		
		public ConnectionException(ConnectionMode currentMode, int errorCode, String description, boolean isSlientReconnectSocket, boolean isSlientReconnectBle) {
			super(description);
			
			mCurrentMode = currentMode;
			mErrorCode = errorCode;
			mDescription = description;
			mIsSilentReconnectSocket = isSlientReconnectSocket;
			mIsSilentReconnectBle = isSlientReconnectBle;
		}

		public ConnectionMode getCurrentMode() {
			return mCurrentMode;
		}
			
		public int getErrorCode() {
			return mErrorCode;
		}
		
		public String getDescription() {
			return mDescription;
		}
		
		@Deprecated
		public boolean isSilentReconnectSocket() {
			return mIsSilentReconnectSocket;
		}
		
		public boolean isSlientReconnectBle() {
			return mIsSilentReconnectBle;
		}
	}
	
	public static class AuthException extends Exception {
		private static final long serialVersionUID = 8950607718347686677L;
		
		private final Boolean mDefaultPassword;
		
		public AuthException(Boolean defaultPassword) {
			super("Default Password:"+defaultPassword.toString());	
			
			mDefaultPassword = defaultPassword;
		}
		
		public Boolean isDefaultPassword() {
			return mDefaultPassword;
		}
	}
	
	public static class BleCommandException extends Exception {
		private static final long serialVersionUID = 8950607718347686678L;
	}
	
	public static class InvalidArgumentsException extends Exception {
		private static final long serialVersionUID = 8950607718347686679L;
		
		public InvalidArgumentsException(String message) {
			super(message);
		}
	}
	
	public static class ScanBleException extends Exception {
		private static final long serialVersionUID = 8950607718347686680L;
	}
}
