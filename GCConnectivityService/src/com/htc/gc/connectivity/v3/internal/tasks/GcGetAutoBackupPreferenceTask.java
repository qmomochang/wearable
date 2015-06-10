package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
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



public class GcGetAutoBackupPreferenceTask extends GcConnectivityTask {

	private final static String TAG = "GcGetAutoBackupPreferenceTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcGetAutoBackupPreferenceTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		Integer bootResult = futureBoot.get();
		if (bootResult != Common.ERROR_SUCCESS) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendFailMessage(Common.ERROR_BOOT_UP_GC);
			return;
		}

		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(
				new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.BACKUP_PERFERENCE_REQUEST));
		
		byte[] writeData = new byte[1];
		writeData[0] = 0x01; // get preference
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(
				new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.BACKUP_PERFERENCE_REQUEST, writeData));
		if (future.get() == null) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}
		
		BluetoothGattCharacteristic result = futureNotify.get();
		if (result == null) {
			sendFailMessage(Common.ERROR_GATT_RECEIVE_NOTIFICATION);
			return;
		}
		
		byte[] resultData = result.getValue();
		// resultData[0] is command
		// resultData[1] is 0x01, get preference
		boolean isEnableBackup = (resultData[2] == (byte) 0x01);
		boolean isDeleteAfterBackup = (resultData[3] == (byte) 0x01);
		boolean isBackupWithoutAC = (resultData[4] == (byte) 0x01);
		sendSuccessMessage(isEnableBackup, isDeleteAfterBackup, isBackupWithoutAC);
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	private void sendFailMessage(int errorCode) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_PREFERENCE_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	private void sendSuccessMessage(boolean isEnableBackup, boolean isDeleteAfterBackup, boolean isBackupWithoutAC) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_PREFERENCE_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, Common.ERROR_SUCCESS);
			outData.putBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_ENABLE_BACKUP, isEnableBackup);
			outData.putBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_DELETE_AFTER_BACKUP, isDeleteAfterBackup);
			outData.putBoolean(IGcConnectivityService.PARAM_AUTO_BACKUP_IS_BACKUP_WITHOUT_AC, isBackupWithoutAC);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
