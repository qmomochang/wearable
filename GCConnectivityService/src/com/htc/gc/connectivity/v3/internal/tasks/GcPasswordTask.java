package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.VerifyPasswordStatus;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleSetNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
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



public class GcPasswordTask extends GcConnectivityTask {

	private final static String TAG = "GcPasswordTask";
	
	public final static int ACTION_VERIFY_PASSWORD = 0;
	public final static int ACTION_CHANGE_PASSWORD = 1;
	public final static int DATA_ADD_END = 1;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private String mPassword;
	
	
	
	public GcPasswordTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, String password) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		mPassword = password;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;

		char[] temp0 = mPassword.toCharArray();
		byte[] passwordArray = new byte[temp0.length + 1 + DATA_ADD_END];
		passwordArray[temp0.length + 1] = (byte)0x00;
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			passwordArray[cnt + 1] = (byte) temp0[cnt];
		}

		if (mAction == ACTION_VERIFY_PASSWORD) {

			Future<BluetoothGattCharacteristic> futureA, futureB, futureC;

			passwordArray[0] = 0;
			
			futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_EVENT));
			futureB = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_EVENT, true));
			if (futureB.get() == null) {
				
				sendMessage(false, null);
				unregisterNotify(GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_EVENT);
				return;
			}
			
			futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_REQUEST, passwordArray));
			if (futureC.get() == null) {

				sendMessage(false, null);
				unregisterNotify(GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_EVENT);
				return;
			}
			
			result = futureA.get();
			if (result != null) {
				
				byte[] status = result.getValue();
				
				if (status[1] == 0) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_NOT_CHANGED_AND_CORRECT);
					
				} else if (status[1] == 1) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_NOT_CHANGED_AND_INCORRECT);
					
				} else if (status[1] == 2) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_CHANGED_AND_CORRECT);
					
				} else if (status[1] == 3) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_CHANGED_AND_INCORRECT);
					
				} else {
					
					sendMessage(false, null);
				}
				
			} else {

				sendMessage(false, null);
			}
			
			unregisterNotify(GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_EVENT);
			
		} else if (mAction == ACTION_CHANGE_PASSWORD) {
			
			Future<BluetoothGattCharacteristic> future;
			
			passwordArray[0] = 1;

			future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.VERIFY_PASSWORD_REQUEST, passwordArray));
			result = future.get();
			
			if (result != null) {
				
				sendMessage(true, null);

			} else {
				
				sendMessage(false, null);
			}
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, VerifyPasswordStatus status) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_VERIFY_PASSWORD) {
				
				outMsg.what = IGcConnectivityService.CB_VERIFY_PASSWORD_RESULT;
				
			} else if (mAction == ACTION_CHANGE_PASSWORD) {
				
				outMsg.what = IGcConnectivityService.CB_CHANGE_PASSWORD_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (status != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_VERIFY_PASSWORD_STATUS, status);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void unregisterNotify(GcBleGattAttributes.GcV2CommandEnum commandID) throws Exception {
		
		Future<BluetoothGattCharacteristic> future;
		
		future = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, commandID, false));
		if (future.get() == null) {

			Log.d(TAG, "[MGCC] unregisterNotify error!!!");
			return;
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null);
	}
}
