package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.interfaces.IGcConnectivityServiceBase.SwitchOnOff;
import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcExampleCallable;
import com.htc.gc.connectivity.v3.internal.common.Common;
import com.htc.gc.connectivity.v3.internal.common.GcConnectivityTask;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcBootTask extends GcConnectivityTask {

	private final static String TAG = "GcBootTask";
	private BluetoothDevice mBluetoothDevice;
	
	
	public GcBootTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		Integer resultBoot;
		Future<Integer> futureBoot;
		
		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		resultBoot = futureBoot.get();
		
		if (resultBoot == Common.ERROR_SUCCESS) {
			sendMessage(SwitchOnOff.SWITCH_ON);
		}
	}
	
	private void sendMessage(SwitchOnOff bootupReady) {
		
		try {
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_SET_POWER_ONOFF_RESULT;
			
			Bundle outData = new Bundle();
			outData.putSerializable(IGcConnectivityService.PARAM_GC_POWER, (SwitchOnOff) bootupReady);
			outMsg.setData(outData);
			mMessenger.send(outMsg);

		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}
	
	@Override
	public void error(Exception e) {

	}
}
