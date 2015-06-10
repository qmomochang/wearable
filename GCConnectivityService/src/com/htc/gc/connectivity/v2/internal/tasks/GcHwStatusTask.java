package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
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



public class GcHwStatusTask extends GcConnectivityTask {

	private final static String TAG = "GcHwStatusTask";
	
	public final static int ACTION_SET_HW_STATUS_LTEVENT = 0;
	public final static int ACTION_CLR_HW_STATUS_LTEVENT = 1;
	public final static int ACTION_GET_HW_STATUS = 2;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	
	
	public GcHwStatusTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> future;

		if (mAction == ACTION_GET_HW_STATUS) {
			
			Integer resultBoot;
			Future<Integer> futureBoot;
			
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
			resultBoot = futureBoot.get();
			
			if (resultBoot == Common.ERROR_SUCCESS) {
				
				future = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_HW_STATUS));
				result = future.get();
				if (result != null) {
					
					int level = GcBleGattAttributeUtil.getHwStatus(result, 0, false);
					int usbStorage = GcBleGattAttributeUtil.getHwStatus(result, 1, false);
					int adapterPlugin = GcBleGattAttributeUtil.getHwStatus(result, 2, false);
					
					sendMessage(true, level, usbStorage, adapterPlugin);
					
				} else {
					
					sendMessage(false, -1, -1, -1);
				}

			} else {
				
				sendMessage(false, -1, -1, -1);
			}

		} else if (mAction == ACTION_SET_HW_STATUS_LTEVENT) {
			
			sendMessage(true, -1, -1, -1);
			
		} else if (mAction == ACTION_CLR_HW_STATUS_LTEVENT) {
			
			sendMessage(true, -1, -1, -1);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, int level, int usbStorage, int adapterPlugin) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_GET_HW_STATUS) {
				
				outMsg.what = IGcConnectivityService.CB_GET_HW_STATUS_RESULT;
				
			} else if (mAction == ACTION_SET_HW_STATUS_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_SET_HW_STATUS_LTEVENT_RESULT;

			} else if (mAction == ACTION_CLR_HW_STATUS_LTEVENT) {
				
				outMsg.what = IGcConnectivityService.CB_CLR_HW_STATUS_LTEVENT_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (level >= 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL, (Integer) level);
			}

			if (usbStorage == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_USB_STORAGE, PlugIO.PLUG_OUT);
				
			} else if (usbStorage == 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_USB_STORAGE, PlugIO.PLUG_IN);
			}

			if (adapterPlugin == 0) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN, PlugIO.PLUG_OUT);
				
			} else if (adapterPlugin == 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_ADAPTER_PLUGIN, PlugIO.PLUG_IN);
			}
			
			outMsg.setData(outData);
		
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false, -1, -1, -1);
	}
}
