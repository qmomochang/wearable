package com.htc.gc.connectivity.v2.internal.component.le;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;

import android.bluetooth.BluetoothDevice;



public interface IGcBleScannerListener {

	public void onScanHit(BluetoothDevice device, GcVersion deviceVersion);
	public void onScanHitConnected(BluetoothDevice device, GcVersion deviceVersion);
   	public void onScanComplete();
}
