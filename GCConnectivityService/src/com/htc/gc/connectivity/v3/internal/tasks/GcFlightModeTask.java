package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v3.internal.callables.GcExampleCallable;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleGattAttributes;
import com.htc.gc.connectivity.v3.internal.component.le.GcBleTransceiver;
import com.htc.gc.connectivity.v3.internal.component.wifi.GcWifiTransceiver;
import com.htc.gc.connectivity.v3.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v3.internal.common.*;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;



public class GcFlightModeTask extends GcConnectivityTask {

	private final static String TAG = "GcFlightModeTask";
	private BluetoothDevice mBluetoothDevice;

	public GcFlightModeTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		mBluetoothDevice = device;
	}

	@Override
	public void execute() throws Exception {

		super.execute();
		Future<Integer> futureBoot;
		Integer bootResult;
		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		bootResult = futureBoot.get();

		if (bootResult == Common.ERROR_SUCCESS)
		{
			BluetoothGattCharacteristic result;
			Future<BluetoothGattCharacteristic> futureFlight;
			byte[] operation = new byte[1];
			operation[0] = 0;//Flight mode
			futureFlight = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.POWER_ON_REQUEST, operation));
			result = futureFlight.get();

			if (result != null) {
				sendMessage(true);
			}
			else
			{
				sendMessage(false);
			}
		}
		else
		{
			Log.d(TAG, "[MGCC] boot up is fail");
			sendMessage(false);
		}
	}

	private void sendMessage(boolean result) {

		try {
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_SET_FLIGHT_MODE_RESULT;

			Bundle outData = new Bundle();
			if (result == true)
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_SUCCESS);
			else
				outData.putSerializable(IGcConnectivityService.PARAM_RESULT, IGcConnectivityService.Result.RESULT_FAIL);
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
