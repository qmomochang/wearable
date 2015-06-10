package com.htc.gc.interfaces;

import java.util.ArrayList;
import java.util.Calendar;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.interfaces.Common.ErrorCallback;
import com.htc.gc.interfaces.Common.OperationCallback;


public interface IAutoBackuper {
	public static final int SSID_LEN = 40;
	public static final int KEY_LEN = 64;
	public static final int RSSI_LEN = 2;
	public static final int PROXY_NAME_LEN = 256;
	public static final int ACCOUNT_LEN = 256;
	
	public static final byte AUTHORIZATED = 0x01;
	
	public enum ProviderType {
		PROVIDER_NONE 			((byte) 0x0),
		PROVIDER_DROPBOX 		((byte) 0x1),
		PROVIDER_GOOGLE 		((byte) 0x2),
		PROVIDER_AUTOSAVE		((byte) 0x3),
		PROVIDER_BAIDU			((byte) 0x4),
		//PROVIDER_FACTORYRESET	((byte) 0x5), // only for GC internal usage
		PROVIDER_DUMMY			((byte) 0xFF);
		
		private final byte mVal;
		ProviderType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static ProviderType getKey(byte val) throws Common.NoImpException {
			for(ProviderType res : ProviderType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum TokenType {
		TOKENTYPE_ACCESS 		((byte) 0x0),
		TOKENTYPE_REFRESH 		((byte) 0x1),
		TOKENTYPE_CLIENTID		((byte) 0x2),
		TOKENTYPE_CLIENTSECRET	((byte) 0x3);
		
		private final byte mVal;
		TokenType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static TokenType getKey(byte val) throws Common.NoImpException {
			for(TokenType res : TokenType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum SecurityType {
		SECURITY_OPEN 				((byte) 0x0),
		SECURITY_WEP 				((byte) 0x1),
		SECURITY_WPA				((byte) 0x2),
		SECURITY_WPA2				((byte) 0x4),
		SECURITY_WPA_AES			((byte) 0x5),
		SECURITY_WPA2_TKIP			((byte) 0x6),
		SECURITY_WPA_ENTERPRISE		((byte) 0x7),
		SECURITY_WPA2_ENTERPRISE	((byte) 0x8),
		SECURITY_UNKNOWN			((byte) 0x9);
		
		private final byte mVal;
		SecurityType(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static SecurityType getKey(byte val) throws Common.NoImpException {
			for(SecurityType res : SecurityType.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}
	}
	
	public enum OptionCheck {
		CHECK_OFF 		((byte) 0x0),
		CHECK_ON 		((byte) 0x1);
		
		private final byte mVal;
		OptionCheck(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static OptionCheck getKey(byte val) throws Common.NoImpException {
			for(OptionCheck res : OptionCheck.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}		
	}
	
	public enum ScanMode {
		SCAN_MODE_ALL_INTEGRATE_SAVED_LIST 		((byte) 0x0),
		SCAN_MODE_ALL 							((byte) 0x1),
		SCAN_MODE_SAVED_LIST					((byte) 0x2),
		SCAN_MODE_ONLY_SAVED					((byte) 0x3);
		
		private final byte mVal;
		ScanMode(byte val) { mVal = val; }
		public byte getVal() { return mVal; } 
		public static ScanMode getKey(byte val) throws Common.NoImpException {
			for(ScanMode res : ScanMode.values()) {
				if(res.getVal() == val) {
					return res;
				}
			}
			throw new Common.NoImpException();
		}	
	}

	public class WifiApInfo {
		private final String mSSID;
		private final short mRSSI;
		private final SecurityType mSecurity;
		private final boolean mIsAuthorized;
		
		public WifiApInfo(String ssid, short rssi, SecurityType security, boolean isAuthorized) {
			mSSID = ssid;
			mRSSI = rssi;
			mSecurity = security;
			mIsAuthorized = isAuthorized;
		}
		
		public String getSSID() {
			return mSSID;
		}
		
		public short getRSSI() {
			return mRSSI;
		}
		
		public SecurityType getSecurity() {
			return mSecurity;
		}
		
		public boolean isAuthorized() {
			return mIsAuthorized;
		}
		
	}
	
	public enum AutoBackupError2 {
		AUTO_BACKUP_ERROR2_SUCCESS,
		AUTO_BACKUP_ERROR2_AUTO_TOKEN_INVALID,
		AUTO_BACKUP_ERROR2_USER_OUT_OF_SPACE,
		AUTO_BACKUP_ERROR2_UNKNOWN,
	}
	
	public interface AutoBackupHotspotRequestListener {
		public void onSwitch(boolean on);
	}
	
	public interface AutoBackupProgressListener {
		public void onChange(int remainCount, int totalCount);
	}
	
	public interface AutoBackupScanApListener {
		public void onDeviceFound(int index, WifiApInfo info, boolean isLastOne);
	}
	
	public interface AutoBackupErrorListener {
		public void onError(int type, int errorCode);
	}
	
	public interface AutoBackupErrorListener2 {
		public void onError(AutoBackupError2 error);
	}
	
	public interface AutoBackupStatusCallback extends ErrorCallback {
		void result(IAutoBackuper that,  ProviderType provider, int unbackupItemCount, Calendar lastBackupDate);
	}
	
	public interface AutoBackupPreferenceCallback extends ErrorCallback {
		void result(IAutoBackuper that,  OptionCheck enableAutoBackup, OptionCheck backupWhenACPluggedIn, OptionCheck deleteAfterBackingup);
	}
	
	public interface AutoBackupWifiApListCallback extends ErrorCallback {
		void result(IAutoBackuper that, ArrayList<WifiApInfo> apList);
	}
	
	public interface AutoBackupGetHttpProxyCallback extends ErrorCallback {
		void result(IAutoBackuper that, String proxy, int port);
	}
	
	public interface AutoBackupGetAccountCallback extends ErrorCallback {
		void result(IAutoBackuper that, String account);
	}
	
	public void getCurrentStatus(AutoBackupStatusCallback callback) throws Exception;
	
	public void isAutobackupAvailable(OperationCallback callback) throws Exception;
	public void setProvider(ProviderType provider, TokenType tokenType, String token, OperationCallback callback) throws Exception;
	
	public void setAccount(String account, OperationCallback callback) throws Exception;
	public void getAccount(AutoBackupGetAccountCallback callback) throws Exception;
	
	public void getWifiApList(ScanMode mode, AutoBackupWifiApListCallback callback) throws Exception;
	public void startScanWifiAp(ScanMode mode, OperationCallback callback) throws Exception;
	
	public void selectWifiAp(WifiAP apType, SecurityType security, String ssid, String key, OperationCallback callback) throws Exception;
	public void deselectWifiAp(String ssid, SecurityType security, OperationCallback callback) throws Exception;
	
	public void setHttpProxy(String ssid, SecurityType security, String proxy, int port, OperationCallback callback) throws Exception;
	public void getHttpProxy(String ssid, SecurityType security, AutoBackupGetHttpProxyCallback callback) throws Exception;
	
	public void getPreference(AutoBackupPreferenceCallback callback) throws Exception;
	public void setPreference(OptionCheck enableAutoBackup, OptionCheck backupWhenACPluggedIn, OptionCheck deleteAfterBackingup, OperationCallback callback) throws Exception;
	
	
	public void setHotspotRequestListener(AutoBackupHotspotRequestListener l);
	public void setAutoBackupProgressListener(AutoBackupProgressListener l);
	public void setAutoBackupScanApListener(AutoBackupScanApListener l);
	public void setAutoBackupErrorListener(AutoBackupErrorListener l);
	public void setAutoBackupErrorListener2(AutoBackupErrorListener2 l);
}
