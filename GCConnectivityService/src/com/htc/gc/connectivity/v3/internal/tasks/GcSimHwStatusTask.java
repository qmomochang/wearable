package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.SimHwStatus;
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



public class GcSimHwStatusTask extends GcConnectivityTask {

	private final static String TAG = "GcSimHwStatusTask";
	
	public final static int ACTION_SET_LTEVENT = 0;
	public final static int ACTION_CLR_LTEVENT = 1;
	public final static int ACTION_GET_STATUS = 2;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	
	
	public GcSimHwStatusTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();

		if (mAction == ACTION_GET_STATUS) {
			Future<Integer> futureBoot;
			Integer bootResult;
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			bootResult = futureBoot.get();
			if (bootResult == Common.ERROR_SUCCESS)
			{
				BluetoothGattCharacteristic result;
				Future<BluetoothGattCharacteristic> future, futureA;
				byte[] data = {};
				futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SIM_HW_STATUS_EVENT));
				future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SIM_HW_STATUS_EVENT, data));
				result = future.get();
				if (result != null)
				{
					result = futureA.get();
					if (result != null)
					{
						byte status = result.getValue()[1];
						SimHwStatus simHwStatus = SimHwStatus.findStatus(status);
						if (simHwStatus != null)
						{
							sendMessage(true, simHwStatus);
						}
						else
						{
							Log.d(TAG, "[MGCC] unknown sim hw status:" + status);
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
		} else if (mAction == ACTION_SET_LTEVENT) {
			
			sendMessage(true, null);
			
		} else if (mAction == ACTION_CLR_LTEVENT) {
			
			sendMessage(true, null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, SimHwStatus simHwStatus) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_GET_STATUS) {
				
				outMsg.what = IGcConnectivityService.CB_GET_SIM_HW_STATUS_RESULT;
				
			} else if (mAction == ACTION_SET_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_SET_SIM_HW_STATUS_LTEVENT_RESULT;

			} else if (mAction == ACTION_CLR_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_CLR_SIM_HW_STATUS_LTEVENT_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (simHwStatus != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_SIM_HW_STATUS, simHwStatus);
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
