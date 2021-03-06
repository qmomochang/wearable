package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BroadcastPlatform;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcGetBroadcastPlatformTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcGetBroadcastPlatformTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcGetBroadcastPlatformTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (!bootup(mBluetoothDevice)) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendFailMessage(Common.ERROR_BOOT_UP_GC);
			return;
		}
		
		if (!read(mBluetoothDevice, APP_ID_BROADCAST, "pf", "pltfrm")) {
			Log.d(TAG, "[MGCC] read pltfrm fail");
			sendFailMessage(mReadError);
			return;
		}
		
		BroadcastPlatform broadcastPlatform = toBroadcastPlatform(mReadValue);
		sendSuccessMessage(broadcastPlatform);
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_PLATFORM_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage(BroadcastPlatform broadcastPlatform) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_PLATFORM_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, IGcConnectivityService.ERROR_SUCCESS);
			outData.putSerializable(IGcConnectivityService.PARAM_BROADCAST_PLATFORM, broadcastPlatform);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private BroadcastPlatform toBroadcastPlatform(String value) {
		if (value.equals("utube")) {
			return BroadcastPlatform.BROADCAST_PLATFORM_YOUTUBE;
		} else if (value.equals("ll")) {
			return BroadcastPlatform.BROADCAST_PLATFORM_LL;
		} else {
			return BroadcastPlatform.BROADCAST_PLATFORM_NONE;
		}
	}
}
