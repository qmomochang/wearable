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



public class GcLTECampingStatusTask extends GcGeneralPurposeCommandTask {
	
	public final static int ACTION_GET = 0;
	public final static int ACTION_SET_LTEVENT = 1;
	public final static int ACTION_CLR_LTEVENT = 2;
	
	private final static String TAG = "GcLTECampingStatusTask";
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	public GcLTECampingStatusTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {
		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		switch (mAction) {
		case ACTION_GET:
			getStatus();
			break;
		case ACTION_SET_LTEVENT:
			setLTEvent();
			break;
		case ACTION_CLR_LTEVENT:
			clearLTEvent();
			break;
		}
		
		super.to(TAG);
	}
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null);
	}
	
	private void getStatus() throws Exception{
		Future<Integer> futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
		Integer bootResult = futureBoot.get();
		if (bootResult != Common.ERROR_SUCCESS) {
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false, null);
			return;
		}
		
		Future<BluetoothGattCharacteristic> futureNotify = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT));
		Future<BluetoothGattCharacteristic> future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice,GcBleGattAttributes.GcV2CommandEnum.LTE_CAMPING_STATUS_REQUEST_EVENT, new byte[0]));
		BluetoothGattCharacteristic result = future.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] write fail");
			sendMessage(false, null);
			return;
		}
		
		result = futureNotify.get();
		if (result == null) {
			Log.d(TAG, "[MGCC] receive notify fail");
			sendMessage(false, null);
			return;
		}
		
		byte[] data = result.getValue();
		if (data == null || data.length < 2) {
			Log.d(TAG, "[MGCC] invalid data");
			sendMessage(false, null);
			return;
		}
		
		IGcConnectivityService.LTECampingStatus lteCampingStatus = IGcConnectivityService.LTECampingStatus.findStatus(data[1]);
		if (lteCampingStatus == null) {
			Log.d(TAG, "[MGCC] invalid lte camping status:" + data[1]);
			sendMessage(false, null);
			return;
		}
		
		sendMessage(true, lteCampingStatus);
	}
	
	private void setLTEvent() {
		sendMessage(true, null);
	}
	
	private void clearLTEvent() {
		sendMessage(true, null);
	}
	
	private void sendMessage(boolean result, IGcConnectivityService.LTECampingStatus lteCampingStatus) {
		try {
			Message outMsg = Message.obtain();
			
			switch (mAction) {
			case ACTION_GET:
				outMsg.what = IGcConnectivityService.CB_GET_LTE_CAMPING_STATUS_RESULT;
				break;
			case ACTION_SET_LTEVENT:
				outMsg.what = IGcConnectivityService.CB_SET_LTE_CAMPING_STATUS_LTEVENT_RESULT;
				break;
			case ACTION_CLR_LTEVENT:
				outMsg.what = IGcConnectivityService.CB_CLR_LTE_CAMPING_STATUS_LTEVENT_RESULT;
				break;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			} else {
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (lteCampingStatus != null) {
				outData.putSerializable(IGcConnectivityService.PARAM_LTE_CAMPING_STATUS, lteCampingStatus);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
}
