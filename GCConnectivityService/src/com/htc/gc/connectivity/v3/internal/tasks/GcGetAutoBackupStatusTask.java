package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupProcessStatus;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupProviderIdIndex;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
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



public class GcGetAutoBackupStatusTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcGetAutoBackupStatusTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcGetAutoBackupStatusTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (!bootup(mBluetoothDevice)) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendFailMessage(Common.ERROR_BOOT_UP_GC);
			return;
		}
		
		Future<BluetoothGattCharacteristic> futureNotify;
		Future<BluetoothGattCharacteristic> future;
		byte[] writeData;
		BluetoothGattCharacteristic result;
		
		BackupProcessStatus lastBackupStatus;
		BackupProviderIdIndex providerIndex;
		Calendar lastBackupTime;
		int unbackupItemNumber;
		int totalItemNumber;
		
		// ---------------
		// get from 0x82
		// ---------------
		// unbackup item number & total item number
		futureNotify = mExecutor.submit(
				new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.BACKUP_GET_STATUS_EVENT));
		writeData = new byte[0];
		future = mExecutor.submit(
				new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.BACKUP_GET_STATUS_EVENT, writeData));
		if (future.get() == null) {
			sendFailMessage(Common.ERROR_GATT_WRITE);
			return;
		}
		result = futureNotify.get();
		if (result == null) {
			sendFailMessage(Common.ERROR_GATT_RECEIVE_NOTIFICATION);
			return;
		}
		byte[] resultData = result.getValue();
		if (resultData[0] != GcBleGattAttributes.GcV2CommandEnum.BACKUP_GET_STATUS_EVENT.getID()) {
			sendFailMessage(Common.ERROR_FAIL);
			return;
		}
		
		unbackupItemNumber = GcBleGattAttributeUtil.byteArrayToInt(resultData, 1);
		totalItemNumber = GcBleGattAttributeUtil.byteArrayToInt(resultData, 5);
		
		// ---------------
		// get from 0x91 command
		// ---------------
		// last backup status
		if (!read(mBluetoothDevice, APP_ID_AUTOBACKUP, "pf", "lstatus")) {
			Log.d(TAG, "[MGCC] read lstatus fail");
			sendFailMessage(mReadError);
			return;
		}
		lastBackupStatus = BackupProcessStatus.findStatus((byte)Integer.parseInt(mReadValue));
		
		// provider
		if (!read(mBluetoothDevice, APP_ID_AUTOBACKUP, "pf", "pvdr")) {
			Log.d(TAG, "[MGCC] read pvdr fail");
			sendFailMessage(mReadError);
			return;
		}
		providerIndex = toBackupProviderIdIndex(mReadValue);
		
		// last backup time
		if (!read(mBluetoothDevice, APP_ID_AUTOBACKUP, "pf", "ltime")) {
			Log.d(TAG, "[MGCC] read ltime fail");
			sendFailMessage(mReadError);
			return;
		}
		lastBackupTime = Calendar.getInstance();
		lastBackupTime.setTimeInMillis(Integer.parseInt(mReadValue) * 1000L);
		
		sendSuccessMessage(lastBackupStatus, providerIndex, unbackupItemNumber, totalItemNumber, lastBackupTime);
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage(BackupProcessStatus lastBackupStatus, BackupProviderIdIndex providerIndex, int unbackupItemNumber, int totalItemNumber, Calendar lastBackupTime) {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_AUTO_BACKUP_STATUS_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, Common.ERROR_SUCCESS);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROCESS_STATUS, lastBackupStatus);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_PROVIDER_INDEX, providerIndex);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_UNBACKUP_ITEM_NUMBER, unbackupItemNumber);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_TOTAL_ITEM_NUMBER, totalItemNumber);
			outData.putSerializable(IGcConnectivityService.PARAM_AUTO_BACKUP_LAST_BACKUP_DATE_TIME, lastBackupTime);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private BackupProviderIdIndex toBackupProviderIdIndex(String data) {
		if (data.equals("none")) {
			return BackupProviderIdIndex.PROVIDER_NONE;
		} else if (data.equals("db")) {
			return BackupProviderIdIndex.PROVIDER_DROPBOX;
		} else if (data.equals("gd")) {
			return BackupProviderIdIndex.PROVIDER_GOOGLEDRIVE;
		} else if (data.equals("as")) {
			return BackupProviderIdIndex.PROVIDER_AUTOSAVE;
		} else if (data.equals("bd")) {
			return BackupProviderIdIndex.PROVIDER_BAIDU;
		} else {
			return null;
		}
	}
}
