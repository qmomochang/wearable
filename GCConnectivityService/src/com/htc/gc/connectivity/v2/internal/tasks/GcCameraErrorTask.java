package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcCameraErrorTask extends GcConnectivityTask {

	private final static String TAG = "GcCameraErrorTask";
	
	public final static int ACTION_SET_CAMERA_ERROR_LTEVENT = 0;
	public final static int ACTION_CLR_CAMERA_ERROR_LTEVENT = 1;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	
	
	public GcCameraErrorTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mAction == ACTION_SET_CAMERA_ERROR_LTEVENT) {
			
			sendMessage(true);

		} else if (mAction == ACTION_CLR_CAMERA_ERROR_LTEVENT) {

			sendMessage(true);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result) {
		
		try {
			
			Message outMsg = Message.obtain();

			if (mAction == ACTION_SET_CAMERA_ERROR_LTEVENT) {

				outMsg.what = IGcConnectivityService.CB_SET_CAMERA_ERROR_LTEVENT_RESULT;

			} else if (mAction == ACTION_CLR_CAMERA_ERROR_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_CLR_CAMERA_ERROR_LTEVENT_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	

	
	@Override
	public void error(Exception e) {

		sendMessage(false);
	}
}
