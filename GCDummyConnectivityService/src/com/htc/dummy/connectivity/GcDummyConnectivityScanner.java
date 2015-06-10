package com.htc.dummy.connectivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.htc.gc.connectivity.interfaces.IGcConnectivityScanner;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;

public class GcDummyConnectivityScanner implements IGcConnectivityScanner {
	
	private final Context mContext;
	private final Messenger mMessenger;
	
	private final Handler mHandler = new Handler();
	
	private boolean mIsScanning = false;
	
	public GcDummyConnectivityScanner(Context context, Messenger messenger) {
		mContext = context;
		mMessenger = messenger;
	}

	@Override
	public boolean gcOpen() {
		return true;
	}

	@Override
	public boolean gcClose() {
		return true;
	}

	@Override
	public boolean gcScan(int period) {
		processGCScan(period);
		return true;
	}

	@Override
	public boolean gcStopScan() {
		processGCStopScan();
		return true;
	}

	private void processGCScan(int peroid) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityScanner.ScanResult.SCAN_RESULT_HIT);
				bundle.putParcelable(IGcConnectivityService.PARAM_BLUETOOTH_DEVICE, null);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				Message message = Message.obtain();
				message.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
				Bundle bundle = new Bundle();
				bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityScanner.ScanResult.SCAN_RESULT_COMPLETE);
				message.setData(bundle);
				try {
					mMessenger.send(message);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
		}, peroid);
	}
	
	private void processGCStopScan() {
		if(mIsScanning == true) {
			Message message = Message.obtain();
			message.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
			Bundle bundle = new Bundle();
			bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityScanner.ScanResult.SCAN_RESULT_COMPLETE);
			message.setData(bundle);
			try {
				mMessenger.send(message);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		} else {
			Message message = Message.obtain();
			message.what = IGcConnectivityService.CB_BLE_SCAN_RESULT;
			Bundle bundle = new Bundle();
			bundle.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityScanner.ScanResult.SCAN_RESULT_ERROR);
			message.setData(bundle);
			try {
				mMessenger.send(message);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
