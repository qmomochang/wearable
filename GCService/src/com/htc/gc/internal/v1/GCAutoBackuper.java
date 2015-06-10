package com.htc.gc.internal.v1;

import android.os.Bundle;
import android.util.Log;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.LongTermEvent;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.interfaces.Common;
import com.htc.gc.interfaces.Common.BleCommandException;
import com.htc.gc.interfaces.Common.InvalidArgumentsException;
import com.htc.gc.interfaces.Common.NoImpException;
import com.htc.gc.interfaces.Common.OperationCallback;
import com.htc.gc.interfaces.Common.StatusException;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.IGCService.ConnectionMode;
import com.htc.gc.internal.DeviceItem;
import com.htc.gc.internal.v1.IMediator.IBleEventListener;
import com.htc.gc.tasks.AutoBackupGetAccountTask;
import com.htc.gc.tasks.AutoBackupGetCurrentStatusTask;
import com.htc.gc.tasks.AutoBackupGetPreferenceTask;
import com.htc.gc.tasks.AutoBackupGetWifiApListTask;
import com.htc.gc.tasks.AutoBackupSetAccountTask;
import com.htc.gc.tasks.AutoBackupSetPreferenceTask;
import com.htc.gc.tasks.AutoBackupSetProviderTask;

class GCAutoBackuper implements IAutoBackuper {
	protected static final boolean DEBUG = Common.DEBUG;

	private final IMediator mService;
	
	private AutoBackupHotspotRequestListener mAutoBackupHotspotRequestListener;
	private AutoBackupProgressListener mAutoBackupProgressListener;
	private AutoBackupScanApListener mAutoBackupScanApListener;
	private AutoBackupErrorListener mAutoBackupErrorListener;
	private AutoBackupErrorListener2 mAutoBackupErrorListener2;
	
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
					if(mAutoBackupScanApListener != null) mAutoBackupScanApListener.onDeviceFound(index, new WifiApInfo(ssid, rssi, security, isAuthorized), isLastOne);
				} else if(event.equals(LongTermEvent.LTEVENT_AUTO_BACKUP_ERROR)) {
					int type = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_TYPE);
					int errorCode = bundle.getInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE);
					if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] BLE autobackup error event, type= "+type+", errorCode= "+errorCode);
					if(mAutoBackupErrorListener != null) mAutoBackupErrorListener.onError(type, errorCode);
				}

			}
			
		});
		
	}
	
	@Override
	public void getCurrentStatus(AutoBackupStatusCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getCurrentStatus");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new AutoBackupGetCurrentStatusTask(this, callback));
	}
	
	@Override
	public void isAutobackupAvailable(OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] isAutobackupAvailable");
		
		setProvider(ProviderType.PROVIDER_DUMMY, TokenType.TOKENTYPE_ACCESS, "0", callback);
	}
	
	@Override
	public void setProvider(ProviderType provider, TokenType tokenType, String token, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setProvider");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new AutoBackupSetProviderTask(this, provider, tokenType, token, callback));
	}
	
	@Override
	public void setAccount(String account, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setAccount");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		if(account.getBytes().length >= IAutoBackuper.ACCOUNT_LEN - 1) throw new InvalidArgumentsException("Invalid Account Length: "+account.getBytes().length);
		
		mService.requestCommand(new AutoBackupSetAccountTask(this, account, callback));
	}

	@Override
	public void getAccount(AutoBackupGetAccountCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getAccount");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
		
		mService.requestCommand(new AutoBackupGetAccountTask(this, callback));
	}
	
	@Override
	public void getWifiApList(ScanMode mode, AutoBackupWifiApListCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getWifiApList");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new AutoBackupGetWifiApListTask(this, mode, callback));
	}
	
	@Override
	public void startScanWifiAp(ScanMode mode, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] startScanWifiAp");
		
		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full && mService.getCurrentConnectionMode() != ConnectionMode.Partial) throw new StatusException();
		
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();
		if(!conn.gcSetAutoBackupAPScan(device.getDevice(), (int) mode.getVal())) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_SCAN_RESULT, callback);
		}
	}
	
	@Override
	public void selectWifiAp(WifiAP apType, SecurityType security, String ssid, String key, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] selectAp");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		if(key.getBytes().length >= IAutoBackuper.KEY_LEN - 1) throw new InvalidArgumentsException("Invalid KEY Length: "+key.getBytes().length);
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full && mService.getCurrentConnectionMode() != ConnectionMode.Partial) throw new StatusException(); 
		 
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();

		if(!conn.gcSetAutoBackupAP(device.getDevice(), apType, ssid, key, security.getVal())) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_AP_RESULT, callback);
		}
	}
	
	@Override
	public void deselectWifiAp(String ssid, SecurityType security, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] selectAp");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full && mService.getCurrentConnectionMode() != ConnectionMode.Partial) throw new StatusException();
			
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();

		if(!conn.gcClrAutoBackupAP(device.getDevice(), security.getVal(), ssid)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_CLR_AUTO_BACKUP_AP_RESULT, callback);
		}
	}
	
	@Override
	public void setHttpProxy(String ssid, SecurityType security, String proxy, int port, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setHttpProxy");
		
		if(callback == null) throw new NullPointerException();
		
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		if(proxy.getBytes().length >= IAutoBackuper.PROXY_NAME_LEN - 1) throw new InvalidArgumentsException("Invalid Proxy Name Length: "+ssid.getBytes().length);
		if(port < 0 || port > 65535) throw new InvalidArgumentsException("Invalid port number:"+port);
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Partial && mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();
			
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();

		if(!conn.gcSetAutoBackupProxy(device.getDevice(), port, security.getVal(), ssid, proxy)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_SET_AUTO_BACKUP_PROXY_RESULT, callback);
		}
	}
	
	@Override
	public void getHttpProxy(String ssid, SecurityType security, AutoBackupGetHttpProxyCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getHttpProxy");

		if(callback == null) throw new NullPointerException();
		if(ssid.getBytes().length >= IAutoBackuper.SSID_LEN - 1) throw new InvalidArgumentsException("Invalid SSID Length: "+ssid.getBytes().length);
		
		if(mService.getCurrentConnectionMode() != ConnectionMode.Partial && mService.getCurrentConnectionMode() != ConnectionMode.Full)	throw new StatusException();
		
		IGcConnectivityService conn = mService.getConnectivityService();
		DeviceItem device = (DeviceItem)mService.getTargetDevice();

		if(!conn.gcGetAutoBackupProxy(device.getDevice(), security.getVal(), ssid)) {
			throw new BleCommandException();
		} else {
			mService.addBleCallback(GCAutoBackuper.this, IGcConnectivityService.CB_GET_AUTO_BACKUP_PROXY_RESULT, callback);
		}
	}
	
	@Override
	public void getPreference(AutoBackupPreferenceCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] getPreference");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new AutoBackupGetPreferenceTask(this, callback));
	}
	
	@Override
	public void setPreference(OptionCheck enableAutoBackup, OptionCheck backupWhenACPluggedIn, OptionCheck deleteAfterBackingup, OperationCallback callback) throws Exception {
		if(DEBUG) Log.i(Common.TAG, "[GCAutoBackuper] setPreference");

		if(callback == null) throw new NullPointerException();
		if(mService.getCurrentConnectionMode() != ConnectionMode.Full) throw new StatusException();

		mService.requestCommand(new AutoBackupSetPreferenceTask(this, enableAutoBackup, backupWhenACPluggedIn, deleteAfterBackingup, callback));
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
