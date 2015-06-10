package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
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



public class GcSetAutoBackupPreferenceTask extends GcConnectivityTask {

	private final static String TAG = "GcSetAutoBackupPreferenceTask";
	
	private BluetoothDevice mBluetoothDevice;
	private boolean mEnableBackup;
	private boolean mDeleteAfterBackup;
	private boolean mBackupWithoutAC;
	
	
	public GcSetAutoBackupPreferenceTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, boolean enableBackup, boolean deleteAfterBackup, boolean backupWithoutAC) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
		mEnableBackup = enableBackup;
		mDeleteAfterBackup = deleteAfterBackup;
		mBackupWithoutAC = backupWithoutAC;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		Integer bootResult = futureBoot.get();
		if (bootResult != Common.ERROR_SUCCESS) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false, Common.ERROR_BOOT_UP_GC);
			return;
		}
		
		byte[] writeData = new byte[4];
		writeData[0] = (byte) 0x00; // set preference
		writeData[1] = mEnableBackup ? (byte) 0x01 : (byte) 0x00;
		writeData[2] = mDeleteAfterBackup ? (byte) 0x01 : (byte)0x00;
		writeData[3] = mBackupWithoutAC ? (byte) 0x01 : (byte) 0x00;
		
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.BACKUP_PERFERENCE_REQUEST, writeData));
		if (future.get() != null) {
			sendMessage(true, Common.ERROR_SUCCESS);
		} else {
			sendMessage(false, Common.ERROR_GATT_WRITE);
		}
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendMessage(false, Common.ERROR_FAIL);
	}
	
	private void sendMessage(boolean result, int errorCode) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_PREFERENCE_RESULT;
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
