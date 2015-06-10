package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.internal.common.GcConnectivityDevice;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReadCallable;
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



public class GcGetBleFWVersionTask extends GcConnectivityTask {

	private final static String TAG = "GcGetBleFWVersionTask";
	private BluetoothDevice mBluetoothDevice;
	
	
	
	public GcGetBleFWVersionTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> future;

		future = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_DEVICE_INFORMATION, GcBleGattAttributes.GC_FW_REVISION));
		result = future.get();
		if (result != null) {
			
			String version = GcBleGattAttributeUtil.getBleFWVersion(result);
			
			GcConnectivityDevice gcDevice = mGcBleTransceiver.getGcConnectivityDeviceGroup().getDevice(mBluetoothDevice);
			Log.d(TAG, "[MGCC] gcDevice = " + gcDevice);
			if (gcDevice != null) {

				Integer value = Integer.parseInt(version);
				if (value != null) {

					gcDevice.setVersionBle((int)(value));
				}
			}
			
			sendMessage(true, version);
			
		} else {
			
			sendMessage(false, null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, String version) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_GET_BLE_FW_VERSION_RESULT;
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (version != null) {
				
				outData.putString(IGcConnectivityService.PARAM_BLE_FW_VERSION, version);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null);
	}
}
