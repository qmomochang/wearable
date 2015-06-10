package com.htc.gc.connectivity.v2.internal.component.le;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;

import android.bluetooth.BluetoothDevice;



public class GcBleScannerListener implements IGcBleScannerListener {

	@Override
	public void onScanHit(BluetoothDevice device, GcVersion deviceVersion) {
		
	}

	@Override
	public void onScanHitConnected(BluetoothDevice device, GcVersion deviceVersion) {
		
	}
	
	@Override
	public void onScanComplete() {
		
	}
}
