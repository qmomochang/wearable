package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReliableWriteCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v2.internal.component.le.GcBleGattAttributeUtil;
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



public class GcWifiDisconnectTask extends GcConnectivityTask {

	private final static String TAG = "GcWifiDisconnectTask";
	
	protected BluetoothDevice mBluetoothDevice;
	
	
	
	public GcWifiDisconnectTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;

	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		if (mGcBleTransceiver != null) {

			Integer result;
			Future<Integer> future;
			
			future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			result = future.get();
			
			if (result == Common.ERROR_SUCCESS) {
				
				disconnectWifi();

			} else {
				
				sendMessage(false, result);
			}
		}
		
		super.to(TAG);
	}

	
	
	private void disconnectWifi() throws Exception {
		
		Log.d(TAG, "[MGCC] Wifi disconnecting...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB, futureC, futureD, futureE;

		byte[] serverArray = {(byte) 0x01, (byte) 0x00, 'W', 'T'};
		futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_WIFI_SERVER_BAND, serverArray));
		if (futureA.get() == null) {
			
			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}

		futureB = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR, 10000));
		///futureC = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_ERROR, true));
		///if (futureC.get() == null) {
			
			///sendMessage(false, Common.ERROR_GATT_SET_NOTIFICATION);
			///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
			///return;
		///}

		byte[] cfgArray = {(byte) 1, (byte) 4, (byte) 2};
		futureD = mExecutor.submit(new GcBleReliableWriteCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, GcBleGattAttributes.GC_PHONE_WIFI_CFG, cfgArray));
		if (futureD.get() == null) {
			
			sendMessage(false, Common.ERROR_GATT_WRITE);
			///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
			return;
		}

		result = futureB.get();
		
		if (result != null) {
		
			Log.d(TAG, "[MGCC] future result B = " + result.getUuid());
			
			byte[] disconnectResult = GcBleGattAttributeUtil.getWifiConnectResult(result);
			
			if (disconnectResult[1] == 0x00) {
				
				Log.d(TAG, "[MGCC] Wifi disconnect successful!!");

				sendMessage(true, 0);
				
			} else {
				
				Log.d(TAG, "[MGCC] WIFI disconnect fail, Error code = " + disconnectResult[1]);
				
				sendMessage(false, (int)disconnectResult[1]);
			}
			
		} else {
			
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
		
		///unregisterNotify(GcBleGattAttributes.GC_PHONE_WIFI_ERROR);
	}
	
    
	private void sendMessage(boolean result, int errorCode) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_WIFI_DISCONNECT_RESULT;
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
				outData.putInt(IGcConnectivityService.PARAM_WIFI_ERROR_CODE, errorCode);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, Common.ERROR_FAIL);
	}
}
