package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcGetBroadcastInvitationListTask extends GcGeneralPurposeCommandTask {

	private final static String TAG = "GcGetBroadcastInvitationListTask";
	
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcGetBroadcastInvitationListTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

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
		
		if (!read(mBluetoothDevice, APP_ID_BROADCAST, "pf", "invtnum")) {
			sendFailMessage(mReadError);
			return;
		}
		
		int inviteeNum = Integer.parseInt(mReadValue);
		int inviteeIndex = 0;
		ArrayList<String> invitationList = new ArrayList<String>();
		String key = "";
		for (int i = 0; i < inviteeNum; ++i) {
			key = "invt" + inviteeIndex;
			if (!read(mBluetoothDevice, APP_ID_BROADCAST, "pf", key)) {
				Log.d(TAG, "[MGCC] read " + key + "fail");
				sendFailMessage(mReadError);
				return;
			}
			invitationList.add(mReadValue);
			inviteeIndex++;
		}
		
		sendSuccessMessage(invitationList);
		
		super.to(TAG);
	}
	
	
	
	@Override
	public void error(Exception e) {
		sendFailMessage(Common.ERROR_FAIL);
	}
	
	
	
	private void sendFailMessage(int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_INVITATION_LIST_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, errorCode);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	private void sendSuccessMessage(ArrayList<String> invitationList) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			outMsg.what = IGcConnectivityService.CB_GET_BROADCAST_INVITATION_LIST_RESULT;
			
			Bundle outData = new Bundle();
			
			outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			outData.putInt(IGcConnectivityService.PARAM_BROADCAST_ERROR_CODE, IGcConnectivityService.ERROR_SUCCESS);
			outData.putStringArrayList(IGcConnectivityService.PARAM_BROADCAST_INVITATION_LIST, invitationList);
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
