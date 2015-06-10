package com.htc.gc.connectivity.v3.internal.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v3.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v3.internal.callables.GcBleReadCallable;
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



public class GcSetAutoSleepTimerOffsetTask extends GcConnectivityTask {

	private final static String TAG = "GcSetAutoSleepTimerOffsetTask";
	private BluetoothDevice mBluetoothDevice;
	private int mOffsetInSec;

	public GcSetAutoSleepTimerOffsetTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, int offset_sec) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);

		mBluetoothDevice = device;
		mOffsetInSec = offset_sec;
	}

	@Override
	public void execute() throws Exception {

		super.execute();
		super.from();

		BluetoothGattCharacteristic result = null;
		Future<BluetoothGattCharacteristic> futureA;
		byte[] tmp_data = new byte[2];
		Future<Integer> futureBoot;
		Integer resultBoot;

		futureBoot = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice, IGcConnectivityService.BootUpType.BOOTUP_RTOS, mMessenger));
		resultBoot = futureBoot.get();

		if (resultBoot == Common.ERROR_SUCCESS) {

			tmp_data[0] = (byte) (mOffsetInSec & 0xff);
			tmp_data[1] = (byte) ((mOffsetInSec >> 8) & 0xff);
			futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GcV2CommandEnum.SET_TIMEOUT_OFFSET_REQUEST, tmp_data));
			result = futureA.get();
			if (result != null) {
				sendMessage(true);
			} else {
				sendMessage(false);
			}
		}
		else
		{
			sendMessage(false);
		}

		super.to(TAG);
	}

	private void sendMessage(boolean result) {
		try {
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_SET_AUTOSLEEP_TIMER_OFFSET_RESULT;
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
