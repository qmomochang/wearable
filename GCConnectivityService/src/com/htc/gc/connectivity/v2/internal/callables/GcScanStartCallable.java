package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;

import com.htc.gc.connectivity.interfaces.IGcConnectivityDevice.GcVersion;
import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner;
import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner.ScanResult;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScanner;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScannerListener;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcScanStartCallable implements Callable<Integer> {

	private static final String TAG = "GcScanStartCallable";
	
	protected GcBleScanner mGcBleScanner;
	protected Messenger mMessenger;
	
	
	
	private GcBleScannerListener mGcBleScannerListener = new GcBleScannerListener() {
		
		@Override
		public void onScanHit(BluetoothDevice device, GcVersion deviceVersion) {
			
			Log.d(TAG, "[MGCC] onScanHit. device = " + device);
			
			try {
				
				Message outMsg = Message.obtain();
				outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
				Bundle outData = new Bundle();
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_HIT);
				outData.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, device);
				outData.putSerializable(IGcConnectivityScanner.PARAM_BLUETOOTH_DEVICE_VERSION, deviceVersion);
				outMsg.setData(outData);
			
				mMessenger.send(outMsg);

			} catch (RemoteException e) {

				e.printStackTrace();
			}
		}
	};
	
	
	
	public GcScanStartCallable(GcBleScanner scanner, Messenger messenger) {

		mGcBleScanner = scanner;
		mMessenger = messenger;
	}



	@Override
	public Integer call() throws Exception {

		Integer ret = 0;
		
		mGcBleScanner.registerListener(mGcBleScannerListener);
		
		if (mGcBleScanner.scanStart()) {
			
			
		} else {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_ERROR);
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);
			
			ret = -1;
		}

		mGcBleScanner.unregisterListener(mGcBleScannerListener);
		
		return ret;
	}
}
