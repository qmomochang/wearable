package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.PlugIO;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService.MCUBatteryLevel;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReceiveNotificationCallable;
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
		Future<BluetoothGattCharacteristic> future, futureA;

		if (mAction == ACTION_GET_HW_STATUS) {
			Future<Integer> futureBoot;
			Integer bootResult;
			futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_LINUX, mMessenger));
			bootResult = futureBoot.get();
			if (bootResult == Common.ERROR_SUCCESS)
			{
			
				byte[] getType = {(byte)0x7};
				futureA = mExecutor.submit(new GcBleReceiveNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.HWSTATUS_EVENT));
				future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.HWSTATUS_EVENT, getType));
				result = future.get();
				if (result != null)
				{
					result = futureA.get();
					if (result != null)
					{
						int battery_cap = GcBleGattAttributeUtil.getHwStatus_BatteryLevel(result);
						int usb_status  = GcBleGattAttributeUtil.getHwStatus_USBStatus(result);
						int adp_status  = GcBleGattAttributeUtil.getHwStatus_AdapterStatus(result);
						
						sendMessage(true, battery_cap, usb_status, adp_status);
					}
					else
					{
						sendMessage(false, -1, -1, -1);
					}
				}
				else
				{
					sendMessage(false, -1, -1, -1);
				}
			}
			else
			{
				Log.d(TAG, "[MGCC] boot up is fail");
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
			
			if (level >= 1) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_BATTERY_LEVEL, MCUBatteryLevel.findLevel(level));
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
