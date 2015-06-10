package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.VerifyPasswordStatus;
import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleSetNotificationCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v2.internal.component.wifi.GcWifiTransceiver;

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
		byte[] passwordArray = new byte[temp0.length + 1];
		for (int cnt = 0; cnt < temp0.length; cnt++) {
			passwordArray[cnt + 1] = (byte) temp0[cnt];
		}

		if (mAction == ACTION_VERIFY_PASSWORD) {

			Future<BluetoothGattCharacteristic> futureA, futureB, futureC;

			passwordArray[0] = 0;
			
			futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PSW_VERIFY));
			futureB = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PSW_VERIFY, true));
			if (futureB.get() == null) {
				
				sendMessage(false, null);
				unregisterNotify(GcBleGattAttributes.GC_PSW_VERIFY);
				return;
			}
			
			futureC = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PSW_ACTION, passwordArray));
			if (futureC.get() == null) {

				sendMessage(false, null);
				unregisterNotify(GcBleGattAttributes.GC_PSW_VERIFY);
				return;
			}
			
			result = futureA.get();
			if (result != null) {
				
				byte[] status = result.getValue();
				
				if (status[0] == 0) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_NOT_CHANGED_AND_CORRECT);
					
				} else if (status[0] == 1) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_NOT_CHANGED_AND_INCORRECT);
					
				} else if (status[0] == 2) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_CHANGED_AND_CORRECT);
					
				} else if (status[0] == 3) {
					
					sendMessage(true, VerifyPasswordStatus.VPSTATUS_CHANGED_AND_INCORRECT);
					
				} else {
					
					sendMessage(false, null);
				}
				
			} else {

				sendMessage(false, null);
			}
			
			unregisterNotify(GcBleGattAttributes.GC_PSW_VERIFY);
			
		} else if (mAction == ACTION_CHANGE_PASSWORD) {
			
			Future<BluetoothGattCharacteristic> future;
			
			passwordArray[0] = 1;

			future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PSW_ACTION, passwordArray));
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
	
	
	
	private void unregisterNotify(String uuidString) throws Exception {
		
		Future<BluetoothGattCharacteristic> future;
		
		future = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, uuidString, false));
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
