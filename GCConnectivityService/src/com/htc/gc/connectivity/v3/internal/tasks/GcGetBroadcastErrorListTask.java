package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcGetBroadcastErrorListTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcGetBroadcastErrorListTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcGetBroadcastErrorListTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

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
		
		if (!read(mBluetoothDevice, APP_ID_BROADCAST, "error", "errnum")) {
			Log.d(TAG, "[MGCC] read videourl fail");
			sendFailMessage(mReadError);
			return;
		}
		
		int errorNumber = 0;
		try {
			Integer.parseInt(mReadValue);
		} catch (NumberFormatException e) {
			Log.d(TAG, "[MGCC] fail to parse error number from " + mReadValue);
			sendFailMessage(Common.ERROR_FAIL);
			return;
		}
		if (errorNumber < 0) {
			Log.d(TAG, "[MGCC] invalid error number " + errorNumber);
			sendFailMessage(Common.ERROR_FAIL);
			return;
		}
		
		byte[] errorList = new byte[errorNumber];
		String errorKey;
		for (int i = 0; i < errorNumber; ++i) {
			errorKey = "err" + i;
			if (!read(mBluetoothDevice, APP_ID_BROADCAST, "error", errorKey)) {
				Log.d(TAG, "[MGCC] read " + errorKey + " fail");
				sendFailMessage(mReadError);
				return;
			}
			errorList[i] = mReadValue.getBytes()[0];
		}
		
		sendSuccessMessage(errorList);
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_ERROR_LIST_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage(byte[] errorList) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_ERROR_LIST_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, IGcConnectivityService.ERROR_SUCCESS);
			outData.putByteArray(IGcConnectivityService.PARAM_BROADCAST_ERROR_LIST, errorList);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
