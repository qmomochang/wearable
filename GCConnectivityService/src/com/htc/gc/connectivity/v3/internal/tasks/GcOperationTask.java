package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BootUpType;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.Operation;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
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



public class GcOperationTask extends GcConnectivityTask {

	private final static String TAG = "GcOperationTask";
	
	public final static int ACTION_SET_OPERATION_LTEVENT = 0;
	public final static int ACTION_CLR_OPERATION_LTEVENT = 1;
	public final static int ACTION_SET_OPERATION = 2;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private Operation mOperation = Operation.OPERATION_NONE;
	private IGcConnectivityService.BootUpType mBootupType = BootUpType.BOOTUP_RTOS;
	
	
	
	public GcOperationTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, Operation operation) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		mOperation = operation;
		if (mOperation == Operation.OPERATION_BROADCAST_START) {
			mBootupType = BootUpType.BOOTUP_LINUX;
		}
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mAction == ACTION_SET_OPERATION_LTEVENT) {
			
			sendMessage(true, -1, null);
			
		} else if (mAction == ACTION_CLR_OPERATION_LTEVENT) {

			sendMessage(true, -1, null);

		} else if (mAction == ACTION_SET_OPERATION) {
			
			Integer bootResult;
			Future<Integer> futureBoot;
			
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, mBootupType, mMessenger));
			bootResult = futureBoot.get();
			
			if (bootResult == Common.ERROR_SUCCESS) {
				
				setOperation();

			} else {
			    Log.d(TAG, "[MGCC] boot up is fail");
				sendMessage(false, -1, null);
			}

		} else {
			
			sendMessage(false, -1, null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void setOperation() throws Exception {
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureC;
		byte type = -1;
		byte action = -1;

		if (mOperation == Operation.OPERATION_CAPTURE_START) {

			type = 0;
			action = 1;
			
		} else if (mOperation == Operation.OPERATION_VIDEO_RECORDING_NORMAL_START) {

			type = 1;
			action = 1;

		} else if (mOperation == Operation.OPERATION_VIDEO_RECORDING_NORMAL_STOP) {

			type = 1;
			action = 0;

		} else if (mOperation == Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_START) {

			type = 3;
			action = 1;

		} else if (mOperation == Operation.OPERATION_VIDEO_RECORDING_SLOW_MOTION_STOP) {

			type = 3;
			action = 0;

		} else if (mOperation == Operation.OPERATION_TIME_LAPS_RECORDING_START) {

			type = 2;
			action = 1;

		} else if (mOperation == Operation.OPERATION_TIME_LAPS_RECORDING_STOP) {

			type = 2;
			action = 0;

		} else if (mOperation == Operation.OPERATION_TIME_LAPS_RECORDING_PAUSE) {

			type = 2;
			action = 2;

		} else if (mOperation == Operation.OPERATION_TIME_LAPS_RECORDING_RESUME) {

			type = 2;
			action = 3;

		} else if (mOperation == Operation.OPERATION_GET_DR_STATUS) {

			type = 4;
			action = 4;

		} else if (mOperation == Operation.OPERATION_GET_FREE_SPACE) {

			type = 5;
			action = 4;
			
		} else if (mOperation == Operation.OPERATION_GET_TIME_LAPS_SETTING){

			type = 6;
			action = 4;

		} else if (mOperation == Operation.OPERATION_BROADCAST_START) {

			type = 1;
			action = 1;

		} else if (mOperation == Operation.OPERATION_BROADCAST_STOP) {
			
			type = 1;
			action = 0;
			
		} else {

			sendMessage(false, -1, null);
			return;
		}

		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.OPERATION_STATUS_EVENT));

		byte[] operationArray = {type, action};
		futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.OPERATION_REQUEST, operationArray));
		if (futureC.get() == null) {
			sendMessage(false, -1, null);
			return;
		}

		result = futureA.get();
		
		if (result != null) {

			int operationResult = GcBleGattAttributeUtil.getOperationResult(result, mOperation);
			
			if (operationResult == 0) {
				
				sendMessage(true, operationResult, result);
				
			} else {
				
				sendMessage(true, operationResult, null);
			}
			
		} else {
			
			sendMessage(false, -1, null);
		}
	}
	
	
	private void sendMessage(boolean result, int operationResult, BluetoothGattCharacteristic characteristic) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_OPERATION_LTEVENT) {

				outMsg.what = IGcConnectivityService.CB_SET_OPERATION_LTEVENT_RESULT;

			} else if (mAction == ACTION_CLR_OPERATION_LTEVENT) {
			
				outMsg.what = IGcConnectivityService.CB_CLR_OPERATION_LTEVENT_RESULT;

			} else if (mAction == ACTION_SET_OPERATION) {

				outMsg.what = IGcConnectivityService.CB_SET_OPERATION_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (mOperation != Operation.OPERATION_NONE) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_OPERATION, mOperation);
				outData.putInt(IGcConnectivityService.PARAM_OPERATION_ERROR_CODE, operationResult);
				
				if (operationResult == 0x00) {
					
					if (mOperation.equals(Operation.OPERATION_GET_DR_STATUS)) {
						
						byte[] value = characteristic.getValue();
						int drStatus = value[2];
						int count = GcBleGattAttributeUtil.byteArrayToInt(value, 3);

						outData.putInt(IGcConnectivityService.PARAM_DR_STATUS, drStatus);
						outData.putInt(IGcConnectivityService.PARAM_DR_STATUS_COUNT, count);
						
					} else if (mOperation.equals(Operation.OPERATION_GET_FREE_SPACE)) {

						byte[] value = characteristic.getValue();
						long freeSpace = GcBleGattAttributeUtil.byteArrayToLong(value, 2);
						long totalSpace = GcBleGattAttributeUtil.byteArrayToLong(value, 10);
						
						outData.putLong(IGcConnectivityService.PARAM_FREE_SPACE, freeSpace);
						outData.putLong(IGcConnectivityService.PARAM_TOTAL_SPACE, totalSpace);
					}
				}
			}

			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, -1, null);
	}
}
