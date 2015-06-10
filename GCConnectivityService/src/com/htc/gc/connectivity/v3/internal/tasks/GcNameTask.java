package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReadCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleSetNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributeUtil;
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



public class GcNameTask extends GcConnectivityTask {

	private final static String TAG = "GcNameTask";

	public final static int ACTION_SET_NAME = 1;
	public final static int ACTION_GET_NAME = 0;
	public final static int MAX_DATA_LENGTH = 15;
	public final static int DATA_OFFSET_INDEX = 1;
	public final static int DATA_ADD_END = 1;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private String mName;
	
	
	
	public GcNameTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, String name) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		
		if (name != null) {
			
			mName = name;
			
		} else {
			
			mName = "hTC GC";
		}
	}

	private void setName(String name)
	{
		mName = name;
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
			Future<BluetoothGattCharacteristic> futureA0, futureA1, futureB;

			if (mAction == ACTION_SET_NAME) {
			
				char[] temp = mName.toCharArray();
			
				if (temp.length > MAX_DATA_LENGTH) {
					sendMessage(false, null);
					return;
				}
			
				byte[] nameArray = new byte[temp.length + DATA_OFFSET_INDEX + DATA_ADD_END];
				nameArray[0] = (byte)ACTION_SET_NAME;
				nameArray[temp.length + DATA_OFFSET_INDEX] = 0x00;
				for (int cnt = 0; cnt < temp.length; cnt++) {
					nameArray[cnt + DATA_OFFSET_INDEX] = (byte) temp[cnt];
				}
				Log.d(TAG, "[MGCC] setName:" + mName + ", length:" + mName.length());
			
				futureA0 = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST, nameArray));

				result = futureA0.get();
			
				if (result != null) {
				
					sendMessage(true, null);

				} else {
				
					sendMessage(false, null);
				}

			} else if (mAction == ACTION_GET_NAME) {

				byte[] nameArray = new byte[DATA_OFFSET_INDEX];
				nameArray[0] = (byte)ACTION_GET_NAME;
				//Here is no data!!
				futureA0 = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST));
				futureA1 = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST, true));

				if (futureA1.get() == null) {

					sendMessage(false, null);
					unregisterNotify(GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST);
					return;
				}
			
				futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST, nameArray));

				result = futureB.get();
			
				if (result != null) {

					result = futureA0.get();
					if (result != null)
					{
						setName(GcBleGattAttributeUtil.getGcName(result));
						sendMessage(true, mName);
					}
					else
					{
						sendMessage(false, null);
						unregisterNotify(GcBleGattAttributes.GcV2CommandEnum.SET_GC_NAME_REQUEST);
					}

				} else {
				
					sendMessage(false, null);
				}
			}
		}
		else
		{
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false, null);
		}

		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, String name) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_NAME) {
			
				outMsg.what = IGcConnectivityService.CB_SET_NAME_RESULT;
				
			} else if (mAction == ACTION_GET_NAME) {
				
				outMsg.what = IGcConnectivityService.CB_GET_NAME_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (name != null) {
				
				outData.putString(IGcConnectivityService.PARAM_GC_NAME, name);
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

	private void unregisterNotify(GcBleGattAttributes.GcV2CommandEnum commandID) throws Exception {

		Future<BluetoothGattCharacteristic> future;

		future = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, commandID, false));
		if (future.get() == null) {

			Log.d(TAG, "[MGCC] unregisterNotify error!!!");
			return;
		}
	}

}
