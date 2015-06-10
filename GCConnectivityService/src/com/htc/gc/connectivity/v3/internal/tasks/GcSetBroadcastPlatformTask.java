package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BroadcastPlatform;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BroadcastTokenType;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcSetBroadcastPlatformTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcSetBroadcastPlatformTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	private BroadcastPlatform mPlatform;
	private BroadcastTokenType mTokenType;
	private String mToken;
	
	
	public GcSetBroadcastPlatformTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, BroadcastPlatform platform, BroadcastTokenType tokenType, String token) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
		mPlatform = platform;
		mTokenType = tokenType;
		mToken = token;
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
		
		// set token
		if (mPlatform == BroadcastPlatform.BROADCAST_PLATFORM_NONE) {
			// no need to set token when platform is none
			//TODO need to clear old tokens
		} else {
			if (!write(mBluetoothDevice, APP_ID_BROADCAST, getTable(mPlatform), getKey(mTokenType), mToken)) {
				Log.d(TAG, "[MGCC] write token fail");
				sendFailMessage(mWriteError);
				return;
			}
		}
		
		// set platform
		if (!write(mBluetoothDevice, APP_ID_BROADCAST, "pf", "pltfrm", getValue(mPlatform))) {
			Log.d(TAG, "[MGCC] write pltfrm fail");
			sendFailMessage(mWriteError);
			return;
		}
		
		sendSuccessMessage();
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_SET_BROADCAST_PLATFORM_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage() {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_SET_BROADCAST_PLATFORM_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, Common.ERROR_SUCCESS);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private String getTable(BroadcastPlatform broadcastPlatform) {
		switch (broadcastPlatform) {
		case BROADCAST_PLATFORM_YOUTUBE:
			return "utube/token";
		case BROADCAST_PLATFORM_LL:
			return "ll/token";
		default:
			return "";
		}
	}
	
	
	
	private String getKey(BroadcastTokenType tokenType) {
		switch (tokenType) {
		case TOKENTYPE_ACCESS:
			return "acc";
		case TOKENTYPE_REFLESH:
			return "ref";
		default:
			return "";
		}
	}
	
	
	
	private String getValue(BroadcastPlatform broadcastPlatform) {
		switch (broadcastPlatform) {
		case BROADCAST_PLATFORM_NONE:
			return "none";
		case BROADCAST_PLATFORM_YOUTUBE:
			return "utube";
		case BROADCAST_PLATFORM_LL:
			return "ll";
		default:
			return "";
		}
	}
}
