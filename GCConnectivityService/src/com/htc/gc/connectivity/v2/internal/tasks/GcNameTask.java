package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleReadCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
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



public class GcNameTask extends GcConnectivityTask {

	private final static String TAG = "GcNameTask";

	public final static int ACTION_SET_NAME = 0;
	public final static int ACTION_GET_NAME = 1;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	private String mName;
	
	
	
	public GcNameTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action, String name) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
		
		if (name != null) {
			
			mName = name;
			
		} else {
			
			mName = "hTC GC";
		}
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		BluetoothGattCharacteristic result;
		Future<BluetoothGattCharacteristic> future;
		
		if (mAction == ACTION_SET_NAME) {
			
			char[] temp = mName.toCharArray();
			
			if (temp.length > 15) {
				
				sendMessage(false, null);
				return;
			}
			
			byte[] nameArray = new byte[temp.length];
			for (int cnt = 0; cnt < temp.length; cnt++) {
				nameArray[cnt] = (byte) temp[cnt];
			}
			
			future = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_NAME, nameArray));

			result = future.get();
			
			if (result != null) {
				
				sendMessage(true, null);

			} else {
				
				sendMessage(false, null);
			}

		} else if (mAction == ACTION_GET_NAME) {

			future = mExecutor.submit(new GcBleReadCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_NAME));
			
			result = future.get();
			
			if (result != null) {
				
				String name = GcBleGattAttributeUtil.getGcName(result);

				sendMessage(true, name);
				
			} else {
				
				sendMessage(false, null);
			}
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result, String name) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_NAME) {
			
				outMsg.what = IGcConnectivityService.CB_SET_NAME_RESULT;
				
			} else if (mAction == ACTION_GET_NAME) {
				
				outMsg.what = IGcConnectivityService.CB_GET_NAME_RESULT;
			}
			
			Bundle outData = new Bundle();
			
			if (result) {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
				
			} else {
				
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
			}
			
			if (name != null) {
				
				outData.putString(IGcConnectivityService.PARAM_GC_NAME, name);
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
