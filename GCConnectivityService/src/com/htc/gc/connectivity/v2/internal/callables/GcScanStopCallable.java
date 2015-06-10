package com.htc.gc.connectivity.v2.internal.callables;

import java.util.concurrent.Callable;

import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner.ScanResult;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleScanner;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;



public class GcScanStopCallable implements Callable<Integer> {

	private final static String TAG = "GcScanStopCallable";
	
	protected GcBleScanner mGcBleScanner;
	protected Messenger mMessenger;
	
	
	
	public GcScanStopCallable(GcBleScanner scanner, Messenger messenger) {

		mGcBleScanner = scanner;
		mMessenger = messenger;
	}



	@Override
	public Integer call() throws Exception {

		Integer ret = 0;
		
		if (mGcBleScanner.scanStop()) {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_COMPLETE);
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} else {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, ScanResult.SCAN_RESULT_ERROR);
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);
		}
		
		return ret;
	}
}
