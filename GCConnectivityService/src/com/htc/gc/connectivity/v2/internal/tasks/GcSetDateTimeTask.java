package com.htc.gc.connectivity.v2.internal.tasks;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.htc.gc.connectivity.v2.interfaces.IGcConnectivityService;
import com.htc.gc.connectivity.v2.internal.callables.GcBleWriteCallable;
import com.htc.gc.connectivity.v2.internal.callables.GcBootUpCallable;
import com.htc.gc.connectivity.v2.internal.common.Common;
import com.htc.gc.connectivity.v2.internal.common.GcConnectivityTask;
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



public class GcSetDateTimeTask extends GcConnectivityTask {

	private final static String TAG = "GcSetDateTimeTask";
	
	private BluetoothDevice mBluetoothDevice;
	private Calendar mCalendar;
	
	
	
	public GcSetDateTimeTask(GcBleTransceiver gcBleTransceiver, GcWifiTransceiver gcWifiTransceiver, Messenger messenger, ExecutorService executor, BluetoothDevice device, Calendar calendar) {

		super(gcBleTransceiver, gcWifiTransceiver, messenger, executor);
		
		mBluetoothDevice = device;
		
		if (calendar == null) {
			
			mCalendar = Calendar.getInstance();

		} else {

			mCalendar = calendar;
		}
	}

	
	
	@Override
	public void execute() throws Exception {
		
		super.execute();
		
		super.from();
		
		Integer result;
		Future<Integer> future;
		BluetoothGattCharacteristic resultA;
		Future<BluetoothGattCharacteristic> futureA;
		
		future = mExecutor.submit(new GcBootUpCallable(mGcBleTransceiver, mExecutor, mBluetoothDevice));
		result = future.get();
		
		if (result == Common.ERROR_SUCCESS) {

			byte[] dateTimeArray = new byte[8];
			dateTimeArray[0] = (byte) (mCalendar.get(Calendar.YEAR) & 0xff);
			dateTimeArray[1] = (byte) ((mCalendar.get(Calendar.YEAR) >> 8) & 0xff);
			dateTimeArray[2] = (byte) (mCalendar.get(Calendar.MONTH));
			dateTimeArray[3] = (byte) (mCalendar.get(Calendar.DATE));
			dateTimeArray[4] = (byte) (mCalendar.get(Calendar.HOUR_OF_DAY));
			dateTimeArray[5] = (byte) (mCalendar.get(Calendar.MINUTE));
			dateTimeArray[6] = (byte) (mCalendar.get(Calendar.SECOND));
			dateTimeArray[7] = (byte) (mCalendar.get(Calendar.AM_PM));
			
			futureA = mExecutor.submit(new GcBleWriteCallable(mGcBleTransceiver, mBluetoothDevice, GcBleGattAttributes.GC_DATE_TIME, dateTimeArray));

			resultA = futureA.get();
			
			sendMessage(resultA);

		} else {
			
			sendMessage(null);
		}
		
		super.to(TAG);
	}
	
	
	
	private void sendMessage(BluetoothGattCharacteristic result) {
		
		try {
			
			Message outMsg = Message.obtain();
			outMsg.what = IGcConnectivityService.CB_SET_DATE_TIME_RESULT;
			Bundle outData = new Bundle();
			
			if (result != null) {
				
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

		sendMessage(null);
	}
}
