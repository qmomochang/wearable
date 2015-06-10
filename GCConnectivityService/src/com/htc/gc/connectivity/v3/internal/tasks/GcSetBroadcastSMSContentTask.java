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



public class GcSetBroadcastSMSContentTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcSetBroadcastSMSContentTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	private String mBroadcastSMSContent;
	
	
	public GcSetBroadcastSMSContentTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, String broadcastSMSContent) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
		mBroadcastSMSContent = broadcastSMSContent;
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
		
		if (!write(mBluetoothDevice, APP_ID_BROADCAST, "pf", "smscontent", mBroadcastSMSContent)) {
			Log.d(TAG, "[MGCC] write smscontent fail");
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
			
			outMsg.what = IGcConnectivityService.CB_SET_BROADCAST_SMS_CONTENT_RESULT;
			
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
			
			outMsg.what = IGcConnectivityService.CB_SET_BROADCAST_SMS_CONTENT_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, Common.ERROR_SUCCESS);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
}
