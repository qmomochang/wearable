package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
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



public class GcUnlockSimPinTask extends GcGeneralPurposeCommandTask {
	
	private final static String TAG = "GcUnlockSimPinTask";
	
	private BluetoothDevice mBluetoothDevice;
	private String mPinCode;
	
	public GcUnlockSimPinTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, String pinCode) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mPinCode = pinCode;
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		unlock();
		
		super.to(TAG);
	}
	
	@Override
	public void error(Exception e) {

		sendFailMessage();
	}
	
	private void unlock() throws Exception{
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
		Integer bootResult = futureBoot.get();
		if (bootResult != Common.ERROR_SUCCESS) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendFailMessage();
			return;
		}
		
		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SIM_PIN_ACTION_RESULT_EVENT));
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice,GcBleGattAttributes.GcV2CommandEnum.SIM_PIN_ACTION_REQUEST, getWriteData()));
		BluetoothGattCharacteristic result = future.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] write fail");
			sendFailMessage();
			return;
		}
		
		result = futureNotify.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] receive notify fail");
			sendFailMessage();
			return;
		}
		
		byte[] data = result.getValue();
		if (data == null || data.length < 4) {
			Log.d(TAG, "[MGCC] invalid data");
			sendFailMessage();
			return;
		}
		
		sendSuccessMessage((data[1] == 0), data[2]);
	}
	
	private void sendSuccessMessage(boolean unlockResult, int pinRetryCount) {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_UNLOCK_SIM_PIN_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			
			outData.putBoolean(IGcConnectivityService.PARAM_SIM_UNLOCK_PIN_RESULT, unlockResult);
			outData.putInt(IGcConnectivityService.PARAM_SIM_PIN_RETRY_COUNT, pinRetryCount);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	private void sendFailMessage() {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_UNLOCK_SIM_PIN_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	private byte[] getWriteData() {
		byte[] data = new byte[17];
		
		data[0] = ((byte)0x01); // disable pin
		
		byte[] pinCodeData = mPinCode.getBytes();
		System.arraycopy(pinCodeData, 0, data, 1, Math.min(pinCodeData.length, 8)); // max length of pin code is 8
		
		return data;
	}
}
