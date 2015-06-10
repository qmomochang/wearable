package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupProviderIdIndex;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.BackupTokenType;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcSetAutoBackupProviderTokenTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcSetAutoBackupProviderTokenTask";
	
	private BluetoothDevice mBluetoothDevice;
	private BackupProviderIdIndex mProviderIndex;
	private BackupTokenType mTokenType;
	private String mToken;
	
	
	public GcSetAutoBackupProviderTokenTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, BackupProviderIdIndex providerIndex, BackupTokenType tokenType, String token) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
	
		mBluetoothDevice = device;
		mProviderIndex = providerIndex;
		mTokenType = tokenType;
		mToken = token;
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
		
		// set token
		if (!write(mBluetoothDevice, APP_ID_AUTOBACKUP, getTable(mProviderIndex), getKey(mTokenType), mToken)) {
			Log.d(TAG, "[MGCC] write token fail");
			sendFailMessage(mWriteError);
			return;
		}
		
		// set provider
		if (!write(mBluetoothDevice, APP_ID_AUTOBACKUP, "pf", "pvdr", getValue(mProviderIndex))) {
			Log.d(TAG, "[MGCC] write pvdr fail");
			sendFailMessage(mWriteError);
			return;
		}
		
		sendSuccessMessage();
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_TOKEN_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage() {
		try {
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_SET_AUTO_BACKUP_TOKEN_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_AUTO_BACKUP_ERROR_CODE, Common.ERROR_SUCCESS);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private String getTable(BackupProviderIdIndex providerIndex) {
		switch (providerIndex) {
		case PROVIDER_NONE:
			return "none";
		case PROVIDER_DROPBOX:
			return "db";
		case PROVIDER_GOOGLEDRIVE:
			return "gd";
		case PROVIDER_AUTOSAVE:
			return "as";
		case PROVIDER_BAIDU:
			return "bd";
		default:
			return "";
		}
	}
	
	
	
	private String getKey(BackupTokenType tokenType) {
		switch (tokenType) {
		case TOKENTYPE_ACCESS:
			return "tk_acc";
		case TOKENTYPE_REFLESH:
			return "tk_ref";
		case TOKENTYPE_CLIENTID:
			return "tk_cid";
		case TOKENTYPE_CLIENTSELECT:
			return "tk_csec";
		default:
			return "";
		}
	}
	
	
	private String getValue(BackupProviderIdIndex providerIndex) {
		switch (providerIndex) {
		case PROVIDER_NONE:
			return "none";
		case PROVIDER_DROPBOX:
			return "db";
		case PROVIDER_GOOGLEDRIVE:
			return "gd";
		case PROVIDER_AUTOSAVE:
			return "as";
		case PROVIDER_BAIDU:
			return "bd";
		default:
			return "";
		}
	}
}
