package com.htc.gc.internal;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.WifiAP;
import com.htc.gc.interfaces.IAutoBackuper;
import com.htc.gc.interfaces.Common.OperationCallback;

class NullGCAutoBackuper implements IAutoBackuper {

	@Override
	public void getCurrentStatus(AutoBackupStatusCallback callback)
			throws Exception {
	}

	@Override
	public void isAutobackupAvailable(OperationCallback callback)
			throws Exception {
	}

	@Override
	public void setProvider(ProviderType provider, TokenType tokenType,
			String token, OperationCallback callback) throws Exception {
	}

	@Override
	public void setAccount(String account, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void getAccount(AutoBackupGetAccountCallback callback)
			throws Exception {
	}

	@Override
	public void getWifiApList(ScanMode mode,
			AutoBackupWifiApListCallback callback) throws Exception {
	}

	@Override
	public void startScanWifiAp(ScanMode mode, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void selectWifiAp(WifiAP apType, SecurityType security, String ssid,
			String key, OperationCallback callback) throws Exception {
	}

	@Override
	public void deselectWifiAp(String ssid, SecurityType security,
			OperationCallback callback) throws Exception {
	}

	@Override
	public void setHttpProxy(String ssid, SecurityType security, String proxy,
			int port, OperationCallback callback) throws Exception {
	}

	@Override
	public void getHttpProxy(String ssid, SecurityType security,
			AutoBackupGetHttpProxyCallback callback) throws Exception {
	}

	@Override
	public void getPreference(AutoBackupPreferenceCallback callback)
			throws Exception {
	}

	@Override
	public void setPreference(OptionCheck enableAutoBackup,
			OptionCheck backupWhenACPluggedIn,
			OptionCheck deleteAfterBackingup, OperationCallback callback)
			throws Exception {
	}

	@Override
	public void setHotspotRequestListener(AutoBackupHotspotRequestListener l) {
	}

	@Override
	public void setAutoBackupProgressListener(AutoBackupProgressListener l) {
	}

	@Override
	public void setAutoBackupScanApListener(AutoBackupScanApListener l) {
	}

	@Override
	public void setAutoBackupErrorListener(AutoBackupErrorListener l) {
	}
	
	@Override
	public void setAutoBackupErrorListener2(AutoBackupErrorListener2 l) {
	}

}
