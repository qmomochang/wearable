package com.htc.gc.internal.v2;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;

final class GCDeviceInfoManager {
	
	private static IGcConnectivityService.MCUBatteryLevel currentBatteryLevel;

	static IGcConnectivityService.MCUBatteryLevel getCurrentBatteryLevel() {
		return currentBatteryLevel;
	}
	
	static void setCurrentBatteryLevel(IGcConnectivityService.MCUBatteryLevel batteryLevel) {
		currentBatteryLevel = batteryLevel;
	}
	
}
