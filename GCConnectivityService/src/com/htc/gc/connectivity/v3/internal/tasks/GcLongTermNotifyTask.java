package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleSetNotificationCallable;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
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



public class GcLongTermNotifyTask extends GcConnectivityTask {

	private final static String TAG = "GcLongTermNotifyTask";
	
	public final static int ACTION_SET_LTNOTIFY = 0;
	public final static int ACTION_CLR_LTNOTIFY = 1;
	
	private BluetoothDevice mBluetoothDevice;
	private int mAction;
	
	
	
	public GcLongTermNotifyTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int action) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		mAction = action;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		Future<BluetoothGattCharacteristic> futureA, futureB;

		if (mAction == ACTION_SET_LTNOTIFY) {
			
			futureA = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GC_SHORT_COMMAND_NOTIFY, true));
			futureB = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GC_LONG_COMMAND_NOTIFY, true));

			if ((futureA.get() == null) || (futureB.get() == null)) {
			
				sendMessage(false);
				
			} else {
				
				sendMessage(true);
			}

		} else if (mAction == ACTION_CLR_LTNOTIFY) {

			futureA = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GC_SHORT_COMMAND_NOTIFY, false));
			futureB = mExecutor.submit(new GcBleSetNotificationCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.GC_LONG_COMMAND_NOTIFY, false));

			if ((futureA.get() == null) || (futureB.get() == null)) {
			
				sendMessage(false);
				
			} else {
				
				sendMessage(true);
			}
		}

		super.to(TAG);
	}
	
	
	
	private void sendMessage(boolean result) {
		
		try {
			
			Message outMsg = Message.obtain();
			
			if (mAction == ACTION_SET_LTNOTIFY) {
				
				outMsg.what = IGcConnectivityService.CB_SET_LTNOTIFY_RESULT;

			} else if (mAction == ACTION_CLR_LTNOTIFY) {

				outMsg.what = IGcConnectivityService.CB_CLR_LTNOTIFY_RESULT;

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
	
	
	
	@Override
	public void error(Exception e) {

		sendMessage(false);
	}
}
