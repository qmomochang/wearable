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



public class GcGetModemStatusTask extends GcGeneralPurposeCommandTask {
	
	private final static String TAG = "GcGetModemStatusTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	public GcGetModemStatusTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		getStatus();
		
		super.to(TAG);
	}
	
	@Override
	public void error(Exception e) {

		sendFailMessage();
	}
	
	private void getStatus() throws Exception{
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
		Integer bootResult = futureBoot.get();
		if (bootResult != Common.ERROR_SUCCESS) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendFailMessage();
			return;
		}
		
		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GET_MODEM_STATUS_REQUEST_EVENT));
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice,GcBleGattAttributes.GcV2CommandEnum.GET_MODEM_STATUS_REQUEST_EVENT, new byte[0]));
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
		
		IGcConnectivityService.SimLockType simLockType = IGcConnectivityService.SimLockType.findType(data[1]);
		if (simLockType == null) {
			Log.d(TAG, "[MGCC] invalid sim lock type:" + data[1]);
			sendFailMessage();
			return;
		}
		
		sendSuccessMessage(simLockType, data[2], data[3]);
	}
	
	private void sendSuccessMessage(IGcConnectivityService.SimLockType simLockType, int pinRetryCount, int pukRetryCount) {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_MODEM_STATUS_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			
			outData.putSerializable(IGcConnectivityService.PARAM_SIM_LOCK_TYPE, simLockType);
			outData.putInt(IGcConnectivityService.PARAM_SIM_PIN_RETRY_COUNT, pinRetryCount);
			outData.putInt(IGcConnectivityService.PARAM_SIM_PUK_RETRY_COUNT, pukRetryCount);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	private void sendFailMessage() {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_MODEM_STATUS_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
