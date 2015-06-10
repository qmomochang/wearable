package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReadCallable;
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



public class GcGetAllFwVersionTask extends GcConnectivityTask {

	private final static String TAG = "GcGetAllFwVersionTask";
	private BluetoothDevice mBluetoothDevice;
	
	
	
	public GcGetAllFwVersionTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> future;
		
		Integer resultBoot;
		Future<Integer> futureBoot;
		
		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
		resultBoot = futureBoot.get();
		
		if (resultBoot == Common.ERROR_SUCCESS) {
			
			future = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_ALL_FW_VERSION));
			result = future.get();
			if (result != null) {
				
				if (result.getUuid().toString().equals(GcBleGattAttributes.GC_ALL_FW_VERSION)) {
					
					byte[] value = result.getValue();

					short verMainMajor = GcBleGattAttributeUtil.byteArrayToShort(value, 0);
					short verMainMinor = GcBleGattAttributeUtil.byteArrayToShort(value, 2);
					Integer verMain = (Integer)(verMainMajor * 10000 + verMainMinor);
					
					short verBootMajor = GcBleGattAttributeUtil.byteArrayToShort(value, 4);
					short verBootMinor = GcBleGattAttributeUtil.byteArrayToShort(value, 6);
					Integer verBoot = (Integer)(verBootMajor * 10000 + verBootMinor);

					Integer verMcu = (Integer)GcBleGattAttributeUtil.byteArrayToInt(value, 8);
					Integer verBle = (Integer)GcBleGattAttributeUtil.byteArrayToInt(value, 12);

					Log.d(TAG, "[MGCC] Main version = " + verMain + ", Boot version = " + verBoot + ", Mcu version = " + verMcu + ", Ble version = " + verBle);
					
					sendMessage(true, verMain, verBoot, verMcu, verBle);
					
				} else {
					
					sendMessage(false, null, null, null, null);
				}
				
			} else {
				
				sendMessage(false, null, null, null, null);
			}

		} else {
			
			sendMessage(false, null, null, null, null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, Integer verMain, Integer verBoot, Integer verMcu, Integer verBle) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_GET_ALL_FW_VERSION_RESULT;
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (verMain != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_MAIN_FW_VERSION, verMain);
			}

			if (verBoot != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BOOT_FW_VERSION, verBoot);
			}

			if (verMcu != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_MCU_FW_VERSION, verMcu);
			}

			if (verBle != null) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BLE_FW_VERSION, verBle);
			}

			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, null, null, null, null);
	}
}
