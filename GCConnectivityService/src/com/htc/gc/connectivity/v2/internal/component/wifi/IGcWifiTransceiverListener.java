package com.htc.gc.connectivity.v2.internal.component.wifi;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;

import android.net.wifi.WifiInfo;



public interface IGcWifiTransceiverListener {

	public void onWifiDirectGroupCreated();
	public void onWifiDirectGroupRemoved();
	public void onWifiP2pDisabled();
	public void onWifiConnected(WifiInfo info);
	public void onWifiDisconnected();
	public void onWifiConnecting();
	public void onWifiSupplicantCompleted();
	public void onWifiSupplicantScanning();
	public void onWifiSupplicantDisconnected();
   	public void onError(GcWifiTransceiverErrorCode errorCode);
}
