package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.CameraMode;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcGetCameraModeTask extends GcConnectivityTask {

	private final static String TAG = "GcGetCameraModeTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	
	public GcGetCameraModeTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();

		Future<Integer> futureBoot;
		Integer bootResult;
		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		bootResult = futureBoot.get();
		if (bootResult == Common.ERROR_SUCCESS)
		{
			BluetoothGattCharacteristic result;
			Future<BluetoothGattCharacteristic> future, futureA;
			byte[] data = {};
			futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT));
			future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GET_CAMERA_MODE_REQUEST_EVENT, data));
			result = future.get();
			if (result != null)
			{
				result = futureA.get();
				if (result != null)
				{
					byte mode = result.getValue()[1];
					CameraMode cameraMode = CameraMode.findMode(mode);
					if (cameraMode != null)
					{
						sendMessage(true, cameraMode);
					}
					else
					{
						Log.d(TAG, "[MGCC] unknown camera mode:" + mode);
						sendMessage(false, null);
					}
				}
				else
				{
					sendMessage(false, null);
				}
			}
			else
			{
				sendMessage(false, null);
			}
		}
		else
		{
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false, null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, CameraMode cameraMode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_CAMERA_MODE_RESULT;
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (cameraMode != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_CAMERA_MODE, cameraMode);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null);
	}
}
