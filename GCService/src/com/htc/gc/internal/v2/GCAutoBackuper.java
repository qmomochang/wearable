package com.htc.gc.internal.v2;

import java.util.ArrayList;
import android.os.Bundle;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.InvalidArgumentsException;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v2.IMediator.IBleEventListener;

class GCAutoBackuper implements IAutoBackuper {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	
	private AutoBackupHotspotRequestListener mAutoBackupHotspotRequestListener;
	private AutoBackupProgressListener mAutoBackupProgressListener;
	private AutoBackupScanApListener mAutoBackupScanApListener;
	private AutoBackupErrorListener mAutoBackupErrorListener;
	private AutoBackupErrorListener2 mAutoBackupErrorListener2;
	
	private AutoBackupWifiApListCallback mAutoBackupWifiApListCallback;
	private boolean mIsScanningWifiApList;
	private ArrayList<WifiApInfo> mWifiApList = new ArrayList<WifiApInfo>();
	
	GCAutoBackuper(IMediator service) {
		mService = service;
		
		mService.addBleEventListener(IGcConnectivityService.CB_LONG_TERM_EVENT_RESULT, new IBleEventListener() {

			@Override
			public void event(int callbackID, Bundle bundle) {
				LongTermEvent event = (LongTermEvent) bundle.getSerializable(IGcConnectivityService.PARAM_LONG_TERM_EVENT);
				if(event.equals(LongTermEvent.LTEVENT_HOTSPOT_CONTROL)) {
					IGcConnectivityService.SwitchOnOff onOff = (IGcConnectivityService.SwitchOnOff) bundle.getSerializable(IGcConnectivityService.PARAM_SWITCH_ON_OFF);
					if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] BLE autobackup hotspot control event, onOff="+onOff);
					if(mAutoBackupHotspotRequestListener != null) mAutoBackupHotspotRequestListener.onSwitch(onOff.equals(SwitchOnOff.SWITCH_ON));
				} else if(event.equals(LongTermEvent.LTEVENT_AUTO_BACKUP_PROGRESS)) {
					int remainCount = bundle.getInt(IGcConnectivityService.PARAM_REMAIN_FILE_COUNT);
					int totalCount = bundle.getInt(IGcConnectivityService.PARAM_TOTAL_FILE_COUNT);
					if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] BLE autobackup progress remain= "+remainCount+", total="+totalCount);
					if(mAutoBackupProgressListener != null) mAutoBackupProgressListener.onChange(remainCount, totalCount);
				} else if(event.equals(LongTermEvent.LTEVENT_AUTO_BACKUP_AP_SCAN_RESULT)) {
					boolean anyScanResult = bundle.getBoolean(IGcConnectivityService.PARAM_AP_ANY_SCAN_RESULT);
					if (anyScanResult) {
						boolean isLastOne = bundle.getInt(IGcConnectivityService.PARAM_AP_END_OF_SCAN_LIST) == 0 ? false : true;
						int	index = bundle.getInt(IGcConnectivityService.PARAM_AP_INDEX_OF_SCAN_LIST);
						short rssi = bundle.getShort(IGcConnectivityService.PARAM_AP_RSSI);
						SecurityType security = SecurityType.SECURITY_UNKNOWN;
						try {
							security = SecurityType.getKey((byte) bundle.getInt(IGcConnectivityService.PARAM_AP_SECURITY));
						} catch (NoImpException e) {
							Log.e(Common.TAG, "CB_LONG_TERM_EVENT_RESULT: invalid securityType");
							e.printStackTrace();
						}
						boolean isAuthorized = bundle.getInt(IGcConnectivityService.PARAM_AP_AUTHORIZATION) == AUTHORIZATED ? true : false;
						String ssid = bundle.getString(IGcConnectivityService.PARAM_AP_SSID);
						WifiApInfo wifiApInfo = new WifiApInfo(ssid, rssi, security, isAuthorized);
						if(mAutoBackupScanApListener != null) mAutoBackupScanApListener.onDeviceFound(index, wifiApInfo, isLastOne);
						
						mWifiApList.add(wifiApInfo);
						if (isLastOne) {
							if(mAutoBackupWifiApListCallback != null) mAutoBackupWifiApListCallback.result(GCAutoBackuper.this, mWifiApList);
							mIsScanningWifiApList = false;
							mWifiApList.clear();
						}
					} else {
						if(mAutoBackupScanApListener != null) mAutoBackupScanApListener.onDeviceFound(-1, null, true);
						
						mWifiApList.clear();
						if(mAutoBackupWifiApListCallback != null) mAutoBackupWifiApListCallback.result(GCAutoBackuper.this, mWifiApList);
						mIsScanningWifiApList = false;
					}
				} else if(event.equals(LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR)) {
					int type = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_TYPE);
					int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
					if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] BLE autobackup error event, type= "+type+", errorCode= "+errorCode);
					if(mAutoBackupErrorListener != null) mAutoBackupErrorListener.onError(type, errorCode);
				} else if (event.equals(LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR2)) {
					String message = bundle.getString(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR2_MESSAGE);
					if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] BLE autobackup error2 message=" + message);
					if (mAutoBackupErrorListener2 != null) {
						AutoBackupError2 error = AutoBackupError2.AUTO_BACKUP_ERROR2_UNKNOWN;
						if (message.equals("ok")) {
							error = AutoBackupError2.AUTO_BACKUP_ERROR2_SUCCESS;
						} else if (message.equals("tk_inval")) {
							error = AutoBackupError2.AUTO_BACKUP_ERROR2_AUTO_TOKEN_INVALID;
						} else if (message.equals("no_sp")) {
							error = AutoBackupError2.AUTO_BACKUP_ERROR2_USER_OUT_OF_SPACE;
						}
						mAutoBackupErrorListener2.onError(error);
					}
				}

			}
			
		});
		
	}
	
	@Override
	public void getCurrentStatus(AutoBackupStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getCurrentStatus");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			if(!conn.gcGetAutoBackupStatus(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(this, IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void isAutobackupAvailable(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] isAutobackupAvailable");
		
		if (callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if (!conn.gcGetAutoBackupIsAvailable(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_GET_AUTO_BACKUP_IS_AVAILABLE_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setProvider(ProviderType provider, TokenType tokenType, String token, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setProvider");

		if(callback == null) throw new NullPointerException();
		
		switch(mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			IGcConnectivityService.BackupProviderIdIndex backupProviderIdIndex = IGcConnectivityService.BackupProviderIdIndex.findProvider(provider.getVal());
			IGcConnectivityService.BackupTokenType backupTokenType = IGcConnectivityService.BackupTokenType.findToken(tokenType.getVal());
			if(!conn.gcSetAutoBackupToken(device.getDevice(), backupProviderIdIndex, backupTokenType, token)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_TOKEN_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setAccount(String account, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setAccount");

		if(callback == null) throw new NullPointerException();
		if(account.getBytes().length >= IAutoBackuper.ACCOUNT_LEN - 1) throw new InvalidArgumentsException("Invalid Account Length: "+account.getBytes().length);
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if (!conn.gcSetAutoBackupAccount(device.getDevice(), account)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_ACCOUNT_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}

	@Override
	public void getAccount(AutoBackupGetAccountCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getAccount");

		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if (!conn.gcGetAutoBackupAccount(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_GET_AUTO_BACKUP_ACCOUNT_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getWifiApList(ScanMode mode, AutoBackupWifiApListCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getWifiApList");

		if(callback == null) throw new NullPointerException();

		if (mIsScanningWifiApList) {
			Log.i(Common.TAG, "[GCAutoBackuper] it is scanning wifi ap list, wait for result");
		} else {
			startScanWifiAp(mode, new OperationCallback(){

				@Override
				public void error(Exception e) {
					Log.d(Common.TAG, "[GCAutoBackuper] start scan wifi ap fail, error:" + e);
				}

				@Override
				public void done(Object that) {
					Log.d(Common.TAG, "[GCAutoBackuper] start scan wifi ap ok");
				}});
			mAutoBackupWifiApListCallback = callback;
		}
	}
	
	@Override
	public void startScanWifiAp(ScanMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] startScanWifiAp");
		
		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if(!conn.gcSetAutoBackupAPScan(device.getDevice(), 0/*start*/, (int) mode.getVal())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_START_RESULT, callback);
				mIsScanningWifiApList = true;
				mWifiApList.clear();
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void selectWifiAp(WifiAP apType, SecurityType security, String ssid, String key, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] selectAp");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		if(key.getBytes().length >= IAutoBackuper.KEY_LEN - 1) throw new InvalidArgumentsException("Invalid KEY Length: "+key.getBytes().length);
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			//test Marvin: not ready
			if(!conn.gcSetAutoBackupAP(device.getDevice(), /*apType,*/ ssid, key, security.getVal())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void deselectWifiAp(String ssid, SecurityType security, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] selectAp");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			if(!conn.gcClrAutoBackupAP(device.getDevice(), security.getVal(), ssid)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void setHttpProxy(String ssid, SecurityType security, String proxy, int port, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setHttpProxy");
		
		if(callback == null) throw new NullPointerException();
		
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		if(proxy.getBytes().length >= IAutoBackuper.PROXY_NAME_LEN - 1) throw new InvalidArgumentsException("Invalid Proxy Name Length: "+ssid.getBytes().length);
		if(port < 0 || port > 65535) throw new InvalidArgumentsException("Invalid port number:"+port);
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			if(!conn.gcSetAutoBackupProxy(device.getDevice(), port, security.getVal(), ssid, proxy)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_PROXY_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getHttpProxy(String ssid, SecurityType security, AutoBackupGetHttpProxyCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getHttpProxy");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();

			if(!conn.gcGetAutoBackupProxy(device.getDevice(), security.getVal(), ssid)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_GET_AUTO_BACKUP_PROXY_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}
	}
	
	@Override
	public void getPreference(AutoBackupPreferenceCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getPreference");

		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if (!conn.gcGetAutoBackupPreference(device.getDevice())) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_GET_AUTO_BACKUP_PREFERENCE_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}		
	}
	
	@Override
	public void setPreference(OptionCheck enableAutoBackup, OptionCheck backupWhenACPluggedIn, OptionCheck deleteAfterBackingup, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setPreference");

		if(callback == null) throw new NullPointerException();
		
		switch (mService.getCurrentConnectionMode()) {
		case Full:
		case Partial:
			IGcConnectivityService conn = mService.getConnectivityService();
			DeviceItem device = (DeviceItem)mService.getTargetDevice();
			if (!conn.gcSetAutoBackupPreference(device.getDevice(), 
					enableAutoBackup == OptionCheck.CHECK_ON, 
					deleteAfterBackingup == OptionCheck.CHECK_ON, 
					backupWhenACPluggedIn == OptionCheck.CHECK_OFF)) {
				throw new BleCommandException();
			} else {
				mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_PREFERENCE_RESULT, callback);
			}
			break;
		default:
			throw new StatusException();
		}		
	}

	@Override
	public void setHotspotRequestListener(AutoBackupHotspotRequestListener l) {
		mAutoBackupHotspotRequestListener = l;
	}

	@Override
	public void setAutoBackupProgressListener(AutoBackupProgressListener l) {
		mAutoBackupProgressListener = l;
	}

	@Override
	public void setAutoBackupScanApListener(AutoBackupScanApListener l) {
		mAutoBackupScanApListener = l;
	}

	@Override
	public void setAutoBackupErrorListener(AutoBackupErrorListener l) {
		mAutoBackupErrorListener = l;
	}
	
	@Override
	public void setAutoBackupErrorListener2(AutoBackupErrorListener2 l) {
		mAutoBackupErrorListener2 = l;
	}

}
