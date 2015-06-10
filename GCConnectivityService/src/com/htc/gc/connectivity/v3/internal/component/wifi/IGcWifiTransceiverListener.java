package com.htc.gc.connectivity.v3.internal.component.wifi;

import com.htc.gc.connectivity.internal.common.CommonBase.GcWifiTransceiverErrorCode;


public interface IGcWifiTransceiverListener {

	public void onWifiDirectGroupCreated();
	public void onWifiDirectGroupRemoved();
	public void onWifiP2pDisabled();
	public void onWifiConnected();
	public void onWifiDisconnected();
	public void onWifiConnecting();
   	public void onError(GcWifiTransceiverErrorCode errorCode);
}
