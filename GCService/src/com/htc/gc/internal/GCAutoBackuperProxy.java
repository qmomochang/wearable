package com.htc.gc.internal;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.Common.OperationCallback;

public class GCAutoBackuperProxy implements IAutoBackuper {
	
	private IAutoBackuper mAutoBackuper = new NullGCAutoBackuper();
	
	private AutoBackupHotspotRequestListener mAutoBackupHotspotRequestListener;
	private AutoBackupProgressListener mAutoBackupProgressListener;
	private AutoBackupScanApListener mAutoBackupScanApListener;
	private AutoBackupErrorListener mAutoBackupErrorListener;
	private AutoBackupErrorListener2 mAutoBackupErrorListener2;
	
	public void setAutoBackuper(IAutoBackuper autoBackuper) {
		mAutoBackuper = autoBackuper;
		
		mAutoBackuper.setHotspotRequestListener(mAutoBackupHotspotRequestListener);
		mAutoBackuper.setAutoBackupProgressListener(mAutoBackupProgressListener);
		mAutoBackuper.setAutoBackupScanApListener(mAutoBackupScanApListener);
		mAutoBackuper.setAutoBackupErrorListener(mAutoBackupErrorListener);
		mAutoBackuper.setAutoBackupErrorListener2(mAutoBackupErrorListener2);
	}

	@Override
	public void getCurrentStatus(AutoBackupStatusCallback callback)
			throws Exception {
		mAutoBackuper.getCurrentStatus(callback);
	}

	@Override
	public void isAutobackupAvailable(OperationCallback callback)
			throws Exception {
		mAutoBackuper.isAutobackupAvailable(callback);
	}

	@Override
	public void setProvider(ProviderType provider, TokenType tokenType,
			String token, OperationCallback callback) throws Exception {
		mAutoBackuper.setProvider(provider, tokenType, token, callback);
	}

	@Override
	public void setAccount(String account, OperationCallback callback)
			throws Exception {
		mAutoBackuper.setAccount(account, callback);
	}

	@Override
	public void getAccount(AutoBackupGetAccountCallback callback)
			throws Exception {
		mAutoBackuper.getAccount(callback);
	}

	@Override
	public void getWifiApList(ScanMode mode,
			AutoBackupWifiApListCallback callback) throws Exception {
		mAutoBackuper.getWifiApList(mode, callback);
	}

	@Override
	public void startScanWifiAp(ScanMode mode, OperationCallback callback)
			throws Exception {
		mAutoBackuper.startScanWifiAp(mode, callback);
	}

	@Override
	public void selectWifiAp(WifiAP apType, SecurityType security, String ssid,
			String key, OperationCallback callback) throws Exception {
		mAutoBackuper.selectWifiAp(apType, security, ssid, key, callback);
	}

	@Override
	public void deselectWifiAp(String ssid, SecurityType security,
			OperationCallback callback) throws Exception {
		mAutoBackuper.deselectWifiAp(ssid, security, callback);
	}

	@Override
	public void setHttpProxy(String ssid, SecurityType security, String proxy,
			int port, OperationCallback callback) throws Exception {
		mAutoBackuper.setHttpProxy(ssid, security, proxy, port, callback);
	}

	@Override
	public void getHttpProxy(String ssid, SecurityType security,
			AutoBackupGetHttpProxyCallback callback) throws Exception {
		mAutoBackuper.getHttpProxy(ssid, security, callback);
	}

	@Override
	public void getPreference(AutoBackupPreferenceCallback callback)
			throws Exception {
		mAutoBackuper.getPreference(callback);
	}

	@Override
	public void setPreference(OptionCheck enableAutoBackup,
			OptionCheck backupWhenACPluggedIn,
			OptionCheck deleteAfterBackingup, OperationCallback callback)
			throws Exception {
		mAutoBackuper.setPreference(enableAutoBackup, backupWhenACPluggedIn, deleteAfterBackingup, callback);
	}

	@Override
	public void setHotspotRequestListener(AutoBackupHotspotRequestListener l) {
		mAutoBackupHotspotRequestListener = l;
		mAutoBackuper.setHotspotRequestListener(l);
	}

	@Override
	public void setAutoBackupProgressListener(AutoBackupProgressListener l) {
		mAutoBackupProgressListener = l;
		mAutoBackuper.setAutoBackupProgressListener(l);
	}

	@Override
	public void setAutoBackupScanApListener(AutoBackupScanApListener l) {
		mAutoBackupScanApListener = l;
		mAutoBackuper.setAutoBackupScanApListener(l);
	}

	@Override
	public void setAutoBackupErrorListener(AutoBackupErrorListener l) {
		mAutoBackupErrorListener = l;
		mAutoBackuper.setAutoBackupErrorListener(l);
	}
	
	@Override
	public void setAutoBackupErrorListener2(AutoBackupErrorListener2 l) {
		mAutoBackupErrorListener2 = l;
		mAutoBackuper.setAutoBackupErrorListener2(l);
	}

}
