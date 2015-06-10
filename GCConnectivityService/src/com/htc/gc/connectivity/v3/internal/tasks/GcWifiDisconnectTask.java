package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigBand;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.WifiConfigureType;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReliableWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleSetNotificationCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
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

			Integer bootResult;
			Future<Integer> futureBoot;
			
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			bootResult = futureBoot.get();
			
			if (bootResult == Common.ERROR_SUCCESS) {

				disconnectWifi();

			} else {
				Log.d(TAG, "[MGCC] boot up is fail");
				sendMessage(false, bootResult);
			}
		}
		
		super.to(TAG);
	}

	
	
	private void disconnectWifi() throws Exception {
		
		Log.d(TAG, "[MGCC] Wifi disconnecting...");
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> futureA, futureB;


		futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_STATUS_EVENT, 10000));

		byte[] serverArray = {(byte)(byte) WifiConfigureType.WIFI_DISCONN.getType(), 'W', 'T'};
		futureB = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.WIFI_CONFIG_REQUEST, serverArray));
		if (futureB.get() == null) {
			sendMessage(false, Common.ERROR_GATT_WRITE);
			return;
		}
		
		result = futureA.get();
		if (result != null) {
			Log.d(TAG, "[MGCC] future result A = " + result.getUuid());
			byte[] disconnectResult = GcBleGattAttributeUtil.getWifiDisconnectResult(result);
			if (disconnectResult[0] == 0x00) {
				Log.d(TAG, "[MGCC] Wifi disconnect successful!!");
				sendMessage(true, 0);
			} else {
				Log.d(TAG, "[MGCC] WIFI disconnect fail, Error code = " + disconnectResult[0]);
				sendMessage(false, (int)disconnectResult[0]);
			}
		} else {
			sendMessage(false, Common.ERROR_GATT_RECEIVE_NOTIFICATION);
		}
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
