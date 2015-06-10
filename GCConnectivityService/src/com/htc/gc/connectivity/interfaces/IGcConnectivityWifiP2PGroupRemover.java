package com.htc.gc.connectivity.interfaces;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;



public interface IGcConnectivityWifiP2PGroupRemover {
	
	/// Interfaces
	public boolean gcRemoveWifiP2pGroupInFinish();
	
	/// Callback
	public static final int CB_REMOVE_WIFI_P2P_GROUP_IN_FINISH_RESULT	= IGcConnectivityService.CB_REMOVE_WIFI_P2P_GROUP_IN_FINISH_RESULT;
	
	/// Parameters
	public static final String PARAM_RESULT								= IGcConnectivityService.PARAM_RESULT;
}
