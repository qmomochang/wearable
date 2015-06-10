package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;



public class GcGeneralPurposeCommandNotifyTask extends GcGeneralPurposeCommandTask {
	
	public final static int ACTION_SET_LTEVENT = 0;
	public final static int ACTION_CLR_LTEVENT = 1;
	
	private final static String TAG = "GcGeneralPurposeCommandNotifyTask";
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	public GcGeneralPurposeCommandNotifyTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		sendMessage(true);
		
		super.to(TAG);
	}
	
	@Override
	public void error(Exception e) {

		sendMessage(false);
	}
	
	private void sendMessage(boolean result) {
		try {
			Message outMsg = Message.obtain();
			
			switch (mAction) {
			case ACTION_SET_LTEVENT:
				outMsg.what = IGcConnectivityService.CB_SET_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT;
				break;
			case ACTION_CLR_LTEVENT:
				outMsg.what = IGcConnectivityService.CB_CLR_GENERAL_PURPOSE_COMMAND_LTNOTIFY_RESULT;
				break;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			} else {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
